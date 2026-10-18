package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.block.EnderStorageBlock;
import com.magicbanana666.enderstorage.block.entity.EnderChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the ender chest: the model, the three colour buttons on the lid, and the end portal and pearl inside. */
public class EnderChestRenderer implements BlockEntityRenderer<EnderChestBlockEntity, EnderChestRenderer.State> {

    private static final EnderChestModel MODEL = new EnderChestModel();
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.SOUTH;
        public Frequency frequency = Frequency.DEFAULT;
        public float lidAngle;
        public float time;
        public final ItemStackRenderState pearl = new ItemStackRenderState();
    }

    public EnderChestRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EnderChestBlockEntity chest, State state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTick, cameraPosition, breakProgress);
        state.facing = chest.getBlockState().getValue(EnderStorageBlock.FACING);
        state.frequency = chest.getFrequency();
        state.lidAngle = chest.lidAngle(partialTick);
        state.time = (chest.getLevel() != null ? chest.getLevel().getGameTime() : 0) + partialTick + chest.getBlockPos().hashCode() % 100;
        this.itemModelResolver.updateForTopItem(state.pearl, new ItemStack(Items.ENDER_PEARL), ItemDisplayContext.GROUND, chest.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        drawChest(pose, collector, state.facing, state.frequency, state.lidAngle, state.lightCoords, OverlayTexture.NO_OVERLAY, state.breakProgress);
        if (state.lidAngle != 0F) {
            // The end portal floor and the floating pearl, seen when the lid is open
            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            pose.rotate(Axis.YP.rotationDegrees(-state.facing.toYRot()));
            pose.translate(-0.5F, 0F, -0.5F);
            collector.submitCustomGeometry(pose, RenderTypes.endPortal(), (p, c) -> {
                float y = 0.626F;
                c.addVertex(p, 0.188F, y, 0.188F);
                c.addVertex(p, 0.188F, y, 0.812F);
                c.addVertex(p, 0.812F, y, 0.812F);
                c.addVertex(p, 0.812F, y, 0.188F);
            });
            pose.popPose();
            pose.pushPose();
            pose.translate(0.5F, 0.2F - state.lidAngle * 0.5F + Mth.sin(state.time / 10F) * 0.05F, 0.5F);
            pose.rotate(Axis.YP.rotation(state.time / 20F));
            pose.scale(0.5F, 0.5F, 0.5F);
            state.pearl.submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    /** Draws a chest in block space (0..1), also used for the item. */
    public static void drawChest(PoseStack pose, SubmitNodeCollector collector, Direction facing, Frequency frequency, float lidAngle,
                                 int light, int overlay, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.rotate(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5F, 0F, -0.5F);

        pose.pushPose();
        pose.translate(0F, 1F, 1F);
        pose.scale(1F, -1F, -1F);
        EnderChestModel.State modelState = new EnderChestModel.State(lidAngle, frequency.hasOwner(), frequency.hasTeam());
        collector.submitModel(MODEL, modelState, pose, EnderChestModel.RENDER_TYPE, light, overlay, 0);
        if (breakProgress != null) {
            collector.order(1).submitCrumblingOverlay(MODEL, modelState, pose, EnderChestModel.RENDER_TYPE, light, overlay, -1, breakProgress);
        }
        pose.popPose();

        // The buttons sit on the lid and turn with it around the hinge at the back
        pose.pushPose();
        pose.translate(0.5F, 9F / 16F, 1F / 16F);
        pose.rotate(Axis.XP.rotation(lidAngle));
        pose.translate(0F, 5F / 16F, 7F / 16F);
        collector.submitCustomGeometry(pose, Buttons.RENDER_TYPE, (p, c) -> Buttons.drawAll(p, c, frequency, 3F / 16F, 0F, 1F, light, overlay));
        pose.popPose();

        pose.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(EnderChestBlockEntity chest) {
        return new net.minecraft.world.phys.AABB(chest.getBlockPos()).expandTowards(0, 1, 0);
    }

}
