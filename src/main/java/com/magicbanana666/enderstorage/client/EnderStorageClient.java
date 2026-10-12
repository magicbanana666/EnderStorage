package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.EnderStorage;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EnderStorage.MOD_ID, dist = Dist.CLIENT)
public final class EnderStorageClient {

    public EnderStorageClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(RegisterMenuScreensEvent.class, event -> {
            event.register(EnderStorage.ENDER_CHEST_MENU.get(), EnderChestScreen::new);
            event.register(EnderStorage.ENDER_TANK_MENU.get(), EnderTankScreen::new);
        });
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerBlockEntityRenderer(EnderStorage.ENDER_CHEST_BE.get(), EnderChestRenderer::new);
            event.registerBlockEntityRenderer(EnderStorage.ENDER_TANK_BE.get(), EnderTankRenderer::new);
        });
        modBus.addListener(RegisterSpecialModelRendererEvent.class, event -> {
            event.register(id("ender_chest"), FrequencyItemRenderers.Chest.CODEC);
            event.register(id("ender_tank"), FrequencyItemRenderers.Tank.CODEC);
        });
        modBus.addListener(RegisterConditionalItemModelPropertyEvent.class, event -> {
            event.register(id("bag/owned"), PouchProperties.Owned.CODEC);
            event.register(id("bag/open"), PouchProperties.Open.CODEC);
        });
        modBus.addListener(RegisterSelectItemModelPropertyEvent.class, event -> event.register(id("bag/frequency"), PouchProperties.Colour.TYPE));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, path);
    }
}
