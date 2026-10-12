package com.magicbanana666.enderstorage;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The three colours (and optionally the owner) that link ender chests, tanks and pouches together.
 * Everything with the same frequency shares the same storage.
 */
public record Frequency(DyeColor left, DyeColor middle, DyeColor right, Optional<UUID> owner, Optional<String> ownerName) {

    public static final Frequency DEFAULT = new Frequency(DyeColor.WHITE, DyeColor.WHITE, DyeColor.WHITE);

    public static final Codec<Frequency> CODEC = RecordCodecBuilder.create(i -> i.group(
            DyeColor.CODEC.fieldOf("left").forGetter(Frequency::left),
            DyeColor.CODEC.fieldOf("middle").forGetter(Frequency::middle),
            DyeColor.CODEC.fieldOf("right").forGetter(Frequency::right),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Frequency::owner),
            Codec.STRING.optionalFieldOf("owner_name").forGetter(Frequency::ownerName)
    ).apply(i, Frequency::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Frequency> STREAM_CODEC = StreamCodec.composite(
            DyeColor.STREAM_CODEC, Frequency::left,
            DyeColor.STREAM_CODEC, Frequency::middle,
            DyeColor.STREAM_CODEC, Frequency::right,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Frequency::owner,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Frequency::ownerName,
            Frequency::new);

    public Frequency(DyeColor left, DyeColor middle, DyeColor right) {
        this(left, middle, right, Optional.empty(), Optional.empty());
    }

    public static Frequency of(DyeColor colour) {
        return new Frequency(colour, colour, colour);
    }

    public static Frequency get(ItemStack stack) {
        return stack.getOrDefault(EnderStorage.FREQUENCY.get(), DEFAULT);
    }

    public ItemStack applyTo(ItemStack stack) {
        stack.set(EnderStorage.FREQUENCY.get(), this);
        return stack;
    }

    /** The colour of button 0 (left), 1 (middle) or 2 (right). */
    public DyeColor colour(int button) {
        return switch (button) {
            case 0 -> this.left;
            case 1 -> this.middle;
            default -> this.right;
        };
    }

    public Frequency withColour(int button, DyeColor colour) {
        return switch (button) {
            case 0 -> new Frequency(colour, this.middle, this.right, this.owner, this.ownerName);
            case 1 -> new Frequency(this.left, colour, this.right, this.owner, this.ownerName);
            default -> new Frequency(this.left, this.middle, colour, this.owner, this.ownerName);
        };
    }

    /** Replaces the colours that are not null. */
    public Frequency withColours(@Nullable DyeColor left, @Nullable DyeColor middle, @Nullable DyeColor right) {
        return new Frequency(left != null ? left : this.left, middle != null ? middle : this.middle, right != null ? right : this.right, this.owner, this.ownerName);
    }

    public Frequency withOwner(Player player) {
        return new Frequency(this.left, this.middle, this.right, Optional.of(player.getUUID()), Optional.of(player.getGameProfile().name()));
    }

    public Frequency withoutOwner() {
        return new Frequency(this.left, this.middle, this.right, Optional.empty(), Optional.empty());
    }

    public boolean hasOwner() {
        return this.owner.isPresent();
    }

    public boolean isOwner(Player player) {
        return this.owner.isEmpty() || this.owner.get().equals(player.getUUID());
    }

    /** The key of the shared storage; the owner name is only for display. */
    public String key() {
        return this.left.getSerializedName() + "," + this.middle.getSerializedName() + "," + this.right.getSerializedName()
                + this.owner.map(uuid -> "," + uuid).orElse("");
    }

    public MutableComponent describe() {
        return Component.translatable("color.minecraft." + this.left.getSerializedName())
                .append("/")
                .append(Component.translatable("color.minecraft." + this.middle.getSerializedName()))
                .append("/")
                .append(Component.translatable("color.minecraft." + this.right.getSerializedName()));
    }
}
