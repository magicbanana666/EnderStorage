package com.magicbanana666.enderstorage.menu;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.storage.ItemStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The ender chest inventory. It shows up to six rows at a time; bigger (upgraded) inventories scroll. The slots show
 * a window of the storage that starts at the scrolled row, so only the visible part is synced to the client.
 */
public class EnderChestMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 9;
    public static final int MAX_VISIBLE_ROWS = 6;

    private final @Nullable ItemStorage storage;
    private final int visibleRows;
    private final int viewSize;
    /** Number of slots of the storage. */
    private final DataSlot size = DataSlot.standalone();
    /** The first visible row. */
    private final DataSlot scroll = DataSlot.standalone();

    /** Client side: the server sends the number of slots. */
    public EnderChestMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, null, buf.readVarInt());
    }

    public EnderChestMenu(int id, Inventory inventory, ItemStorage storage) {
        this(id, inventory, storage, storage.getContainerSize());
    }

    private EnderChestMenu(int id, Inventory inventory, @Nullable ItemStorage storage, int slots) {
        super(EnderStorage.ENDER_CHEST_MENU.get(), id);
        this.storage = storage;
        this.visibleRows = Mth.clamp(Mth.positiveCeilDiv(slots, COLUMNS), 1, MAX_VISIBLE_ROWS);
        this.viewSize = this.visibleRows * COLUMNS;
        this.size.set(slots);
        this.addDataSlot(this.size);
        this.addDataSlot(this.scroll);

        Container view = storage != null ? new View() : new SimpleContainer(this.viewSize);
        for (int row = 0; row < this.visibleRows; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                this.addSlot(new ViewSlot(view, row * COLUMNS + column, 8 + column * 18, 18 + row * 18));
            }
        }
        this.addStandardInventorySlots(inventory, 8, 18 + this.visibleRows * 18 + 13);
        if (storage != null) {
            storage.startOpen(inventory.player);
        }
    }

    public int visibleRows() {
        return this.visibleRows;
    }

    public int slotCount() {
        return this.size.get();
    }

    public int totalRows() {
        return Mth.positiveCeilDiv(this.size.get(), COLUMNS);
    }

    public int maxScroll() {
        return Math.max(0, this.totalRows() - this.visibleRows);
    }

    public int scrollRow() {
        return this.scroll.get();
    }

    /** The scroll buttons send the row to show. */
    @Override
    public boolean clickMenuButton(Player player, int row) {
        int clamped = Mth.clamp(row, 0, this.maxScroll());
        if (clamped != this.scroll.get()) {
            this.scroll.set(clamped);
        }
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (this.storage != null) {
            this.size.set(this.storage.getContainerSize());
            if (this.scroll.get() > this.maxScroll()) {
                this.scroll.set(this.maxScroll());
            }
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < this.viewSize) {
            if (!this.moveItemStackTo(stack, this.viewSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Into the whole storage, also the rows that are scrolled out of view; the server decides
            if (this.storage == null || !this.insertIntoStorage(stack)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Merges the stack into the storage, first onto matching stacks, then into empty slots. */
    private boolean insertIntoStorage(ItemStack stack) {
        int before = stack.getCount();
        int slots = this.storage.getContainerSize();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < slots && !stack.isEmpty(); i++) {
                ItemStack existing = this.storage.getItem(i);
                if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                    int move = Math.min(stack.getCount(), this.storage.getMaxStackSize(existing) - existing.getCount());
                    if (move > 0) {
                        existing.grow(move);
                        stack.shrink(move);
                        this.storage.setChanged();
                    }
                } else if (pass == 1 && existing.isEmpty()) {
                    this.storage.setItem(i, stack.split(Math.min(stack.getCount(), this.storage.getMaxStackSize(stack))));
                }
            }
        }
        return stack.getCount() != before;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.storage == null || this.storage.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.storage != null) {
            this.storage.stopOpen(player);
        }
    }

    /** Server side: the visible rows of the storage, starting at the scrolled row. */
    private class View implements Container {

        private int index(int slot) {
            return EnderChestMenu.this.scroll.get() * COLUMNS + slot;
        }

        private ItemStorage storage() {
            return EnderChestMenu.this.storage;
        }

        @Override
        public int getContainerSize() {
            return EnderChestMenu.this.viewSize;
        }

        @Override
        public boolean isEmpty() {
            return this.storage().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.storage().getItem(this.index(slot));
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return this.index(slot) < this.storage().getContainerSize() ? this.storage().removeItem(this.index(slot), count) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return this.index(slot) < this.storage().getContainerSize() ? this.storage().removeItemNoUpdate(this.index(slot)) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.storage().setItem(this.index(slot), stack);
        }

        @Override
        public void setChanged() {
            this.storage().setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
        }
    }

    /** A slot that is hidden and disabled past the end of the storage. */
    private class ViewSlot extends Slot {

        ViewSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return EnderChestMenu.this.scroll.get() * COLUMNS + this.getContainerSlot() < EnderChestMenu.this.size.get();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isActive();
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.isActive();
        }
    }
}
