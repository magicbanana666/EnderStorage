package com.magicbanana666.enderstorage.block;

import java.util.EnumMap;
import java.util.Map;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.block.entity.EnderTankBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

public class EnderTankBlock extends EnderStorageBlock {

    public static final MapCodec<EnderTankBlock> CODEC = simpleCodec(EnderTankBlock::new);
    private static final VoxelShape BODY = Shapes.create(0.15, 0, 0.15, 0.85, 0.916, 0.85);

    /** For a tank facing south: the three buttons on top and the valve at the front. */
    private static final AABB[] PARTS = {
            new AABB(0.3125, 0.91, 0.425, 0.3875, 0.9475, 0.575),
            new AABB(0.4625, 0.91, 0.425, 0.5375, 0.9475, 0.575),
            new AABB(0.6125, 0.91, 0.425, 0.6875, 0.9475, 0.575),
            new AABB(0.338, 0.268, 0.85, 0.642, 0.565, 0.95)
    };
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public EnderTankBlock(Properties properties) {
        super(properties);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            this.shapes.put(facing, this.withParts(BODY, facing));
        }
    }

    @Override
    protected AABB[] parts() {
        return PARTS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (result != InteractionResult.TRY_WITH_EMPTY_HAND || this.partAt(state, pos, hit.getLocation()) == LOCK
                || !(level.getBlockEntity(pos) instanceof EnderTankBlockEntity tank)) {
            return result;
        }
        if (!tank.getFrequency().mayUse(player)) {
            return this.denied(player, tank.getFrequency());
        }
        // Buckets and other fluid containers fill or empty the tank. The client has no access to the shared tank,
        // so it only checks that the item holds fluid; the server does the work.
        if (ItemAccess.forPlayerInteraction(player, hand).getCapability(Capabilities.Fluid.ITEM) == null) {
            return result;
        }
        if (!level.isClientSide()) {
            FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection(), null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, EnderStorage.ENDER_TANK_BE.get(), (l, p, s, tank) -> tank.tick());
    }
}
