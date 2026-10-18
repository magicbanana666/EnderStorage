package com.magicbanana666.enderstorage.storage;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;

/** The shared tank of one frequency. Upgrades add capacity up to the configured maximum. */
public class FluidStorage extends SingleVariantStorage<FluidVariant> {

    private final SharedStorage owner;
    private final Frequency frequency;
    /** mB added by upgrades. */
    private int bonusCapacity;
    private int changeCount;
    /** The contents after the last change, to only count real changes. */
    private TankFluid lastContents = TankFluid.EMPTY;

    FluidStorage(SharedStorage owner, Frequency frequency) {
        this.owner = owner;
        this.frequency = frequency;
    }

    void load(TankFluid fluid) {
        this.variant = fluid.variant();
        this.amount = fluid.amount();
        this.lastContents = this.getFluid();
    }

    void loadBonusCapacity(int bonus) {
        this.bonusCapacity = Math.max(0, bonus);
    }

    int bonusCapacity() {
        return this.bonusCapacity;
    }

    public Frequency frequency() {
        return this.frequency;
    }

    public TankFluid getFluid() {
        return TankFluid.of(this.variant, this.amount);
    }

    /** Current capacity in mB. */
    public int capacity() {
        return Math.min(EnderStorageConfig.tankCapacity() + this.bonusCapacity, EnderStorageConfig.tankMaxCapacity());
    }

    public boolean isFullyUpgraded() {
        return this.capacity() >= EnderStorageConfig.tankMaxCapacity();
    }

    /** Adds capacity (up to the maximum) and returns the new capacity in mB. */
    public int upgrade(int amount) {
        this.bonusCapacity = Math.min(this.bonusCapacity + amount, EnderStorageConfig.tankMaxCapacity() - EnderStorageConfig.tankCapacity());
        this.changeCount++;
        this.owner.setDirty();
        return this.capacity();
    }

    public int changeCount() {
        return this.changeCount;
    }

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return this.capacity() * TankFluid.DROPLETS_PER_MB;
    }

    @Override
    protected void onFinalCommit() {
        // Called on every committed transaction that touched the tank, also when nothing actually changed
        TankFluid contents = this.getFluid();
        if (contents.equals(this.lastContents)) {
            return;
        }
        this.lastContents = contents;
        this.changeCount++;
        this.owner.setDirty();
    }
}
