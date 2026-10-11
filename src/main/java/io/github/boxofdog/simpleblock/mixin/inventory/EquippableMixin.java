package io.github.boxofdog.simpleblock.mixin.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;

/**
 * INV-04: right-click equipping swaps the worn piece (exempt) into the hand or inventory (carried).
 * Refuse the swap when that would push the player over the allowance, so worn armor never turns
 * into carried overload and nothing is dropped.
 */
@Mixin(Equippable.class)
abstract class EquippableMixin {
	@Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$refuseOverloadingSwap(ItemStack inHand, Player player, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack worn = player.getItemBySlot(((Equippable) (Object) this).slot());
		if (!worn.isEmpty() && !HeavyLoad.canTakeIn(player, HeavyLoad.weight(worn) - HeavyLoad.unitWeight(inHand))) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
