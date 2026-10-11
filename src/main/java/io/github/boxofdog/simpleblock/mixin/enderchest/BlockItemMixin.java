package io.github.boxofdog.simpleblock.mixin.enderchest;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.EnderChests;

/** FRT-10: ender chests cannot be placed ({@code BlockItem.place} is the path for players and anything else). */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@Shadow
	public abstract Block getBlock();

	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void simple_block$refuseEnderChests(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (this.getBlock() instanceof EnderChestBlock) {
			EnderChests.notifyDisabled(context.getPlayer());
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
