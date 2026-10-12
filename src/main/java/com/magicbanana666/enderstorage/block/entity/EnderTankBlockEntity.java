package com.magicbanana666.enderstorage.block.entity;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.storage.FluidStorage;
import com.magicbanana666.enderstorage.storage.SharedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * pushes its fluid into the blocks around it.
 */
public class EnderTankBlockEntity extends FrequencyBlockEntity {

    /** Whether the valve flips the redstone signal (clicked by the player). */
    private boolean invertRedstone;
    /** Whether the tank is pushing fluid out. */
    private boolean pressure;
    /** On the client: the fluid in the tank and the valve angle in degrees. */
    private FluidStack clientFluid = FluidStack.EMPTY;
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

    /** The fluid to show: the real one on the server, the synced one on the client. */
    public FluidStack getFluid() {
        if (this.level != null && this.level.isClientSide()) {
            return this.clientFluid;
        }
        FluidStorage storage = this.getFluidHandler();
        return storage != null ? storage.getFluid() : FluidStack.EMPTY;
    }

    public boolean hasPressure() {
        return this.pressure;
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
            this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.updateLight();
        }
    }

    private void pushFluid(FluidStorage source) {
        for (Direction side : Direction.values()) {
            if (source.getFluid().isEmpty()) {
                return;
            }
            ResourceHandler<FluidResource> target = this.level.getCapability(Capabilities.Fluid.BLOCK, this.worldPosition.relative(side), side.getOpposite());
            if (target != null) {
                ResourceHandlerUtil.move(source, target, resource -> true, 100, null);
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
        return InteractionResult.PASS;
    }

    @Override
    public int comparatorOutput() {
        FluidStack fluid = this.getFluid();
        if (fluid.isEmpty()) {
            return 0;
        }
        return 1 + Mth.floor(14F * fluid.getAmount() / EnderStorageConfig.tankCapacity());
    }

    @Override
    public int lightLevel() {
        FluidStack fluid = this.getFluid();
        if (fluid.isEmpty()) {
            return 0;
        }
        return Math.round(fluid.getFluidType().getLightLevel(fluid) * Math.min(1F, fluid.getAmount() / (float) EnderStorageConfig.tankCapacity()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("InvertRedstone", this.invertRedstone);
        output.putBoolean("Pressure", this.pressure);
        // Only for the client (the real fluid is in the shared storage)
        output.store("Fluid", FluidStack.OPTIONAL_CODEC, this.getFluid());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.invertRedstone = input.getBooleanOr("InvertRedstone", false);
        this.pressure = input.getBooleanOr("Pressure", false);
        this.clientFluid = input.read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        if (this.level != null && this.level.isClientSide()) {
            this.updateLight();
        }
    }
}
