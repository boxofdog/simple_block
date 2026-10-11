package io.github.boxofdog.simpleblock.mixin.geology;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;

import io.github.boxofdog.simpleblock.feature.RemovedRocks;

/**
 * GEO-01: every block that features and structures place during world generation goes through
 * {@code WorldGenRegion.setBlock}. A removed rock (tuff bricks in trial chambers, polished granite
 * in ocean ruins, diorite walls in villages, tuff around sulfur springs, ...) is swapped for a
 * stone- or deepslate-family block of the same shape, so the structure keeps its layout. This is a
 * map lookup per placed block, not a scan. Blocks placed by players are not affected.
 */
@Mixin(WorldGenRegion.class)
abstract class WorldGenRegionMixin {
	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
		at = @At("HEAD"), argsOnly = true)
	private BlockState simpleBlock$substituteRemovedRock(BlockState state, @Local(argsOnly = true) BlockPos pos) {
		if (!RemovedRocks.isRemoved(state)) {
			return state;
		}
		return RemovedRocks.substitute(state, ((WorldGenRegion) (Object) this).getBlockState(pos), pos);
	}
}
