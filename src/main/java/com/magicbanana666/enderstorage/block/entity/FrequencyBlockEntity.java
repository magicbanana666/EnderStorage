package com.magicbanana666.enderstorage.block.entity;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.storage.SharedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A block entity linked to the shared storage of its frequency. */
public abstract class FrequencyBlockEntity extends BlockEntity {

    protected Frequency frequency = Frequency.DEFAULT;
    /** The item used to lock this block (empty if unknown or not locked). */
    private ItemStack lockItem = ItemStack.EMPTY;

    protected FrequencyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Frequency getFrequency() {
        return this.frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
        this.onFrequencyChanged();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /** Locks the block with the given item (one of it is kept to give back later). */
    public void lock(Frequency locked, ItemStack item) {
        this.lockItem = item.copyWithCount(1);
        this.setFrequency(locked);
    }

    /** Removes the lock and returns the item that locked it, or the given fallback if that is unknown. */
    public ItemStack unlock(ItemStack fallback) {
        ItemStack item = this.lockItem.isEmpty() ? fallback : this.lockItem;
        this.lockItem = ItemStack.EMPTY;
        this.setFrequency(this.frequency.withoutLock());
        return item;
    }

    protected void onFrequencyChanged() {
        this.invalidateCapabilities();
    }

    /** The shared storage of the world, only on the server. */
    protected @Nullable SharedStorage shared() {
        return this.level != null && this.level.getServer() != null ? SharedStorage.get(this.level.getServer()) : null;
    }

    public abstract InteractionResult activate(Player player, int part);

    /** Whether right clicking with this item upgrades the block (also checked on the client). */
    public abstract boolean isUpgradeItem(ItemStack stack);

    /** Server side: applies the upgrade item, uses it up and tells the player the result. */
    public abstract void upgrade(Player player, ItemStack stack);

    protected void upgraded(Player player, ItemStack stack, Component message) {
        stack.consume(1, player);
        player.sendOverlayMessage(message);
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 1F, 1.2F);
        }
    }

    public abstract int comparatorOutput();

    public int lightLevel() {
        return 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Frequency", Frequency.CODEC, this.frequency);
        if (!this.lockItem.isEmpty()) {
            output.store("LockItem", ItemStack.CODEC, this.lockItem);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Frequency old = this.frequency;
        this.frequency = input.read("Frequency", Frequency.CODEC).orElse(Frequency.DEFAULT);
        this.lockItem = input.read("LockItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!old.equals(this.frequency)) {
            this.onFrequencyChanged();
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.frequency = components.getOrDefault(EnderStorage.FREQUENCY.get(), Frequency.DEFAULT);
        this.lockItem = this.frequency.isLocked() ? components.getOrDefault(EnderStorage.LOCK_ITEM.get(), ItemStack.EMPTY).copy() : ItemStack.EMPTY;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        // In anarchy mode a broken chest loses its owner or team
        components.set(EnderStorage.FREQUENCY.get(), EnderStorageConfig.anarchyMode() ? this.frequency.withoutLock() : this.frequency);
        if (!EnderStorageConfig.anarchyMode() && this.frequency.isLocked() && !this.lockItem.isEmpty()) {
            components.set(EnderStorage.LOCK_ITEM.get(), this.lockItem.copy());
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Frequency");
        output.discard("LockItem");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
