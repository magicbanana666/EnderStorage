package com.magicbanana666.enderstorage.menu;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.block.entity.EnderTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** The ender tank screen has no slots; it shows what the tank's block entity syncs to the client. */
public class EnderTankMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    /** Client side: the server sends the position of the tank. */
    public EnderTankMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, pos);
    }

    public EnderTankMenu(int id, BlockPos pos) {
        super(EnderStorage.ENDER_TANK_MENU, id);
        this.pos = pos;
    }

    public @Nullable EnderTankBlockEntity tank(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof EnderTankBlockEntity tank ? tank : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tank(player) != null && player.isWithinBlockInteractionRange(this.pos, 4.0);
    }
}
