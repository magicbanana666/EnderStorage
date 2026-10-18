package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;

public final class EnderStorageClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(EnderStorage.ENDER_CHEST_MENU, EnderChestScreen::new);
        MenuScreens.register(EnderStorage.ENDER_TANK_MENU, EnderTankScreen::new);
        BlockEntityRendererRegistry.register(EnderStorage.ENDER_CHEST_BE, EnderChestRenderer::new);
        BlockEntityRendererRegistry.register(EnderStorage.ENDER_TANK_BE, EnderTankRenderer::new);
        SpecialModelRenderers.ID_MAPPER.put(id("ender_chest"), FrequencyItemRenderers.Chest.CODEC);
        SpecialModelRenderers.ID_MAPPER.put(id("ender_tank"), FrequencyItemRenderers.Tank.CODEC);
        ConditionalItemModelProperties.ID_MAPPER.put(id("bag/owned"), PouchProperties.Owned.CODEC);
        ConditionalItemModelProperties.ID_MAPPER.put(id("bag/open"), PouchProperties.Open.CODEC);
        SelectItemModelProperties.ID_MAPPER.put(id("bag/frequency"), PouchProperties.Colour.TYPE);
        // The server config comes from the server
        ClientPlayNetworking.registerGlobalReceiver(EnderStorageConfig.SyncPayload.TYPE, (payload, context) -> EnderStorageConfig.receive(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EnderStorageConfig.disconnected());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, path);
    }
}
