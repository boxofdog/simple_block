package io.github.boxofdog.simpleblock.mixin.elytra;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SF-03: End ships no longer generate the item frame holding an elytra.
 *
 * <p>The ship template marks the frame with an "Elytra" data marker that is handled in code, so
 * there is no loot table or data file to override. No vanilla loot table, trade or recipe yields
 * elytra. The rest of the ship (chests, dragon head, shulker sentries) is unchanged.
 */
@Mixin(EndCityPieces.EndCityPiece.class)
abstract class EndCityPieceMixin {
	@Inject(method = "handleDataMarker", at = @At("HEAD"), cancellable = true)
	private void simple_block$skipElytraItemFrame(
			String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB, CallbackInfo ci
	) {
		if (markerId.startsWith("Elytra")) {
			ci.cancel();
		}
	}
}
