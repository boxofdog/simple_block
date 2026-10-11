package io.github.boxofdog.simpleblock.mixin.shulker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.ShulkerBoxes;

/**
 * SF-04: an existing shulker box block never opens its inventory. Returns the same result vanilla
 * does when a box is blocked from opening, so client and server agree.
 */
@Mixin(ShulkerBoxBlock.class)
abstract class ShulkerBoxBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void simple_block$neverOpen(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir
	) {
		ShulkerBoxes.notifyDisabled(player);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
