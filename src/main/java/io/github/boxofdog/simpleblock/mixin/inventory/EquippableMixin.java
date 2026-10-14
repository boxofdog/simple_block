package io.github.boxofdog.simpleblock.mixin.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * INV-04: right-click equipping swaps the worn piece (exempt) into the hand or inventory (carried).
 * Armor is equipment, so since M08 the swap is only refused if the worn item is somehow a
 * commodity that does not fit; nothing is dropped.
 */
@Mixin(Equippable.class)
abstract class EquippableMixin {
	@Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$refuseOverloadingSwap(ItemStack inHand, Player player, CallbackInfoReturnable<InteractionResult> cir) {
		EquipmentSlot slot = ((Equippable) (Object) this).slot();
		// M10: a slot the commodity-overload lock holds is not swapped; nothing goes on under the Overalls.
		if (Workwear.locked(player, slot) || slot == EquipmentSlot.LEGS && Workwear.legsReserved(player)) {
			cir.setReturnValue(InteractionResult.FAIL);
			return;
		}
		ItemStack worn = player.getItemBySlot(slot);
		if (!worn.isEmpty() && !HeavyLoad.canTakeIn(player, HeavyLoad.restrictedWeight(worn) - HeavyLoad.restrictedUnitWeight(inHand))) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
