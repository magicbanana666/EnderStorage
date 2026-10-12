package com.magicbanana666.enderstorage.storage;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** The shared tank of one frequency. */
public class FluidStorage extends FluidStacksResourceHandler {

    private final SharedStorage owner;
    private final Frequency frequency;
    private int changeCount;

    FluidStorage(SharedStorage owner, Frequency frequency) {
        super(1, EnderStorageConfig.tankCapacity());
        this.owner = owner;
        this.frequency = frequency;
    }

    void load(FluidStack fluid) {
        this.set(0, FluidResource.of(fluid), fluid.getAmount());
    }

    public Frequency frequency() {
        return this.frequency;
    }

    public FluidStack getFluid() {
        FluidResource resource = this.getResource(0);
        return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack((int) this.getAmountAsLong(0));
    }

    public int changeCount() {
        return this.changeCount;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        this.changeCount++;
        this.owner.setDirty();
    }
}
