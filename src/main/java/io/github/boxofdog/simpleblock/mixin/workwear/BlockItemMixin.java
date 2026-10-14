package io.github.boxofdog.simpleblock.mixin.workwear;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.workwear.TrailMarkers;

/** M10 Trail Blazer: a torch or ladder a player places is recorded as player-placed ({@link TrailMarkers}). */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void simpleBlock$recordPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && context.getPlayer() != null && context.getLevel() instanceof ServerLevel level) {
			TrailMarkers.placed(level, context.getClickedPos(), level.getBlockState(context.getClickedPos()));
		}
	}
}
