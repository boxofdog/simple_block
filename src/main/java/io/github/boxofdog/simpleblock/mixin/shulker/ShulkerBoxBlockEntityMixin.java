package io.github.boxofdog.simpleblock.mixin.shulker;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SF-04: hoppers, hopper minecarts and droppers see no slots on an existing shulker box, so a
 * command-placed box cannot be filled by automation and then mined as a portable inventory.
 */
@Mixin(ShulkerBoxBlockEntity.class)
abstract class ShulkerBoxBlockEntityMixin {
	@Unique
	private static final int[] NO_SLOTS = new int[0];

	@Inject(method = "getSlotsForFace", at = @At("HEAD"), cancellable = true)
	private void simple_block$noAutomationAccess(Direction direction, CallbackInfoReturnable<int[]> cir) {
		cir.setReturnValue(NO_SLOTS);
	}
}
