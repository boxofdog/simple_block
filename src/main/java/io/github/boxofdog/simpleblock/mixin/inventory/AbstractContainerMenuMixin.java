package io.github.boxofdog.simpleblock.mixin.inventory;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.HeavyTransfers;

/**
 * Container-menu side of INV-03 (shift-clicks fill empty hotbar slots from the left) and INV-04
 * (commodities move only as far as the carrying allowance reaches; since M08 equipment always moves,
 * adding its weight). Runs on both sides, so the
 * client's prediction matches what the server does.
 */
@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
	@Shadow
	@Final
	public NonNullList<Slot> slots;

	@Shadow
	public abstract ItemStack getCarried();

	/** Remembers which slot a shift-click moves from, for the transfer limit below. */
	@WrapOperation(method = "doClick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack simpleBlock$trackQuickMoveSource(AbstractContainerMenu menu, Player player, int index, Operation<ItemStack> original) {
		Slot source = index >= 0 && index < this.slots.size() ? this.slots.get(index) : null;
		return HeavyTransfers.withQuickMoveSource(source, () -> original.call(menu, player, index));
	}

	/**
	 * INV-04 for shift-click transfers into the player's inventory: only as many commodities as fit
	 * move; the rest stays in the source slot. Crafting and other results move whole or not at all.
	 * Moving items the player already carries (inventory, offhand, own crafting grid) is free.
	 */
	@WrapMethod(method = "moveItemStackTo")
	private boolean simpleBlock$moveOnlyWhatFits(ItemStack stack, int start, int end, boolean backwards, Operation<Boolean> original) {
		Player player = HeavyTransfers.inventoryOwner(this.slots, start, end);
		if (player == null || HeavyLoad.bypasses(player) || !HeavyLoad.isRestricted(stack)) {
			return original.call(stack, start, end, backwards);
		}
		Slot source = HeavyTransfers.quickMoveSource();
		boolean carried = source != null ? HeavyTransfers.isCarriedSlot(player, source) : HeavyTransfers.isCarriedStack(player, stack);
		if (carried) {
			return HeavyLoad.exempt(() -> original.call(stack, start, end, backwards));
		}
		if (source != null && HeavyTransfers.isResultSlot(source)) {
			return HeavyLoad.canTakeIn(player, HeavyTransfers.intakeFrom(player, source, stack)) && original.call(stack, start, end, backwards);
		}
		int allowed = HeavyLoad.allowedCount(player, stack);
		if (allowed >= stack.getCount()) {
			return original.call(stack, start, end, backwards);
		}
		if (allowed <= 0) {
			return false;
		}
		int held = stack.getCount() - allowed;
		stack.shrink(held);
		try {
			return original.call(stack, start, end, backwards);
		} finally {
			stack.grow(held);
		}
	}

	/**
	 * INV-03: when a backwards transfer (container -> player, the usual shift-click direction)
	 * looks for an empty slot, it reaches the hotbar first, right to left. Visit the hotbar left to
	 * right instead. Only the empty-slot search is changed; topping up existing stacks keeps the
	 * vanilla order, and container-side order is untouched.
	 */
	@WrapOperation(method = "moveItemStackTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;", ordinal = 1))
	private Object simpleBlock$hotbarLeftFirst(NonNullList<Slot> list, int index, Operation<Object> original,
		@Local(argsOnly = true, ordinal = 0) int start, @Local(argsOnly = true, ordinal = 1) int end, @Local(argsOnly = true) boolean backwards) {
		if (!backwards) {
			return original.call(list, index);
		}
		Slot slot = list.get(index);
		if (!(slot.container instanceof Inventory) || !Inventory.isHotbarSlot(slot.getContainerSlot())) {
			return slot;
		}
		int first = index - slot.getContainerSlot();
		if (first < start || first + 8 >= end) {
			return slot;
		}
		for (int k = 0; k < 9; k++) {
			Slot hotbar = list.get(first + k);
			if (hotbar.container != slot.container || hotbar.getContainerSlot() != k) {
				return slot;
			}
		}
		return list.get(first + 8 - slot.getContainerSlot());
	}

	/** INV-04: picking a stack up onto the cursor takes only the commodities that fit. */
	@WrapOperation(method = "doClick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/inventory/Slot;tryRemove(IILnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"))
	private Optional<ItemStack> simpleBlock$pickUpOnlyWhatFits(Slot slot, int amount, int maxAmount, Player player,
		Operation<Optional<ItemStack>> original) {
		ItemStack inSlot = slot.getItem();
		if (HeavyLoad.bypasses(player) || !HeavyLoad.isRestricted(inSlot) || HeavyTransfers.isCarriedSlot(player, slot)) {
			return original.call(slot, amount, maxAmount, player);
		}
		if (HeavyTransfers.isResultSlot(slot)) {
			return HeavyLoad.canTakeIn(player, HeavyTransfers.intakeFrom(player, slot, inSlot))
				? original.call(slot, amount, maxAmount, player) : Optional.empty();
		}
		int wanted = Math.min(Math.min(amount, maxAmount), inSlot.getCount());
		int allowed = HeavyLoad.allowedCount(player, inSlot.copyWithCount(wanted));
		if (allowed >= wanted) {
			return original.call(slot, amount, maxAmount, player);
		}
		return allowed <= 0 ? Optional.empty() : original.call(slot, allowed, maxAmount, player);
	}

	/**
	 * INV-04 safety net for every other click (number-key swaps, double-click collecting, swapping
	 * the cursor with a slot, bundles, ...): if a click takes in commodities that do not fit under
	 * the allowance, the whole click is undone (see {@link HeavyTransfers#brokeCap}). Equipment never
	 * triggers it. Nothing is lost or duplicated, because the exact pre-click contents are restored.
	 */
	@WrapMethod(method = "clicked")
	private void simpleBlock$undoClicksOverTheCap(int slotIndex, int buttonNum, ContainerInput input, Player player, Operation<Void> original) {
		if (HeavyLoad.bypasses(player)) {
			original.call(slotIndex, buttonNum, input, player);
			return;
		}
		int before = HeavyLoad.carried(player);
		int commoditiesBefore = HeavyLoad.carriedCommodities(player);
		HeavyTransfers.Snapshot snapshot = new HeavyTransfers.Snapshot((AbstractContainerMenu) (Object) this, player);
		original.call(slotIndex, buttonNum, input, player);
		int after = HeavyLoad.carried(player);
		if (HeavyTransfers.brokeCap(before, commoditiesBefore, after, HeavyLoad.carriedCommodities(player))) {
			snapshot.restore();
			SimpleBlock.LOGGER.debug("Undid {} click on slot {}: heavy load would go from {} to {} (cap {})", input, slotIndex, before, after,
				HeavyLoad.CAPACITY);
		}
	}

	/** The cursor stack is already carried; putting it back in the inventory on close is free. */
	@WrapMethod(method = "removed")
	private void simpleBlock$returnCursorFreely(Player player, Operation<Void> original) {
		HeavyLoad.exempt(() -> original.call(player));
	}
}
