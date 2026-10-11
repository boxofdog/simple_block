package io.github.boxofdog.simpleblock.mixin.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;

/** INV-04: taking armor or an item off an armor stand puts it straight into the hand; respect the allowance. */
@Mixin(ArmorStand.class)
abstract class ArmorStandMixin {
	@Inject(method = "swapItem", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$refuseOverloadingTake(Player player, EquipmentSlot slot, ItemStack playerItemStack, InteractionHand hand,
		CallbackInfoReturnable<Boolean> cir) {
		ItemStack onStand = ((ArmorStand) (Object) this).getItemBySlot(slot);
		if (playerItemStack.getCount() <= 1 && !HeavyLoad.canTakeIn(player, HeavyLoad.weight(onStand) - HeavyLoad.weight(playerItemStack))) {
			cir.setReturnValue(false);
		}
	}
}
