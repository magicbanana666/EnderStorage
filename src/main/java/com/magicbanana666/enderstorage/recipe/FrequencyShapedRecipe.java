package com.magicbanana666.enderstorage.recipe;

import com.magicbanana666.enderstorage.Frequency;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** A shaped recipe whose result gets the colour of the wool used in it on all three buttons. */
public class FrequencyShapedRecipe extends ShapedRecipe {

    public static final MapCodec<FrequencyShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.resultTemplate)
    ).apply(i, FrequencyShapedRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrequencyShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.resultTemplate,
            FrequencyShapedRecipe::new);
    public static final RecipeSerializer<FrequencyShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate resultTemplate;

    public FrequencyShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.resultTemplate = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(ItemTags.WOOL)) {
                DyeColor colour = RecolourRecipe.woolColour(stack);
                if (colour != null) {
                    Frequency.of(colour).applyTo(result);
                }
                break;
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (Object) SERIALIZER;
    }
}
