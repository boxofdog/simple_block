package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * INV-04 helpers for transfers inside container menus (see {@code mixin/inventory}).
 *
 * <p>A transfer only adds to the carried load when its source is not already carried: moving
 * stacks around the player's own inventory, offhand and 2x2 crafting grid is free, while taking
 * from a chest, a furnace, a crafting result or a worn armor slot is intake.
 */
public final class HeavyTransfers {
	/** The slot a shift-click (quick move) is moving from, while it runs. */
	private static final ThreadLocal<Slot> QUICK_MOVE_SOURCE = new ThreadLocal<>();

	private HeavyTransfers() {
	}

	public static Slot quickMoveSource() {
		return QUICK_MOVE_SOURCE.get();
	}

	public static <T> T withQuickMoveSource(Slot source, java.util.function.Supplier<T> action) {
		Slot previous = QUICK_MOVE_SOURCE.get();
		QUICK_MOVE_SOURCE.set(source);
		try {
			return action.get();
		} finally {
			QUICK_MOVE_SOURCE.set(previous);
		}
	}

	/** The player whose inventory slots lie in menu slots {@code [start, end)}, if any. */
	public static Player inventoryOwner(List<Slot> slots, int start, int end) {
		for (int i = Math.max(0, start); i < Math.min(end, slots.size()); i++) {
			if (slots.get(i).container instanceof Inventory inventory) {
				return inventory.player;
			}
		}
		return null;
	}

	/** Inventory entries 0-35 and the offhand (40); armor (36-39) is worn, so not carried. */
	public static boolean isCarriedInventorySlot(int containerSlot) {
		return containerSlot < 36 || containerSlot == 40;
	}

	public static boolean isCarriedSlot(Player player, Slot slot) {
		if (slot.container == player.getInventory()) {
			return isCarriedInventorySlot(slot.getContainerSlot());
		}
		return slot.container == player.inventoryMenu.getCraftSlots();
	}

	/** Whether this very stack object sits in something the player carries. */
	public static boolean isCarriedStack(Player player, ItemStack stack) {
		Inventory inventory = player.getInventory();
		for (ItemStack carried : inventory.getNonEquipmentItems()) {
			if (carried == stack) {
				return true;
			}
		}
		if (player.getOffhandItem() == stack || player.containerMenu.getCarried() == stack) {
			return true;
		}
		CraftingContainer grid = player.inventoryMenu.getCraftSlots();
		for (int i = 0; i < grid.getContainerSize(); i++) {
			if (grid.getItem(i) == stack) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Output slots whose items exist only as a whole (crafting, smithing, stonecutter, grindstone,
	 * anvil, loom, cartography and villager results). They are taken all or nothing.
	 */
	public static boolean isResultSlot(Slot slot) {
		return slot.container instanceof ResultContainer || slot instanceof ResultSlot
			|| slot.container instanceof MerchantContainer && slot.getContainerSlot() == 2;
	}

	/**
	 * Commodity weight that taking one crafting result removes from the carried load: the
	 * ingredients it consumes from the player's own 2x2 grid, less the remainders left behind.
	 */
	public static int consumedCarriedWeight(Player player, Slot resultSlot) {
		// Only the result slot of the player's own inventory menu crafts from the carried 2x2 grid.
		if (player.inventoryMenu.slots.get(InventoryMenu.RESULT_SLOT) != resultSlot) {
			return 0;
		}
		CraftingContainer grid = player.inventoryMenu.getCraftSlots();
		int consumed = 0;
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack ingredient = grid.getItem(i);
			if (!ingredient.isEmpty()) {
				consumed += HeavyLoad.restrictedUnitWeight(ingredient.copyWithCount(1));
				ItemStackTemplate remainder = ingredient.getItem().getCraftingRemainder();
				if (remainder != null) {
					consumed -= HeavyLoad.restrictedWeight(remainder.create());
				}
			}
		}
		return consumed;
	}

	/** Commodity weight a click would add by taking {@code stack} out of {@code slot}. */
	public static int intakeFrom(Player player, Slot slot, ItemStack stack) {
		return HeavyLoad.restrictedWeight(stack) - consumedCarriedWeight(player, slot);
	}

	/**
	 * Whether a click that moved the load from {@code before} to {@code after} (commodities from
	 * {@code commoditiesBefore} to {@code commoditiesAfter}) broke the cap: it took in commodities
	 * that did not fit. Equipment the same click took in does not count against them, so taking a
	 * rail never makes an ingot fail; equipment it gave up does make room.
	 */
	public static boolean brokeCap(int before, int commoditiesBefore, int after, int commoditiesAfter) {
		if (commoditiesAfter <= commoditiesBefore) {
			return false;
		}
		int equipmentAdded = (after - commoditiesAfter) - (before - commoditiesBefore);
		return after - Math.max(0, equipmentAdded) > HeavyLoad.CAPACITY;
	}

	/** Copies of everything a click can change, to put back if the click broke the cap. */
	public static final class Snapshot {
		private final AbstractContainerMenu menu;
		private final Inventory inventory;
		private final List<ItemStack> slotItems = new ArrayList<>();
		private final List<ItemStack> inventoryItems = new ArrayList<>();
		private final ItemStack carried;

		public Snapshot(AbstractContainerMenu menu, Player player) {
			this.menu = menu;
			this.inventory = player.getInventory();
			for (Slot slot : menu.slots) {
				this.slotItems.add(slot.getItem().copy());
			}
			for (int i = 0; i < this.inventory.getContainerSize(); i++) {
				this.inventoryItems.add(this.inventory.getItem(i).copy());
			}
			this.carried = menu.getCarried().copy();
		}

		public void restore() {
			for (int i = 0; i < this.slotItems.size() && i < this.menu.slots.size(); i++) {
				Slot slot = this.menu.slots.get(i);
				Container container = slot.container;
				if (!ItemStack.matches(slot.getItem(), this.slotItems.get(i))) {
					container.setItem(slot.getContainerSlot(), this.slotItems.get(i).copy());
				}
			}
			for (int i = 0; i < this.inventoryItems.size(); i++) {
				if (!ItemStack.matches(this.inventory.getItem(i), this.inventoryItems.get(i))) {
					this.inventory.setItem(i, this.inventoryItems.get(i).copy());
				}
			}
			this.menu.setCarried(this.carried.copy());
		}
	}
}
