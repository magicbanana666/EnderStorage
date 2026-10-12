package com.magicbanana666.enderstorage.item;

import java.util.function.Consumer;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.Frequency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of an ender chest or tank; it keeps its frequency. */
public class FrequencyBlockItem extends BlockItem {

    public FrequencyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        addFrequencyTooltip(Frequency.get(stack), tooltip);
        int pumps = stack.getOrDefault(EnderStorage.PUMP_UPGRADES.get(), 0);
        if (pumps > 0) {
            tooltip.accept(Component.translatable("enderstorage.pump_upgrades", pumps).withStyle(ChatFormatting.GRAY));
        }
    }

    public static void addFrequencyTooltip(Frequency frequency, Consumer<Component> tooltip) {
        tooltip.accept(frequency.describe().withStyle(ChatFormatting.GRAY));
        frequency.ownerName().ifPresent(name -> tooltip.accept(Component.translatable("enderstorage.owner", name).withStyle(ChatFormatting.DARK_PURPLE)));
        frequency.teamName().ifPresent(name -> tooltip.accept(Component.translatable("enderstorage.team", name).withStyle(ChatFormatting.DARK_GREEN)));
    }
}
