package io.github.boxofdog.simpleblock.mixin.enderchest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.EnderChests;

/** FRT-10: an existing ender chest never opens the player's ender inventory. */
@Mixin(EnderChestBlock.class)
abstract class EnderChestBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void simple_block$neverOpen(
			BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir
	) {
		EnderChests.notifyDisabled(player);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
