package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.block.EnderStorageBlock;
import com.magicbanana666.enderstorage.block.entity.EnderTankBlockEntity;
import com.magicbanana666.enderstorage.storage.TankFluid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the ender tank: the model, the turning valve, the colour buttons, the fluid inside and the floating pearl. */
public class EnderTankRenderer implements BlockEntityRenderer<EnderTankBlockEntity, EnderTankRenderer.State> {

    private static final RenderType TANK_TYPE = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, "textures/endertank.png"));
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.SOUTH;
        public Frequency frequency = Frequency.DEFAULT;
        public float valveAngle;
        public TankFluid fluid = TankFluid.EMPTY;
        public int capacity = EnderStorageConfig.tankCapacity();
        public float time;
        public final ItemStackRenderState pearl = new ItemStackRenderState();
    }

    public EnderTankRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EnderTankBlockEntity tank, State state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPosition, breakProgress);
        state.facing = tank.getBlockState().getValue(EnderStorageBlock.FACING);
        state.frequency = tank.getFrequency();
        state.valveAngle = Mth.lerp(partialTick, tank.prevValveAngle, tank.valveAngle);
        state.fluid = tank.getFluid();
        state.capacity = tank.capacity();
        state.time = (tank.getLevel() != null ? tank.getLevel().getGameTime() : 0) + partialTick + tank.getBlockPos().hashCode() % 100;
        this.itemModelResolver.updateForTopItem(state.pearl, new ItemStack(Items.ENDER_PEARL), ItemDisplayContext.GROUND, tank.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        drawTank(pose, collector, state.facing, state.frequency, state.valveAngle, state.lightCoords, OverlayTexture.NO_OVERLAY);

        pose.translate(0.5F, 0F, 0.5F);
        pose.pushPose();
        pose.translate(0F, 0.45F + Mth.sin(state.time / 10F) * 0.1F, 0F);
        pose.rotate(Axis.YP.rotation(state.time / 20F));
        pose.scale(0.5F, 0.5F, 0.5F);
        state.pearl.submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        if (!state.fluid.isEmpty()) {
            drawFluid(pose, collector, state.fluid, state.capacity, state.lightCoords);
        }
        pose.popPose();
    }

    /** Draws a tank in block space (0..1), also used for the item. */
    public static void drawTank(PoseStack pose, SubmitNodeCollector collector, Direction facing, Frequency frequency, float valveAngle, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.rotate(Axis.YP.rotationDegrees(-facing.toYRot() + 180F));
        collector.submitCustomGeometry(pose, TANK_TYPE, (p, c) -> TankModel.drawBody(p, c, light, overlay));

        pose.pushPose();
        pose.translate(0F, 0.4165F, 0F);
        pose.rotate(Axis.ZP.rotationDegrees(valveAngle));
        pose.translate(0F, -0.4165F, 0F);
        collector.submitCustomGeometry(pose, TANK_TYPE, (p, c) -> TankModel.drawValve(p, c, frequency.hasOwner(), frequency.hasTeam(), light, overlay));
        pose.popPose();

        collector.submitCustomGeometry(pose, Buttons.RENDER_TYPE, (p, c) -> {
            for (int i = 0; i < 3; i++) {
                Buttons.draw(p, c, frequency.colour(i), (1 - i) * 0.15F, 0.91F, 0F, 0.6F, light, overlay);
            }
        });
        pose.popPose();
    }

    private static void drawFluid(PoseStack pose, SubmitNodeCollector collector, TankFluid fluid, int capacity, int light) {
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.variant().getFluid().defaultFluidState());
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int colour = FluidVariantRendering.getColor(fluid.variant()) | 0xFF000000;
        float fill = fluid.fill(capacity);
        float min = -0.28F;
        float max = 0.28F;
        float bottom = 0.12F;
        float top = 0.121F + 0.63F * fill;
        int fluidLight = Math.max(light, fluid.lightLevel() << 4);
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (p, c) -> {
            float u0 = sprite.getU0();
            float u1 = sprite.getU1();
            float v0 = sprite.getV0();
            float v1 = sprite.getV1();
            // Side faces only show the filled part of the texture
            float vSide = sprite.getV(1F - fill);
            int lightCoords = fluidLight;
            face(p, c, min, top, min, min, top, max, max, top, max, max, top, min, u0, v0, u1, v1, 0, 1, 0, colour, lightCoords);
            face(p, c, min, bottom, max, min, bottom, min, max, bottom, min, max, bottom, max, u0, v0, u1, v1, 0, -1, 0, colour, lightCoords);
            face(p, c, max, top, min, max, bottom, min, min, bottom, min, min, top, min, u0, vSide, u1, v1, 0, 0, -1, colour, lightCoords);
            face(p, c, min, top, max, min, bottom, max, max, bottom, max, max, top, max, u0, vSide, u1, v1, 0, 0, 1, colour, lightCoords);
            face(p, c, min, top, min, min, bottom, min, min, bottom, max, min, top, max, u0, vSide, u1, v1, -1, 0, 0, colour, lightCoords);
            face(p, c, max, top, max, max, bottom, max, max, bottom, min, max, top, min, u0, vSide, u1, v1, 1, 0, 0, colour, lightCoords);
        });
    }

    private static void face(PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer c,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz, int colour, int light) {
        c.addVertex(p, ax, ay, az).setColor(colour).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        c.addVertex(p, bx, by, bz).setColor(colour).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        c.addVertex(p, cx, cy, cz).setColor(colour).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        c.addVertex(p, dx, dy, dz).setColor(colour).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
