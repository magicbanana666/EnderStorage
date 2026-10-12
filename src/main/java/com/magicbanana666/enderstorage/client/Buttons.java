package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.Frequency;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

/** Draws the three colour buttons (small boxes with the colour from buttons.png). */
public final class Buttons {

    public static final RenderType RENDER_TYPE = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, "textures/buttons.png"));

    private Buttons() {
    }

    /**
     * Draws a button centred on (x, z) with its bottom at y. The size is 2 x 1 x 4 pixels, times scale.
     */
    public static void draw(PoseStack.Pose pose, VertexConsumer consumer, DyeColor colour, float x, float y, float z, float scale, int light, int overlay) {
        float w = 1F / 16F * scale;
        float d = 2F / 16F * scale;
        float h = 1F / 16F * scale;
        // Each colour has a 8x8 pixel cell in the 32x32 texture, the button uses the middle of it
        int id = colour.getId();
        float u0 = 0.25F * (id % 4) + 0.0938F;
        float v0 = 0.25F * (id / 4) + 0.0625F;
        box(pose, consumer, x - w, y, z - d, x + w, y + h, z + d, u0, v0, u0 + 0.0624F, v0 + 0.125F, light, overlay);
    }

    /** Draws the three buttons of a frequency: left at -spacing, middle at 0, right at +spacing on the x axis. */
    public static void drawAll(PoseStack.Pose pose, VertexConsumer consumer, Frequency frequency, float spacing, float y, float scale, int light, int overlay) {
        for (int i = 0; i < 3; i++) {
            draw(pose, consumer, frequency.colour(i), (i - 1) * spacing, y, 0F, scale, light, overlay);
        }
    }

    /** A box with the same texture region on every face. */
    public static void box(PoseStack.Pose pose, VertexConsumer c, float x1, float y1, float z1, float x2, float y2, float z2,
                           float u0, float v0, float u1, float v1, int light, int overlay) {
        // up
        quad(pose, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, u0, v0, u1, v1, 0, 1, 0, light, overlay);
        // down
        quad(pose, c, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, u0, v0, u1, v1, 0, -1, 0, light, overlay);
        // north
        quad(pose, c, x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1, u0, v0, u1, v1, 0, 0, -1, light, overlay);
        // south
        quad(pose, c, x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2, u0, v0, u1, v1, 0, 0, 1, light, overlay);
        // west
        quad(pose, c, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2, u0, v0, u1, v1, -1, 0, 0, light, overlay);
        // east
        quad(pose, c, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1, u0, v0, u1, v1, 1, 0, 0, light, overlay);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer c,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz, int light, int overlay) {
        vertex(pose, c, ax, ay, az, u0, v0, nx, ny, nz, light, overlay);
        vertex(pose, c, bx, by, bz, u0, v1, nx, ny, nz, light, overlay);
        vertex(pose, c, cx, cy, cz, u1, v1, nx, ny, nz, light, overlay);
        vertex(pose, c, dx, dy, dz, u1, v0, nx, ny, nz, light, overlay);
    }

    public static void vertex(PoseStack.Pose pose, VertexConsumer c, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int overlay) {
        c.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
