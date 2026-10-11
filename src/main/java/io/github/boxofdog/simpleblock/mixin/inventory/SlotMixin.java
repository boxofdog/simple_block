package io.github.boxofdog.simpleblock.mixin.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.inventory.Slot;

import io.github.boxofdog.simpleblock.feature.InventorySlots;

/**
 * INV-01: in every menu, the slots showing player inventory entries 9-17 are inactive (not drawn
 * or hoverable) and accept no placing or taking, whether by click, drag, number key or shift-click.
 */
@Mixin(Slot.class)
abstract class SlotMixin {
	@Inject(method = {"mayPlace", "mayPickup", "isActive"}, at = @At("HEAD"), cancellable = true)
	private void simpleBlock$disabledRow(CallbackInfoReturnable<Boolean> cir) {
		if (InventorySlots.isDisabled((Slot) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
