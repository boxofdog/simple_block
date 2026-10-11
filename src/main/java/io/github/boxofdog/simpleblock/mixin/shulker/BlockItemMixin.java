package io.github.boxofdog.simpleblock.mixin.shulker;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.ShulkerBoxes;

/**
 * SF-04: shulker boxes cannot be placed. {@code BlockItem.place} is the common path for players
 * and for the vanilla shulker-box dispenser behaviour, so one hook covers both.
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@Shadow
	public abstract Block getBlock();

	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void simple_block$refuseShulkerBoxes(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (this.getBlock() instanceof ShulkerBoxBlock) {
			ShulkerBoxes.notifyDisabled(context.getPlayer());
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
