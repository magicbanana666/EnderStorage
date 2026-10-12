package com.magicbanana666.enderstorage.item;

import java.util.function.Consumer;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.block.entity.EnderChestBlockEntity;
import com.magicbanana666.enderstorage.storage.SharedStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Opens the inventory of its frequency from anywhere. Sneak + right click an ender chest to give the pouch that
 * chest's frequency.
 */
public class EnderPouchItem extends Item {

    public EnderPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        FrequencyBlockItem.addFrequencyTooltip(Frequency.get(stack), tooltip);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof EnderChestBlockEntity chest)) {
            return InteractionResult.PASS;
        }
        Frequency frequency = chest.getFrequency();
        if (frequency.hasOwner() && !frequency.isOwner(player) && !EnderStorageConfig.anarchyMode()) {
            // Someone else's personal chest: copy the colours only
            frequency = frequency.withoutOwner();
        }
        if (!context.getLevel().isClientSide()) {
            frequency.applyTo(context.getItemInHand());
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Frequency frequency = Frequency.get(stack);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.level().getServer() != null) {
            if (frequency.hasOwner() && !frequency.isOwner(player) && !EnderStorageConfig.anarchyMode() && !player.hasInfiniteMaterials()) {
                player.sendOverlayMessage(Component.translatable("enderstorage.not_owner", frequency.ownerName().orElse("?")));
                return InteractionResult.FAIL;
            }
            SharedStorage.get(serverPlayer.level().getServer()).chest(frequency).openMenu(serverPlayer, stack.getHoverName());
        }
        return InteractionResult.SUCCESS;
    }
}
