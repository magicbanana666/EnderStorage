package com.magicbanana666.enderstorage.storage;

import java.util.ArrayList;
import java.util.List;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.menu.EnderChestMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** The shared inventory of one frequency. Upgrades add slots up to the configured maximum. */
public class ItemStorage implements Container {

    private final SharedStorage owner;
    private final Frequency frequency;
    private final NonNullList<ItemStack> items = NonNullList.create();
    /** Items that didn't fit after the storage was made smaller; they come back when there is room. */
    private final List<ItemStack> overflow = new ArrayList<>();
    /** Slots added by upgrades. */
    private int bonusSlots;
    private int openCount;
    private int changeCount;

    ItemStorage(SharedStorage owner, Frequency frequency) {
        this.owner = owner;
        this.frequency = frequency;
        this.resize();
    }

    void load(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.items.size() && this.items.get(slot).isEmpty()) {
            this.items.set(slot, stack);
        } else {
            this.overflow.add(stack);
        }
    }

    void loadBonusSlots(int bonus) {
        this.bonusSlots = Math.max(0, bonus);
        this.resize();
    }

    /** Matches the number of slots to the config and the upgrades; slots that are removed go to the overflow. */
    private void resize() {
        int size = Math.min(EnderStorageConfig.chestStartSlots() + this.bonusSlots, EnderStorageConfig.chestMaxSlots());
        while (this.items.size() < size) {
            this.items.add(ItemStack.EMPTY);
        }
        while (this.items.size() > size) {
            ItemStack removed = this.items.removeLast();
            if (!removed.isEmpty()) {
                this.overflow.add(removed);
            }
        }
    }

    List<ItemStack> overflow() {
        return this.overflow;
    }

    int bonusSlots() {
        return this.bonusSlots;
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

    public boolean isFullyUpgraded() {
        return this.items.size() >= EnderStorageConfig.chestMaxSlots();
    }

    /** Adds slots (up to the maximum) and returns the new number of slots. */
    public int upgrade(int slots) {
        this.bonusSlots = Math.min(this.bonusSlots + slots, EnderStorageConfig.chestMaxSlots() - EnderStorageConfig.chestStartSlots());
        this.resize();
        this.restoreOverflow();
        this.setChanged();
        return this.items.size();
    }

    /** Opens the inventory; {@code pos} is the chest it was opened from, or null when opened from an item. */
    public void openMenu(ServerPlayer player, Component title, @Nullable BlockPos pos) {
        this.resize();
        this.restoreOverflow();
        this.giveOverflow(player);
        player.openMenu(new ExtendedMenuProvider<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer p) {
                return ItemStorage.this.items.size();
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new EnderChestMenu(id, inventory, ItemStorage.this, pos);
            }
        });
    }

    private void restoreOverflow() {
        for (int slot = 0; slot < this.items.size() && !this.overflow.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, this.overflow.removeFirst());
                this.setChanged();
            }
        }
    }

    /** Overflow that still doesn't fit goes to the player (dropped at their feet if their inventory is full). */
    private void giveOverflow(ServerPlayer player) {
        if (this.overflow.isEmpty()) {
            return;
        }
        int stacks = this.overflow.size();
        while (!this.overflow.isEmpty()) {
            player.getInventory().placeItemBackInInventory(this.overflow.removeFirst(), Prediction.SERVER_ONLY);
        }
        this.setChanged();
        player.sendSystemMessage(Component.translatable("enderstorage.overflow", stacks));
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
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
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
        if (slot < 0 || slot >= this.items.size()) {
            return;
        }
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
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.setChanged();
    }
}
