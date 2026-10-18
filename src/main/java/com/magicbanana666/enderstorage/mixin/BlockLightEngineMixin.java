package com.magicbanana666.enderstorage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.magicbanana666.enderstorage.block.EnderStorageBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets an ender tank give light from the fluid inside it: the light of a block normally only depends on its block
 * state, this asks the block entity.
 */
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {

    @ModifyExpressionValue(method = "getEmission", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"))
    private int enderstorage$blockEntityLight(int emission, long packedPos, BlockState state) {
        if (emission == 0 && state.getBlock() instanceof EnderStorageBlock block) {
            BlockGetter level = ((LightEngineAccessor) this).enderstorage$chunkSource().getLevel();
            if (level != null) {
                return block.getLightEmission(state, level, BlockPos.of(packedPos));
            }
        }
        return emission;
    }
}
