package com.magicbanana666.enderstorage.storage;

import java.util.ArrayList;
import java.util.List;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** The shared inventory of one frequency. */
public class ItemStorage implements Container {

    private final SharedStorage owner;
    private final Frequency frequency;
    private final NonNullList<ItemStack> items;
    /** Items that didn't fit after the storage size was made smaller; they come back when there is room. */
    private final List<ItemStack> overflow = new ArrayList<>();
    private int openCount;
    private int changeCount;

    ItemStorage(SharedStorage owner, Frequency frequency) {
        this.owner = owner;
        this.frequency = frequency;
        this.items = NonNullList.withSize(EnderStorageConfig.storageSlots(), ItemStack.EMPTY);
    }

    void load(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.items.size() && this.items.get(slot).isEmpty()) {
            this.items.set(slot, stack);
        } else {
            this.overflow.add(stack);
        }
    }

    List<ItemStack> overflow() {
        return this.overflow;
    }

    public Frequency frequency() {
        return this.frequency;
    }

    public int openCount() {
        return this.openCount;
    }

    public int changeCount() {
        return this.changeCount;
    }

    public void openMenu(ServerPlayer player, Component title) {
        this.restoreOverflow();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> switch (this.items.size()) {
            case 9 -> new DispenserMenu(id, inventory, this);
            case 54 -> new ChestMenu(MenuType.GENERIC_9x6, id, inventory, this, 6);
            default -> new ChestMenu(MenuType.GENERIC_9x3, id, inventory, this, 3);
        }, title));
    }

    private void restoreOverflow() {
        for (int slot = 0; slot < this.items.size() && !this.overflow.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, this.overflow.removeFirst());
                this.setChanged();
            }
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, count);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public void setChanged() {
        this.changeCount++;
        this.owner.setDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(ContainerUser user) {
        this.openCount++;
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.openCount = Math.max(0, this.openCount - 1);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }
}
