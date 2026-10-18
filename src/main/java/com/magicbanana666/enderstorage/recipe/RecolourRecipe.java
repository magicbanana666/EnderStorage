package com.magicbanana666.enderstorage.recipe;

import com.magicbanana666.enderstorage.Frequency;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Recolours an ender chest, tank or pouch: put it in the grid with up to three dyes in the row above it.
 * The dye above-left sets the left button, above the middle one, above-right the right one.
 */
public class RecolourRecipe extends CustomRecipe {

    public static final MapCodec<RecolourRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
    ).apply(i, RecolourRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecolourRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            RecolourRecipe::new);
    public static final RecipeSerializer<RecolourRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;

    public RecolourRecipe(ItemStackTemplate result) {
        this.result = result;
    }

    private record Match(ItemStack target, @Nullable DyeColor left, @Nullable DyeColor middle, @Nullable DyeColor right) {
    }

    private @Nullable Match find(CraftingInput input) {
        int targetX = -1;
        int targetY = -1;
        ItemStack target = ItemStack.EMPTY;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.is(this.result.item())) {
                    if (!target.isEmpty()) {
                        return null;
                    }
                    target = stack;
                    targetX = x;
                    targetY = y;
                }
            }
        }
        if (target.isEmpty() || targetY == 0) {
            return null;
        }
        DyeColor[] dyes = new DyeColor[3];
        boolean anyDye = false;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.isEmpty() || (x == targetX && y == targetY)) {
                    continue;
                }
                DyeColor dye = dyeColour(stack);
                int button = x - targetX + 1;
                if (dye == null || y != targetY - 1 || button < 0 || button > 2) {
                    return null;
                }
                dyes[button] = dye;
                anyDye = true;
            }
        }
        return anyDye ? new Match(target, dyes[0], dyes[1], dyes[2]) : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Match match = this.find(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = match.target().copyWithCount(1);
        return Frequency.get(stack).withColours(match.left(), match.middle(), match.right()).applyTo(stack);
    }

    @Override
    public RecipeSerializer<RecolourRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** The colour of a wool block item, EG minecraft:red_wool -> red. */
    public static @Nullable DyeColor woolColour(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!path.endsWith("_wool")) {
            return null;
        }
        return DyeColor.byName(path.substring(0, path.length() - "_wool".length()), null);
    }

    /** The colour of a dye: the dye component of vanilla dyes, or a c:dyes/<colour> tag. */
    public static @Nullable DyeColor dyeColour(ItemStack stack) {
        DyeColor colour = stack.get(DataComponents.DYE);
        if (colour != null) {
            return colour;
        }
        for (DyeColor dye : DyeColor.values()) {
            if (stack.is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/" + dye.getSerializedName())))) {
                return dye;
            }
        }
        return null;
    }
}
