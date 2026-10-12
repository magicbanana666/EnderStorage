package com.magicbanana666.enderstorage.block;

import java.util.EnumMap;
import java.util.Map;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.block.entity.EnderChestBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EnderChestBlock extends EnderStorageBlock {

    public static final MapCodec<EnderChestBlock> CODEC = simpleCodec(EnderChestBlock::new);
    private static final VoxelShape BODY = Block.box(1, 0, 1, 15, 14, 15);

    /** For a chest facing south: the three buttons on the lid and the lock at the front. */
    private static final AABB[] PARTS = {
            part(4, 14, 6, 6, 15, 10),
            part(7, 14, 6, 9, 15, 10),
            part(10, 14, 6, 12, 15, 10),
            part(6.5, 6.5, 15, 9.5, 11.5, 16)
    };
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public EnderChestBlock(Properties properties) {
        super(properties);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            this.shapes.put(facing, this.withParts(BODY, facing));
        }
    }

    private static AABB part(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABB(x1 / 16D, y1 / 16D, z1 / 16D, x2 / 16D, y2 / 16D, z2 / 16D);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, EnderStorage.ENDER_CHEST_BE.get(), (l, p, s, chest) -> chest.tick());
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }
}
