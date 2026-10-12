package com.magicbanana666.enderstorage.client;

import java.util.List;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.menu.EnderChestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The ender chest screen: the vanilla chest look with up to six rows, a scroll bar on the right when the (upgraded)
 * inventory has more rows, and the number of slots next to the title.
 */
public class EnderChestScreen extends AbstractContainerScreen<EnderChestMenu> {

    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final int BACKGROUND = 0xFFC6C6C6;
    /** Extra width for the scroll bar strip on the right. */
    private static final int STRIP = 18;

    private final int rows;
    private boolean scrolling;
    /** The row last asked from the server, to not send the same request every frame while dragging. */
    private int requestedRow = -1;

    public EnderChestScreen(EnderChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176 + STRIP, 114 + menu.visibleRows() * 18);
        this.rows = menu.visibleRows();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private int topHeight() {
        return this.rows * 18 + 17;
    }

    private int trackX() {
        return this.leftPos + 174;
    }

    private int trackY() {
        return this.topPos + 18;
    }

    private int trackHeight() {
        return this.rows * 18;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        // Chest rows: the texture without its right border, a plain strip for the scroll bar, then the right border
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y, 0F, 0F, 172, this.topHeight(), 256, 256);
        for (int i = 0; i < STRIP; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x + 172 + i, y, 170F, 0F, 1, this.topHeight(), 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x + 172 + STRIP, y, 172F, 0F, 4, this.topHeight(), 256, 256);
        // Player inventory
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + this.topHeight(), 0F, 126F, 176, 96, 256, 256);
        // Close the bottom of the strip, which sticks out past the player inventory
        int bottom = y + this.topHeight();
        graphics.fill(x + 176, bottom - 1, x + 192, bottom + 3, BACKGROUND);
        graphics.fill(x + 176, bottom + 1, x + 193, bottom + 3, 0xFF555555);
        graphics.fill(x + 176, bottom + 3, x + 194, bottom + 4, 0xFF000000);
        graphics.fill(x + 193, bottom - 1, x + 194, bottom + 3, 0xFF000000);
        graphics.fill(x + 192, bottom - 1, x + 193, bottom + 1, 0xFF555555);

        // Hide the slot frames past the end of the storage
        for (int i = 0; i < this.rows * EnderChestMenu.COLUMNS; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.isActive()) {
                graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, BACKGROUND);
            }
        }

        // Scroll bar
        int tx = this.trackX();
        int ty = this.trackY();
        graphics.fill(tx, ty, tx + 14, ty + this.trackHeight(), 0xFF8B8B8B);
        graphics.fill(tx, ty, tx + 13, ty + 1, 0xFF373737);
        graphics.fill(tx, ty, tx + 1, ty + this.trackHeight() - 1, 0xFF373737);
        graphics.fill(tx + 1, ty + this.trackHeight() - 1, tx + 14, ty + this.trackHeight(), 0xFFFFFFFF);
        graphics.fill(tx + 13, ty + 1, tx + 14, ty + this.trackHeight(), 0xFFFFFFFF);
        int max = this.menu.maxScroll();
        int scrollerY = max == 0 ? 0 : Math.round((this.trackHeight() - 17) * (this.menu.scrollRow() / (float) max));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, max > 0 ? SCROLLER : SCROLLER_DISABLED, tx + 1, ty + 1 + scrollerY, 12, 15);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        Component count = this.countLabel();
        graphics.text(this.font, count, 168 - this.font.width(count), this.titleLabelY, 0xFF404040, false);
    }

    private Component countLabel() {
        return Component.literal(this.menu.slotCount() + "/" + EnderStorageConfig.chestMaxSlots());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int width = this.font.width(this.countLabel());
        int lx = this.leftPos + 168 - width;
        int ly = this.topPos + this.titleLabelY;
        if (mouseX >= lx && mouseX < lx + width && mouseY >= ly - 1 && mouseY < ly + 9) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("enderstorage.gui.slots", this.menu.slotCount(), EnderStorageConfig.chestMaxSlots()),
                    Component.translatable("enderstorage.gui.chest_upgrade").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private boolean overTrack(double mx, double my) {
        return mx >= this.trackX() && mx < this.trackX() + 14 && my >= this.trackY() && my < this.trackY() + this.trackHeight();
    }

    private void scrollTo(int row) {
        int clamped = Mth.clamp(row, 0, this.menu.maxScroll());
        if (clamped != this.menu.scrollRow() && clamped != this.requestedRow && this.minecraft != null && this.minecraft.gameMode != null) {
            this.requestedRow = clamped;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, clamped);
        }
    }

    private void dragTo(double my) {
        float progress = (float) ((my - this.trackY() - 7.5) / (this.trackHeight() - 17));
        this.scrollTo(Math.round(Mth.clamp(progress, 0F, 1F) * this.menu.maxScroll()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.menu.maxScroll() > 0 && this.overTrack(event.x(), event.y())) {
            this.scrolling = true;
            this.dragTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrolling) {
            this.dragTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.scrolling) {
            this.scrolling = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.menu.maxScroll() > 0 && y < this.topPos + this.topHeight() && scrollY != 0) {
            int from = this.requestedRow >= 0 && this.requestedRow != this.menu.scrollRow() ? this.requestedRow : this.menu.scrollRow();
            int rows = (int) Math.signum(scrollY) * Math.max(1, (int) Math.round(Math.abs(scrollY)));
            this.scrollTo(from - rows);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }
}
