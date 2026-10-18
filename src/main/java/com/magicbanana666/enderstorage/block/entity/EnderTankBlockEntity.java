package com.magicbanana666.enderstorage.block.entity;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.menu.EnderTankMenu;
import com.magicbanana666.enderstorage.storage.FluidStorage;
import com.magicbanana666.enderstorage.storage.SharedStorage;
import com.magicbanana666.enderstorage.storage.TankFluid;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The ender tank. With a redstone signal (or without one, after clicking the valve) the valve opens and the tank
 * pushes its fluid into the blocks around it. Pump upgrades (on this block) make it push faster.
 */
public class EnderTankBlockEntity extends FrequencyBlockEntity {

    /** Whether the valve flips the redstone signal (clicked by the player). */
    private boolean invertRedstone;
    /** Whether the tank is pushing fluid out. */
    private boolean pressure;
    private int pumpUpgrades;
    /** On the client: the fluid and capacity of the tank, and the valve angle in degrees. */
    private TankFluid clientFluid = TankFluid.EMPTY;
    private int clientCapacity = EnderStorageConfig.tankCapacity();
    public float valveAngle = 90F;
    public float prevValveAngle = 90F;
    private boolean valveInitialised;
    private int lastChangeCount = -1;
    private int lastLight;

    public EnderTankBlockEntity(BlockPos pos, BlockState state) {
        super(EnderStorage.ENDER_TANK_BE, pos, state);
    }

    public @Nullable FluidStorage getFluidHandler() {
        SharedStorage shared = this.shared();
        return shared != null ? shared.tank(this.frequency) : null;
    }

    private boolean isClient() {
        return this.level != null && this.level.isClientSide();
    }

    /** The fluid to show: the real one on the server, the synced one on the client. */
    public TankFluid getFluid() {
        if (this.isClient()) {
            return this.clientFluid;
        }
        FluidStorage storage = this.getFluidHandler();
        return storage != null ? storage.getFluid() : TankFluid.EMPTY;
    }

    /** Capacity in mB: the real one on the server, the synced one on the client. */
    public int capacity() {
        if (this.isClient()) {
            return this.clientCapacity;
        }
        FluidStorage storage = this.getFluidHandler();
        return storage != null ? storage.capacity() : EnderStorageConfig.tankCapacity();
    }

    public boolean hasPressure() {
        return this.pressure;
    }

    public boolean isRedstoneInverted() {
        return this.invertRedstone;
    }

    public int pumpUpgrades() {
        return this.pumpUpgrades;
    }

    /** mB per tick the open valve pushes out. */
    public int pumpRate() {
        return EnderStorageConfig.pumpRate(this.pumpUpgrades);
    }

    public void tick() {
        if (this.level == null) {
            return;
        }
        if (this.level.isClientSide()) {
            float target = this.pressure ? -90F : 90F;
            if (!this.valveInitialised) {
                this.valveAngle = this.prevValveAngle = target;
                this.valveInitialised = true;
            }
            this.prevValveAngle = this.valveAngle;
            this.valveAngle = Mth.approach(this.valveAngle, target, 20F);
            return;
        }
        boolean wasPressure = this.pressure;
        this.pressure = this.level.hasNeighborSignal(this.worldPosition) != this.invertRedstone;
        FluidStorage storage = this.getFluidHandler();
        if (storage == null) {
            return;
        }
        if (this.pressure) {
            this.pushFluid(storage);
        }
        if (this.pressure != wasPressure || storage.changeCount() != this.lastChangeCount) {
            this.lastChangeCount = storage.changeCount();
            this.sync();
        }
    }

    private void sync() {
        this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        this.updateLight();
    }

    /** Pushes up to the pump rate per tick into the blocks around the tank. */
    private void pushFluid(FluidStorage source) {
        long budget = this.pumpRate() * TankFluid.DROPLETS_PER_MB;
        for (Direction side : Direction.values()) {
            if (budget <= 0 || source.getFluid().isEmpty()) {
                return;
            }
            BlockPos pos = this.worldPosition.relative(side);
            // A tank of the same frequency shares this storage; pushing into it would only move the fluid back in
            if (this.level.getBlockEntity(pos) instanceof EnderTankBlockEntity tank && tank.getFrequency().key().equals(this.frequency.key())) {
                continue;
            }
            Storage<FluidVariant> target = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(this.level, pos, side.getOpposite());
            if (target != null && target != source) {
                budget -= StorageUtil.move(source, target, resource -> true, budget, null);
            }
        }
    }

