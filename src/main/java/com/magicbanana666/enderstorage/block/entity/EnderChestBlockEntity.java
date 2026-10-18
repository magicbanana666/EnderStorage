package com.magicbanana666.enderstorage.block.entity;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.storage.ItemStorage;
import com.magicbanana666.enderstorage.storage.SharedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class EnderChestBlockEntity extends FrequencyBlockEntity {

    private static final int OPEN_EVENT = 1;

    /** Lid openness from 0 (closed) to 1 (open), and its value last tick. */
    public float openness;
    public float prevOpenness;
    private int openCount;
    private int lastChangeCount = -1;
    private @Nullable Storage<ItemVariant> itemHandler;

    public EnderChestBlockEntity(BlockPos pos, BlockState state) {
        super(EnderStorage.ENDER_CHEST_BE, pos, state);
    }

    public @Nullable ItemStorage getStorage() {
        SharedStorage shared = this.shared();
        return shared != null ? shared.chest(this.frequency) : null;
    }

    public @Nullable Storage<ItemVariant> getItemHandler() {
        if (this.itemHandler == null) {
            ItemStorage storage = this.getStorage();
            if (storage != null) {
                this.itemHandler = ContainerStorage.of(storage, null);
            }
        }
        return this.itemHandler;
    }

    @Override
    protected void onFrequencyChanged() {
        this.itemHandler = null;
        super.onFrequencyChanged();
    }

    public void tick() {
        if (this.level == null) {
            return;
        }
        if (!this.level.isClientSide()) {
            ItemStorage storage = this.getStorage();
            if (storage != null) {
                if (storage.openCount() != this.openCount || this.level.getGameTime() % 20 == 0) {
                    this.openCount = storage.openCount();
                    this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), OPEN_EVENT, this.openCount);
                }
                if (storage.changeCount() != this.lastChangeCount) {
                    this.lastChangeCount = storage.changeCount();
                    this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
                }
            }
        }
        this.prevOpenness = this.openness;
        float target = this.openCount > 0 ? 1F : 0F;
        this.openness = Mth.approach(this.openness, target, 0.1F);
        if (this.prevOpenness >= 0.5F && this.openness < 0.5F) {
            this.playSound(EnderStorageConfig.vanillaSounds() ? SoundEvents.ENDER_CHEST_CLOSE : SoundEvents.CHEST_CLOSE);
        } else if (this.prevOpenness == 0F && this.openness > 0F) {
            this.playSound(EnderStorageConfig.vanillaSounds() ? SoundEvents.ENDER_CHEST_OPEN : SoundEvents.CHEST_OPEN);
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        if (this.level != null && this.level.isClientSide()) {
            this.level.playLocalSound(this.worldPosition, sound, SoundSource.BLOCKS, 0.5F, this.level.getRandom().nextFloat() * 0.1F + 0.9F, false);
        }
    }

    /** The lid angle in radians for rendering, eased like the vanilla chest. */
    public float lidAngle(float partialTick) {
        float open = 1F - Mth.lerp(partialTick, this.prevOpenness, this.openness);
        open = 1F - open * open * open;
        return -(open * Mth.HALF_PI);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == OPEN_EVENT) {
            this.openCount = param;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    public InteractionResult activate(Player player, int part) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStorage storage = this.getStorage();
            if (storage != null) {
                storage.openMenu(serverPlayer, Component.translatable(this.getBlockState().getBlock().getDescriptionId()), this.worldPosition);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isUpgradeItem(ItemStack stack) {
        return EnderStorageConfig.chestUpgradeSlots(stack) > 0;
    }

    @Override
    public void upgrade(Player player, ItemStack stack) {
        ItemStorage storage = this.getStorage();
        if (storage == null) {
            return;
        }
        if (storage.isFullyUpgraded()) {
            player.sendOverlayMessage(Component.translatable("enderstorage.upgrade.chest_full", storage.getContainerSize()));
            return;
        }
        int slots = storage.upgrade(EnderStorageConfig.chestUpgradeSlots(stack));
        this.upgraded(player, stack, Component.translatable("enderstorage.upgrade.chest", slots, EnderStorageConfig.chestMaxSlots()));
    }

    @Override
    public int comparatorOutput() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this.getStorage());
    }
}
