package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.Frequency;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model properties of the ender pouch: its colours, whether it is personal and whether it is open. */
public final class PouchProperties {

    private PouchProperties() {
    }

    /** enderstorage:bag/owned - the pouch is personal. */
    public record Owned() implements ConditionalItemModelProperty {
        public static final MapCodec<Owned> CODEC = MapCodec.unit(new Owned());

        @Override
        public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
            return Frequency.get(stack).isLocked();
        }

        @Override
        public MapCodec<Owned> type() {
            return CODEC;
        }
    }

    /** enderstorage:bag/open - the player looks into this pouch's inventory right now. */
    public record Open() implements ConditionalItemModelProperty {
        public static final MapCodec<Open> CODEC = MapCodec.unit(new Open());

        @Override
        public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || entity != minecraft.player || minecraft.screen == null) {
                return false;
            }
            AbstractContainerMenu menu = minecraft.player.containerMenu;
            return menu != minecraft.player.inventoryMenu && minecraft.screen.getTitle().equals(stack.getHoverName());
        }

        @Override
        public MapCodec<Open> type() {
            return CODEC;
        }
    }

    public enum Location implements StringRepresentable {
        LEFT, MIDDLE, RIGHT;

        public static final Codec<Location> CODEC = StringRepresentable.fromEnum(Location::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** enderstorage:bag/frequency - the colour of one of the three buttons. */
    public record Colour(Location location) implements SelectItemModelProperty<DyeColor> {
        private static final MapCodec<Colour> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Location.CODEC.fieldOf("location").forGetter(Colour::location)
        ).apply(i, Colour::new));
        public static final Type<Colour, DyeColor> TYPE = Type.create(MAP_CODEC, DyeColor.CODEC);

        @Override
        public DyeColor get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
            return Frequency.get(stack).colour(this.location.ordinal());
        }

        @Override
        public Codec<DyeColor> valueCodec() {
            return DyeColor.CODEC;
        }

        @Override
        public Type<Colour, DyeColor> type() {
            return TYPE;
        }
    }
}
