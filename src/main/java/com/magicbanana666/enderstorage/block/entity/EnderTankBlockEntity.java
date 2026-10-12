package com.magicbanana666.enderstorage.block.entity;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.menu.EnderTankMenu;
import com.magicbanana666.enderstorage.storage.FluidStorage;
import com.magicbanana666.enderstorage.storage.SharedStorage;
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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
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
    private FluidStack clientFluid = FluidStack.EMPTY;
    private int clientCapacity = EnderStorageConfig.tankCapacity();
    public float valveAngle = 90F;
    public float prevValveAngle = 90F;
    private boolean valveInitialised;
    private int lastChangeCount = -1;
    private int lastLight;

    public EnderTankBlockEntity(BlockPos pos, BlockState state) {
        super(EnderStorage.ENDER_TANK_BE.get(), pos, state);
    }

    public @Nullable FluidStorage getFluidHandler() {
        SharedStorage shared = this.shared();
        return shared != null ? shared.tank(this.frequency) : null;
    }

    private boolean isClient() {
        return this.level != null && this.level.isClientSide();
    }

    /** The fluid to show: the real one on the server, the synced one on the client. */
    public FluidStack getFluid() {
        if (this.isClient()) {
            return this.clientFluid;
        }
        FluidStorage storage = this.getFluidHandler();
        return storage != null ? storage.getFluid() : FluidStack.EMPTY;
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
        int budget = this.pumpRate();
        for (Direction side : Direction.values()) {
            if (budget <= 0 || source.getFluid().isEmpty()) {
                return;
            }
            ResourceHandler<FluidResource> target = this.level.getCapability(Capabilities.Fluid.BLOCK, this.worldPosition.relative(side), side.getOpposite());
            if (target != null) {
                budget -= ResourceHandlerUtil.move(source, target, resource -> true, budget, null);
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
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnderTankMenu(id, this.worldPosition),
                    Component.translatable(this.getBlockState().getBlock().getDescriptionId())), buf -> buf.writeBlockPos(this.worldPosition));
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
        FluidStack fluid = this.getFluid();
        if (fluid.isEmpty()) {
            return 0;
        }
        return 1 + Mth.floor(14F * Math.min(1F, fluid.getAmount() / (float) this.capacity()));
    }

    @Override
    public int lightLevel() {
        FluidStack fluid = this.getFluid();
        if (fluid.isEmpty()) {
            return 0;
        }
        return Math.round(fluid.getFluidType().getLightLevel(fluid) * Math.min(1F, fluid.getAmount() / (float) this.capacity()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.pumpUpgrades = components.getOrDefault(EnderStorage.PUMP_UPGRADES.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.pumpUpgrades > 0) {
            components.set(EnderStorage.PUMP_UPGRADES.get(), this.pumpUpgrades);
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
        output.store("Fluid", FluidStack.OPTIONAL_CODEC, this.getFluid());
        output.putInt("Capacity", this.capacity());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.invertRedstone = input.getBooleanOr("InvertRedstone", false);
        this.pressure = input.getBooleanOr("Pressure", false);
        this.pumpUpgrades = input.getIntOr("PumpUpgrades", 0);
        this.clientFluid = input.read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        this.clientCapacity = input.getIntOr("Capacity", EnderStorageConfig.tankCapacity());
        if (this.level != null && this.level.isClientSide()) {
            this.updateLight();
        }
    }
}
