package com.magicbanana666.enderstorage.block;

import java.util.Optional;

import com.magicbanana666.enderstorage.EnderStorageConfig;
import com.magicbanana666.enderstorage.Frequency;
import com.magicbanana666.enderstorage.Teams;
import com.magicbanana666.enderstorage.block.entity.FrequencyBlockEntity;
import com.magicbanana666.enderstorage.recipe.RecolourRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base of the ender chest and tank. Both have three colour buttons (dye them to change the frequency) and a lock
 * or valve (the personal item makes it personal, the team item makes it a team block, sneak + empty hand takes the
 * item back). Upgrade items make the storage of the frequency bigger.
 */
public abstract class EnderStorageBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int NONE = 0;
    public static final int LOCK = 4;

    protected EnderStorageBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    /** The parts that can be clicked, for a block facing south: index 0-2 are the buttons, 3 the lock/valve. */
    protected abstract AABB[] parts();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Rotates a box made for a block facing south to the given facing. */
    public static AABB rotate(AABB box, Direction facing) {
        int steps = facing.get2DDataValue();
        double x1 = box.minX - 0.5, z1 = box.minZ - 0.5, x2 = box.maxX - 0.5, z2 = box.maxZ - 0.5;
        for (int i = 0; i < steps; i++) {
            double nx1 = -z1, nz1 = x1, nx2 = -z2, nz2 = x2;
            x1 = nx1;
            z1 = nz1;
            x2 = nx2;
            z2 = nz2;
        }
        return new AABB(Math.min(x1, x2) + 0.5, box.minY, Math.min(z1, z2) + 0.5, Math.max(x1, x2) + 0.5, box.maxY, Math.max(z1, z2) + 0.5);
    }

    protected VoxelShape withParts(VoxelShape body, Direction facing) {
        VoxelShape shape = body;
        for (AABB part : this.parts()) {
            shape = Shapes.joinUnoptimized(shape, Shapes.create(rotate(part, facing)), BooleanOp.OR);
        }
        return shape.optimize();
    }

    /** Which part was clicked: 1-3 = button, 4 = lock/valve, 0 = the rest. */
    public int partAt(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
        AABB[] parts = this.parts();
        for (int i = 0; i < parts.length; i++) {
            if (rotate(parts[i], state.getValue(FACING)).inflate(0.01).contains(local)) {
                return i + 1;
            }
        }
        return NONE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FrequencyBlockEntity storage)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int part = this.partAt(state, pos, hit.getLocation());
        Frequency frequency = storage.getFrequency();

        if (part >= 1 && part <= 3) {
            DyeColor dye = RecolourRecipe.dyeColour(stack);
            if (dye != null && frequency.colour(part - 1) != dye) {
                if (!frequency.mayUse(player)) {
                    return this.denied(player, frequency);
                }
                if (!level.isClientSide()) {
                    storage.setFrequency(frequency.withColour(part - 1, dye));
                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1F, 1F);
                }
                return InteractionResult.SUCCESS;
            }
        } else if (part == LOCK && !frequency.isLocked() && EnderStorageConfig.isPersonalItem(stack)) {
            if (!level.isClientSide()) {
                storage.lock(frequency.withOwner(player), stack);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_DIAMOND.value(), SoundSource.BLOCKS, 1F, 1F);
            }
            return InteractionResult.SUCCESS;
        } else if (part == LOCK && !frequency.isLocked() && EnderStorageConfig.isTeamItem(stack)) {
            if (!level.isClientSide()) {
                Optional<Teams.TeamInfo> team = Teams.teamOf(player);
                if (team.isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("enderstorage.no_team"));
                    return InteractionResult.FAIL;
                }
                storage.lock(frequency.withTeam(team.get()), stack);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1F, 1F);
            }
            return InteractionResult.SUCCESS;
        }
        if (storage.isUpgradeItem(stack)) {
            if (!frequency.mayUse(player)) {
                return this.denied(player, frequency);
            }
            if (!level.isClientSide()) {
                storage.upgrade(player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FrequencyBlockEntity storage)) {
            return InteractionResult.PASS;
        }
        int part = this.partAt(state, pos, hit.getLocation());
        Frequency frequency = storage.getFrequency();
        if (part == LOCK && player.isShiftKeyDown() && frequency.isLocked()) {
            // Take the personal or team item back
            if (!frequency.mayUse(player)) {
                return this.denied(player, frequency);
            }
            if (!level.isClientSide()) {
                // The item that locked it; the configured item if that is unknown
                ItemStack lockItem = storage.unlock(frequency.hasTeam() ? EnderStorageConfig.teamItem() : EnderStorageConfig.personalItem());
                if (!player.hasInfiniteMaterials()) {
                    player.getInventory().placeItemBackInInventory(lockItem);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!frequency.mayUse(player)) {
            return this.denied(player, frequency);
        }
        return storage.activate(player, part);
    }

    protected InteractionResult denied(Player player, Frequency frequency) {
        if (!player.level().isClientSide()) {
            player.sendOverlayMessage(frequency.deniedMessage());
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof FrequencyBlockEntity storage ? storage.comparatorOutput() : 0;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof FrequencyBlockEntity storage) {
            storage.getFrequency().applyTo(stack);
        }
        return stack;
    }

    /** Light from the block entity (the fluid in a tank); used by {@code BlockLightEngineMixin}. */
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FrequencyBlockEntity storage ? storage.lightLevel() : 0;
    }
}
