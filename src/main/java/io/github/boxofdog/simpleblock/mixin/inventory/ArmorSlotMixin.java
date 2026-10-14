package io.github.boxofdog.simpleblock.mixin.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10, in every menu's armor slots (server-enforced; the client predicts the same): a slot the
 * commodity-overload lock holds ({@link Workwear#lockedSlots}) can neither be emptied nor filled, so
 * clicks, shift-clicks, number-key swaps and drags all leave it as it is; and nothing goes into the
 * legs slot while the Overalls take it.
 */
@Mixin(ArmorSlot.class)
abstract class ArmorSlotMixin {
	@Shadow
	@Final
	private LivingEntity owner;
	@Shadow
	@Final
	private EquipmentSlot slot;

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$lockedPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (this.owner instanceof Player wearer && Workwear.locked(wearer, this.slot)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$lockedOrReservedPlace(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
		if (this.owner instanceof Player wearer && Workwear.locked(wearer, this.slot)
			|| this.slot == EquipmentSlot.LEGS && Workwear.legsReserved(this.owner)) {
			cir.setReturnValue(false);
		}
	}
}
