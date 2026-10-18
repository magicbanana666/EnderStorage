package com.magicbanana666.enderstorage.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.network.chat.Component;

/**
 * A fluid and an amount in droplets ({@link FluidConstants#BUCKET} per bucket). Capacities, rates and what the
 * player sees stay in mB; {@link #mb()} converts.
 */
public record TankFluid(FluidVariant variant, long amount) {

    /** Droplets per mB. */
    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;
    public static final TankFluid EMPTY = new TankFluid(FluidVariant.blank(), 0);

    public static final Codec<TankFluid> CODEC = RecordCodecBuilder.create(i -> i.group(
            FluidVariant.CODEC.optionalFieldOf("variant", FluidVariant.blank()).forGetter(TankFluid::variant),
            Codec.LONG.optionalFieldOf("amount", 0L).forGetter(TankFluid::amount)
    ).apply(i, TankFluid::of));

    public static TankFluid of(FluidVariant variant, long amount) {
        return variant.isBlank() || amount <= 0 ? EMPTY : new TankFluid(variant, amount);
    }

    public boolean isEmpty() {
        return this.variant.isBlank() || this.amount <= 0;
    }

    /** The amount in mB. */
    public int mb() {
        return (int) Math.min(Integer.MAX_VALUE, this.amount / DROPLETS_PER_MB);
    }

    /** How full a tank of the given capacity in mB is, from 0 to 1. */
    public float fill(int capacity) {
        return Math.min(1F, this.amount / (float) (capacity * DROPLETS_PER_MB));
    }

    public Component getHoverName() {
        return FluidVariantAttributes.getName(this.variant);
    }

    public int lightLevel() {
        return this.isEmpty() ? 0 : FluidVariantAttributes.getLuminance(this.variant);
    }
}
