package com.magicbanana666.enderstorage.client;

import java.util.LinkedHashMap;
import java.util.Map;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

/**
 * Edits the server config of the world that is open in singleplayer (from the mod list). Outside a world, or on a
 * server, the values are only shown.
 */
public class EnderStorageConfigScreen extends OptionsSubScreen {

    private static final int INVALID_COLOUR = 0xFFFF5555;

    /** The edited text of every option. */
    private final Map<EnderStorageConfig.Option<?>, String> edits = new LinkedHashMap<>();

    public EnderStorageConfigScreen(Screen lastScreen) {
        super(lastScreen, net.minecraft.client.Minecraft.getInstance().options, Component.translatable("enderstorage.config.title"));
    }

    private boolean editable() {
        return EnderStorageConfig.isEditable() && this.minecraft != null && this.minecraft.getSingleplayerServer() != null;
    }

    @Override
    protected void addOptions() {
        boolean editable = this.editable();
        if (!editable) {
            this.list.addHeader(Component.translatable("enderstorage.config.world_only").withStyle(ChatFormatting.GRAY));
        }
        String section = null;
        for (EnderStorageConfig.Option<?> option : EnderStorageConfig.options()) {
            if (!option.section().equals(section)) {
                section = option.section();
                this.list.addHeader(Component.literal(section));
            }
            StringWidget label = new StringWidget(150, 20, Component.literal(option.key()), this.font);
            label.setTooltip(Tooltip.create(this.tooltip(option)));
            this.list.addSmall(label, this.widget(option, editable));
        }
    }

    private Component tooltip(EnderStorageConfig.Option<?> option) {
        Component text = Component.literal(option.comment());
        if (option instanceof EnderStorageConfig.IntOption range) {
            text = text.copy().append("\n").append(Component.literal("Range: " + range.min() + " ~ " + range.max()).withStyle(ChatFormatting.GRAY));
        }
        return text.copy().append("\n").append(Component.literal("Default: " + option.defaultValue()).withStyle(ChatFormatting.GRAY));
    }

    private AbstractWidget widget(EnderStorageConfig.Option<?> option, boolean editable) {
        String value = String.valueOf(option.get());
        this.edits.put(option, value);
        if (option instanceof EnderStorageConfig.BooleanOption) {
            Button button = Button.builder(onOff(value), b -> {
                String flipped = String.valueOf(!Boolean.parseBoolean(this.edits.get(option)));
                this.edits.put(option, flipped);
                b.setMessage(onOff(flipped));
            }).width(150).build();
            button.active = editable;
            return button;
        }
        EditBox box = new EditBox(this.font, 150, 20, Component.literal(option.key()));
        box.setMaxLength(256);
        box.setValue(value);
        box.setEditable(editable);
        box.setResponder(text -> {
            this.edits.put(option, text);
            box.setTextColor(option.isValid(text) ? EditBox.DEFAULT_TEXT_COLOR : INVALID_COLOUR);
        });
        return box;
    }

    private static Component onOff(String value) {
        return Boolean.parseBoolean(value) ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }

    @Override
    public void removed() {
        super.removed();
        IntegratedServer server = this.minecraft != null ? this.minecraft.getSingleplayerServer() : null;
        if (!this.editable() || server == null) {
            return;
        }
        Map<EnderStorageConfig.Option<?>, String> edits = Map.copyOf(this.edits);
        // The server thread uses the values, so change them there; invalid text keeps the old value
        server.execute(() -> {
            edits.forEach((option, text) -> {
                if (!text.equals(String.valueOf(option.get())) && option.isValid(text)) {
                    option.parse(text);
                }
            });
            EnderStorageConfig.save(server);
        });
    }
}
