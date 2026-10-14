package io.github.boxofdog.simpleblock.feature;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * INV-01: exactly 27 usable carrying slots, the 9 hotbar slots plus 18 storage slots.
 *
 * <p>The player inventory keeps vanilla's 36 entries (saves, packets and menus depend on that
 * size), but entries 9-17, the top storage row in every screen, are unusable:
 * <ul>
 *   <li>{@code mixin/inventory/SlotMixin}: in every menu their slots are inactive (not drawn, not
 *       hoverable) and refuse placing and taking;</li>
 *   <li>{@code mixin/inventory/InventoryMixin}: pickups and other inventory insertions never choose
 *       them;</li>
 *   <li>{@link #evict}: anything that still lands there (a command, a creative-mode edit) is moved
 *       to a free usable slot on the next tick, or dropped at the player's feet if there is none.
 *       Nothing is stored where the player cannot reach it, and nothing is deleted.</li>
 * </ul>
 * The client paints over the row and uses the space for the Sort button and the heavy-load
 * indicator.
 *
 * <p>M10: a player wearing the Overalls ({@code workwear/Workwear#backpack}) has the row as a backpack:
 * 36 carrying slots. Taking them off makes it unusable again, and {@link #evict} moves what is in it.
 */
public final class InventorySlots {
	public static final int FIRST_DISABLED = 9;
	public static final int LAST_DISABLED = 17;
	/** INV-02 sort order: the upper storage row left to right, then the bottom storage row. */
	public static final int[] SORT_ORDER = {18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};

	private InventorySlots() {
	}

	public static boolean isDisabled(int inventorySlot) {
		return inventorySlot >= FIRST_DISABLED && inventorySlot <= LAST_DISABLED;
	}

	/** Whether the slot shows the row, whoever's inventory it is (for drawing). */
	public static boolean isRow(Slot slot) {
		return slot.container instanceof Inventory && isDisabled(slot.getContainerSlot());
	}

	/** Whether {@code inventorySlot} is unusable for {@code player}: the row, unless they wear the Overalls. */
	public static boolean isDisabled(@Nullable Player player, int inventorySlot) {
		return isDisabled(inventorySlot) && !Workwear.backpack(player);
	}

	public static boolean isDisabled(Slot slot) {
		return slot.container instanceof Inventory inventory && isDisabled(inventory.player, slot.getContainerSlot());
	}

	/** The sort order for this player: with the backpack row, it comes first (the top row). */
	public static int[] sortOrder(@Nullable Player player) {
		if (!Workwear.backpack(player)) {
			return SORT_ORDER;
		}
		int[] order = new int[27];
		for (int i = 0; i < 27; i++) {
			order[i] = FIRST_DISABLED + i;
		}
		return order;
	}

	/** Moves anything in the disabled row to usable slots, or drops it. Returns how many stacks moved. */
	public static int evict(ServerPlayer player) {
		if (Workwear.backpack(player)) {
			return 0;
		}
		Inventory inventory = player.getInventory();
		int moved = 0;
		for (int slot = FIRST_DISABLED; slot <= LAST_DISABLED; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			inventory.setItem(slot, ItemStack.EMPTY);
			// Already carried, so moving it within the inventory is not new heavy intake.
			HeavyLoad.exempt(() -> inventory.placeItemBackInInventory(stack, Prediction.SERVER_ONLY));
			moved++;
		}
		if (moved > 0) {
			SimpleBlock.LOGGER.debug("Moved {} stacks out of {}'s disabled inventory row", moved, player.getName().getString());
		}
		return moved;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				evict(player);
			}
		});
	}
}
