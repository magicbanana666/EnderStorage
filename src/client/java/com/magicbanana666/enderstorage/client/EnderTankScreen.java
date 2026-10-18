package com.magicbanana666.enderstorage.client;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.block.entity.EnderTankBlockEntity;
import com.magicbanana666.enderstorage.menu.EnderTankMenu;
import com.magicbanana666.enderstorage.storage.TankFluid;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Shows the fluid in an ender tank, its capacity, the pump speed, the valve and who may use it. */
public class EnderTankScreen extends AbstractContainerScreen<EnderTankMenu> {

    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int GAUGE_X = 8;
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_WIDTH = 34;
    private static final int GAUGE_HEIGHT = 72;
    private static final int TEXT_X = 50;
    private static final int TEXT_COLOUR = 0xFF404040;

    public EnderTankScreen(EnderTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 126);
    }

    private @Nullable EnderTankBlockEntity tank() {
        return this.minecraft != null && this.minecraft.player != null ? this.menu.tank(this.minecraft.player) : null;
    }

    private static String number(int value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        // A plain vanilla style panel: the top border, plain rows stretched down, and the bottom border
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y, 0F, 0F, 176, 4, 256, 256);
        for (int row = 4; row < this.imageHeight - 4; row++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + row, 0F, 10F, 176, 1, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + this.imageHeight - 4, 0F, 218F, 176, 4, 256, 256);

        // Gauge: an inset frame with the fluid filled from the bottom
        int gx = x + GAUGE_X;
        int gy = y + GAUGE_Y;
        graphics.fill(gx - 1, gy - 1, gx + GAUGE_WIDTH + 1, gy + GAUGE_HEIGHT + 1, 0xFF373737);
        graphics.fill(gx, gy, gx + GAUGE_WIDTH + 1, gy + GAUGE_HEIGHT + 1, 0xFFFFFFFF);
        graphics.fill(gx, gy, gx + GAUGE_WIDTH, gy + GAUGE_HEIGHT, 0xFF8B8B8B);
        EnderTankBlockEntity tank = this.tank();
        if (tank == null) {
            return;
        }
        TankFluid fluid = tank.getFluid();
        if (!fluid.isEmpty()) {
            int height = Math.max(1, Math.round(GAUGE_HEIGHT * fluid.fill(tank.capacity())));
            FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.variant().getFluid().defaultFluidState());
            TextureAtlasSprite sprite = model.stillMaterial().sprite();
            int colour = FluidVariantRendering.getColor(fluid.variant()) | 0xFF000000;
            graphics.enableScissor(gx, gy + GAUGE_HEIGHT - height, gx + GAUGE_WIDTH, gy + GAUGE_HEIGHT);
            for (int ty = GAUGE_HEIGHT - 16; ty > -16; ty -= 16) {
                for (int tx = 0; tx < GAUGE_WIDTH; tx += 16) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, gx + tx, gy + ty, 16, 16, colour);
                }
            }
            graphics.disableScissor();
        }
        // Scale marks every eighth
        for (int i = 1; i < 8; i++) {
            int my = gy + GAUGE_HEIGHT - i * GAUGE_HEIGHT / 8;
            graphics.fill(gx, my, gx + (i % 2 == 0 ? 10 : 6), my + 1, 0x99373737);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_COLOUR, false);
        EnderTankBlockEntity tank = this.tank();
        if (tank == null) {
            return;
        }
        TankFluid fluid = tank.getFluid();
        int line = GAUGE_Y;
        graphics.text(this.font, fluid.isEmpty() ? Component.translatable("enderstorage.gui.empty") : fluid.getHoverName(), TEXT_X, line, TEXT_COLOUR, false);
        line += 11;
        graphics.text(this.font, Component.translatable("enderstorage.gui.amount", number(fluid.mb()), number(tank.capacity())), TEXT_X, line, TEXT_COLOUR, false);
        line += 11;
        graphics.text(this.font, Component.translatable("enderstorage.gui.pump", tank.pumpUpgrades(), EnderStorageConfig.pumpMaxUpgrades(), number(tank.pumpRate())), TEXT_X, line, TEXT_COLOUR, false);
        line += 11;
        Component valve = Component.translatable(tank.hasPressure() ? "enderstorage.gui.valve_open" : "enderstorage.gui.valve_closed");
        graphics.text(this.font, tank.isRedstoneInverted() ? valve.copy().append(" ").append(Component.translatable("enderstorage.gui.inverted")) : valve, TEXT_X, line, TEXT_COLOUR, false);
        line += 11;
        Frequency frequency = tank.getFrequency();
        Component lock = frequency.hasOwner() ? Component.translatable("enderstorage.owner", frequency.ownerName().orElse("?"))
                : frequency.hasTeam() ? Component.translatable("enderstorage.team", frequency.teamName().orElse("?"))
                : Component.translatable("enderstorage.gui.public");
        graphics.text(this.font, lock, TEXT_X, line, TEXT_COLOUR, false);
        line += 11;
        graphics.text(this.font, frequency.describe(), TEXT_X, line, 0xFF707070, false);
        graphics.textWithWordWrap(this.font, Component.translatable("enderstorage.gui.tank_upgrade"), 8, GAUGE_Y + GAUGE_HEIGHT + 6, 160, 0xFF707070, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        EnderTankBlockEntity tank = this.tank();
        int gx = this.leftPos + GAUGE_X;
        int gy = this.topPos + GAUGE_Y;
        if (tank != null && mouseX >= gx && mouseX < gx + GAUGE_WIDTH && mouseY >= gy && mouseY < gy + GAUGE_HEIGHT) {
            TankFluid fluid = tank.getFluid();
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    fluid.isEmpty() ? Component.translatable("enderstorage.gui.empty") : fluid.getHoverName(),
                    Component.translatable("enderstorage.gui.amount", number(fluid.mb()), number(tank.capacity())).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }
}
