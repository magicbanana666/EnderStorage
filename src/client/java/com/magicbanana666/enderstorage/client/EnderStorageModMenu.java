package com.magicbanana666.enderstorage.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** The config screen in Mod Menu's mod list; only loaded when Mod Menu is installed. */
public final class EnderStorageModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EnderStorageConfigScreen::new;
    }
}
