package com.magicbanana666.enderstorage.client;

import java.util.function.Consumer;

import com.magicbanana666.enderstorage.Frequency;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Item renderers for the ender chest and tank: they draw the block with the colours of the item. */
public final class FrequencyItemRenderers {

    private FrequencyItemRenderers() {
    }

    public static final class Chest implements SpecialModelRenderer<Frequency> {

        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        public record Unbaked() implements SpecialModelRenderer.Unbaked<Frequency> {
            @Override
            public SpecialModelRenderer<Frequency> bake(BakingContext context) {
                return new Chest();
            }

            @Override
            public MapCodec<Unbaked> type() {
                return CODEC;
            }
        }

        @Override
        public void submit(@Nullable Frequency frequency, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
            EnderChestRenderer.drawChest(pose, collector, Direction.SOUTH, frequency != null ? frequency : Frequency.DEFAULT, 0F, light, overlay, null);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            box(output, 1 / 16F, 0F, 1 / 16F, 15 / 16F, 15 / 16F, 1F);
        }

        @Override
        public @Nullable Frequency extractArgument(ItemStack stack) {
            return Frequency.get(stack);
        }
    }

    public static final class Tank implements SpecialModelRenderer<Frequency> {

        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        public record Unbaked() implements SpecialModelRenderer.Unbaked<Frequency> {
            @Override
            public SpecialModelRenderer<Frequency> bake(BakingContext context) {
                return new Tank();
            }

            @Override
            public MapCodec<Unbaked> type() {
                return CODEC;
            }
        }

        @Override
        public void submit(@Nullable Frequency frequency, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
            EnderTankRenderer.drawTank(pose, collector, Direction.SOUTH, frequency != null ? frequency : Frequency.DEFAULT, 90F, light, overlay);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            box(output, 0.15F, 0F, 0.15F, 0.85F, 0.95F, 0.95F);
        }

        @Override
        public @Nullable Frequency extractArgument(ItemStack stack) {
            return Frequency.get(stack);
        }
    }

    private static void box(Consumer<Vector3fc> output, float x1, float y1, float z1, float x2, float y2, float z2) {
        for (float x : new float[] { x1, x2 }) {
            for (float y : new float[] { y1, y2 }) {
                for (float z : new float[] { z1, z2 }) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }
}