    private void updateLight() {
        int light = this.lightLevel();
        if (light != this.lastLight && this.level != null) {
            this.lastLight = light;
            this.level.getLightEngine().checkBlock(this.worldPosition);
        }
    }

    @Override
    public InteractionResult activate(Player player, int part) {
        if (part == 4) {
            if (this.level != null && !this.level.isClientSide()) {
                this.invertRedstone = !this.invertRedstone;
                this.setChanged();
                this.level.playSound(null, this.worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            this.sync();
            serverPlayer.openMenu(new ExtendedMenuProvider<BlockPos>() {
                @Override
                public BlockPos getScreenOpeningData(ServerPlayer p) {
                    return EnderTankBlockEntity.this.worldPosition;
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable(EnderTankBlockEntity.this.getBlockState().getBlock().getDescriptionId());
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                    return new EnderTankMenu(id, EnderTankBlockEntity.this.worldPosition);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isUpgradeItem(ItemStack stack) {
        return EnderStorageConfig.isPumpItem(stack) || EnderStorageConfig.tankUpgradeCapacity(stack) > 0;
    }

    @Override
    public void upgrade(Player player, ItemStack stack) {
        if (EnderStorageConfig.isPumpItem(stack)) {
            if (this.pumpUpgrades >= EnderStorageConfig.pumpMaxUpgrades()) {
                player.sendOverlayMessage(Component.translatable("enderstorage.upgrade.pump_full", this.pumpRate()));
                return;
            }
            this.pumpUpgrades++;
            this.setChanged();
            this.sync();
            this.upgraded(player, stack, Component.translatable("enderstorage.upgrade.pump", this.pumpUpgrades, EnderStorageConfig.pumpMaxUpgrades(), this.pumpRate()));
            return;
        }
        FluidStorage storage = this.getFluidHandler();
        if (storage == null) {
            return;
        }
        if (storage.isFullyUpgraded()) {
            player.sendOverlayMessage(Component.translatable("enderstorage.upgrade.tank_full", storage.capacity() / 1000));
            return;
        }
        int capacity = storage.upgrade(EnderStorageConfig.tankUpgradeCapacity(stack));
        this.upgraded(player, stack, Component.translatable("enderstorage.upgrade.tank", capacity / 1000, EnderStorageConfig.tankMaxCapacity() / 1000));
    }

    @Override
    public int comparatorOutput() {
        TankFluid fluid = this.getFluid();
        if (fluid.isEmpty()) {
            return 0;
        }
        return 1 + Mth.floor(14F * fluid.fill(this.capacity()));
    }

    @Override
    public int lightLevel() {
        TankFluid fluid = this.getFluid();
        if (fluid.isEmpty()) {
            return 0;
        }
        return Math.round(fluid.lightLevel() * fluid.fill(this.capacity()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.pumpUpgrades = components.getOrDefault(EnderStorage.PUMP_UPGRADES, 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.pumpUpgrades > 0) {
            components.set(EnderStorage.PUMP_UPGRADES, this.pumpUpgrades);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("PumpUpgrades");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("InvertRedstone", this.invertRedstone);
        output.putBoolean("Pressure", this.pressure);
        output.putInt("PumpUpgrades", this.pumpUpgrades);
        // Only for the client (the real fluid is in the shared storage)
        output.store("Fluid", TankFluid.CODEC, this.getFluid());
        output.putInt("Capacity", this.capacity());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.invertRedstone = input.getBooleanOr("InvertRedstone", false);
        this.pressure = input.getBooleanOr("Pressure", false);
        this.pumpUpgrades = input.getIntOr("PumpUpgrades", 0);
        this.clientFluid = input.read("Fluid", TankFluid.CODEC).orElse(TankFluid.EMPTY);
        this.clientCapacity = input.getIntOr("Capacity", EnderStorageConfig.tankCapacity());
        if (this.level != null && this.level.isClientSide()) {
            this.updateLight();
        }
    }
}
