package com.magicbanana666.enderstorage.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.magicbanana666.enderstorage.EnderStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * The ender tank model, read from models/endertank.obj. The file is Z-up, so Y and Z are swapped, and it is moved so
 * the tank is centred on x/z = 0.
 */
public final class TankModel {

    private static final Identifier LOCATION = Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, "models/endertank.obj");
    private static final float OFFSET_X = -0.0099F - 0.5F;
    private static final float OFFSET_Z = -0.0027F - 0.5F;

    /** One corner: position, texture coordinate and normal. */
    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }

    private static Map<String, List<Vertex[]>> groups;

    private TankModel() {
    }

    private static Map<String, List<Vertex[]>> groups() {
        if (groups == null) {
            groups = load();
        }
        return groups;
    }

    private static Map<String, List<Vertex[]>> load() {
        Map<String, List<Vertex[]>> result = new HashMap<>();
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        String group = "default";
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(LOCATION)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(new float[] { f(parts[1]) + OFFSET_X, f(parts[3]), f(parts[2]) + OFFSET_Z });
                    case "vt" -> uvs.add(new float[] { f(parts[1]), 1F - f(parts[2]) });
                    case "vn" -> normals.add(new float[] { f(parts[1]), f(parts[3]), f(parts[2]) });
                    case "g", "o" -> group = parts[1];
                    case "f" -> {
                        Vertex[] face = new Vertex[parts.length - 1];
                        for (int i = 1; i < parts.length; i++) {
                            String[] idx = parts[i].split("/");
                            float[] p = positions.get(Integer.parseInt(idx[0]) - 1);
                            float[] t = idx.length > 1 && !idx[1].isEmpty() ? uvs.get(Integer.parseInt(idx[1]) - 1) : new float[2];
                            float[] n = idx.length > 2 ? normals.get(Integer.parseInt(idx[2]) - 1) : new float[] { 0, 1, 0 };
                            face[i - 1] = new Vertex(p[0], p[1], p[2], t[0], t[1], n[0], n[1], n[2]);
                        }
                        result.computeIfAbsent(group, g -> new ArrayList<>()).add(face);
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            EnderStorageClientLog.LOGGER.error("Could not load the ender tank model", e);
        }
        return result;
    }

    private static float f(String s) {
        return Float.parseFloat(s);
    }

    /** Draws every group except the valve. */
    public static void drawBody(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        for (Map.Entry<String, List<Vertex[]>> entry : groups().entrySet()) {
            if (!entry.getKey().equals("Valve")) {
                draw(entry.getValue(), pose, consumer, 0F, light, overlay);
            }
        }
    }

    /** Draws the valve; a personal tank uses the diamond valve texture (13 pixels lower). */
    public static void drawValve(PoseStack.Pose pose, VertexConsumer consumer, boolean owned, int light, int overlay) {
        draw(groups().getOrDefault("Valve", List.of()), pose, consumer, owned ? 13F / 64F : 0F, light, overlay);
    }

    private static void draw(List<Vertex[]> faces, PoseStack.Pose pose, VertexConsumer consumer, float vOffset, int light, int overlay) {
        for (Vertex[] face : faces) {
            // The render type draws quads: repeat the last corner of triangles
            for (int i = 0; i < 4; i++) {
                Vertex v = face[Math.min(i, face.length - 1)];
                consumer.addVertex(pose, v.x(), v.y(), v.z()).setColor(-1).setUv(v.u(), v.v() + vOffset).setOverlay(overlay).setLight(light)
                        .setNormal(pose, v.nx(), v.ny(), v.nz());
            }
        }
    }
}
