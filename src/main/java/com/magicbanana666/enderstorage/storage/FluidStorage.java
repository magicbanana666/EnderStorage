package com.magicbanana666.enderstorage.storage;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** The shared tank of one frequency. Upgrades add capacity up to the configured maximum. */
public class FluidStorage extends FluidStacksResourceHandler {

    private final SharedStorage owner;
    private final Frequency frequency;
    /** mB added by upgrades. */
    private int bonusCapacity;
    private int changeCount;

    FluidStorage(SharedStorage owner, Frequency frequency) {
        super(1, EnderStorageConfig.tankCapacity());
        this.owner = owner;
        this.frequency = frequency;
    }

    void load(FluidStack fluid) {
        this.set(0, FluidResource.of(fluid), fluid.getAmount());
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

    public FluidStack getFluid() {
        FluidResource resource = this.getResource(0);
        return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack((int) this.getAmountAsLong(0));
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
    protected int getCapacity(int index, FluidResource resource) {
        return this.capacity();
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        this.changeCount++;
        this.owner.setDirty();
    }
}
