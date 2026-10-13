package io.github.boxofdog.simpleblock.mixin.inventory;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.NonNullList;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.InventorySlots;

/**
 * INV-01: insertions never choose the disabled row. INV-04: insertions take only the commodities
 * that fit (equipment always fits the allowance, M08); what does not fit stays in the source stack (an item on the ground stays there) or, for
 * items being handed back with nowhere else to go, is dropped at the player's feet.
 */
@Mixin(Inventory.class)
abstract class InventoryMixin {
	@Shadow
	@Final
	private NonNullList<ItemStack> items;
	@Shadow
	@Final
	public Player player;

	@Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$skipDisabledRow(CallbackInfoReturnable<Integer> cir) {
		for (int i = 0; i < this.items.size(); i++) {
			if (!InventorySlots.isDisabled(i) && this.items.get(i).isEmpty()) {
				cir.setReturnValue(i);
				return;
			}
		}
		cir.setReturnValue(-1);
	}

	@WrapOperation(method = "getSlotWithRemainingSpace", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
	private Object simpleBlock$noStackingIntoDisabledRow(NonNullList<?> list, int index, Operation<Object> original) {
		return InventorySlots.isDisabled(index) ? ItemStack.EMPTY : original.call(list, index);
	}

	@WrapMethod(method = "add(ILnet/minecraft/world/item/ItemStack;)Z")
	private boolean simpleBlock$addOnlyWhatFits(int slot, ItemStack stack, Operation<Boolean> original) {
		int allowed = HeavyLoad.allowedCount(this.player, stack);
		if (allowed >= stack.getCount()) {
			return original.call(slot, stack);
		}
		if (allowed <= 0) {
			return false;
		}
		ItemStack part = stack.copyWithCount(allowed);
		original.call(slot, part);
		int taken = allowed - part.getCount();
		stack.shrink(taken);
		return taken > 0;
	}

	@WrapMethod(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)V")
	private void simpleBlock$placeBackOnlyWhatFits(ItemStack stack, boolean sendSlotUpdates, Prediction prediction, Operation<Void> original) {
		int allowed = HeavyLoad.allowedCount(this.player, stack);
		if (allowed < stack.getCount()) {
			this.player.drop(stack.split(stack.getCount() - allowed), false, prediction);
		}
		// The rest fits as a whole; don't let the partial adds inside limit it again.
		HeavyLoad.exempt(() -> original.call(stack, sendSlotUpdates, prediction));
	}
}
