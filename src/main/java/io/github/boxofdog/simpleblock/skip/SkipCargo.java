package io.github.boxofdog.simpleblock.skip;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * M07: the skip's cargo, one slot holding up to {@link #CAPACITY} of one commodity. What may go in is
 * exactly what a freight cart takes ({@link MinecartCargo#isCargo}: plain mined resources); once there
 * is some, only the same item. The 512 lives only here, as an item and a count: whatever leaves the
 * skip leaves as ordinary stacks of at most the item's own stack size, so no oversized stack ever
 * reaches a player's inventory, a cursor, a container or the ground.
 *
 * <p>Every operation takes from its source and gives to its destination in the same server tick, and
 * gives back anything the destination refused, so nothing is made or lost.
 */
public final class SkipCargo {
	/** Owner-settled capacity (M07 brief section 2). */
	public static final int CAPACITY = 512;

	/** The skip's slot. */
	public interface Store {
		/** The item loaded, {@link Items#AIR} when empty. */
		Item cargo();

		int count();

		/** Sets the cargo; a count of 0 (or air) empties it. */
		void setCargo(Item item, int count);
	}

	private SkipCargo() {
	}

	static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.skip." + key, fallback, args);
	}

	/** "Raw Iron 300 / 512", or "Empty 0 / 512". */
	public static Component status(Store store) {
		Component what = store.count() > 0 ? new ItemStack(store.cargo()).getItemName() : message("empty", "Empty");
		return message("status", "%s %s / %s", what, store.count(), CAPACITY);
	}

	/** How many of {@code stack} the skip could take now: 0 if it is not cargo, another commodity, or the skip is full. */
	public static int room(Store store, ItemStack stack) {
		if (!MinecartCargo.isCargo(stack) || store.count() > 0 && !stack.is(store.cargo())) {
			return 0;
		}
		return CAPACITY - store.count();
	}

	/** Moves up to {@code max} of {@code stack} into the skip (shrinking the stack); returns how many went in. */
	public static int deposit(Store store, ItemStack stack, int max) {
		int moved = Math.min(Math.min(max, stack.getCount()), room(store, stack));
		if (moved <= 0) {
			return 0;
		}
		Item item = stack.getItem();
		stack.shrink(moved);
		store.setCargo(item, store.count() + moved);
		return moved;
	}

	/** Takes up to {@code max} out as one ordinary stack (never more than the item's stack size); empty if there is none. */
	public static ItemStack withdraw(Store store, int max) {
		Item item = store.cargo();
		int count = Math.min(Math.min(max, store.count()), item.getDefaultMaxStackSize());
		if (count <= 0) {
			return ItemStack.EMPTY;
		}
		store.setCargo(item, store.count() - count);
		return new ItemStack(item, count);
	}

	/** Puts back what a destination refused (the same item it came out as, so it always fits). */
	public static void giveBack(Store store, ItemStack rest) {
		if (!rest.isEmpty()) {
			store.setCargo(rest.getItem(), store.count() + rest.getCount());
		}
	}

	/** At most this many of the skip's cargo may go to the player now: a stack, within their heavy-load cap. */
	public static int allowed(Player player, Store store, int wanted) {
		if (store.count() <= 0) {
			return 0;
		}
		int stack = Math.min(Math.min(wanted, store.count()), store.cargo().getDefaultMaxStackSize());
		return HeavyLoad.allowedCount(player, new ItemStack(store.cargo(), stack));
	}

	/** Loads the stack in hand into the skip (a right-click with cargo); returns the message to show. */
	public static Component loadFromHand(Store store, ItemStack held) {
		if (!MinecartCargo.isCargo(held)) {
			return message("not_cargo", "%s isn't cargo: ores, raw metals, coal, gems and stone only", held.getHoverName());
		}
		if (store.count() > 0 && !held.is(store.cargo())) {
			return message("other_cargo", "This skip carries %s: empty it before loading %s", new ItemStack(store.cargo()).getItemName(),
				held.getItemName());
		}
		if (deposit(store, held, held.getCount()) <= 0) {
			return message("full", "Full: %s", status(store));
		}
		return status(store);
	}

	/** Moves up to one stack into the player's inventory (a shift-click on the slot); returns the message to show. */
	public static Component unloadToInventory(Store store, ServerPlayer player) {
		int allowed = allowed(player, store, Integer.MAX_VALUE);
		if (store.count() <= 0) {
			return status(store);
		}
		if (allowed <= 0) {
			return message("too_heavy", "Too heavy: you can't carry any more %s", new ItemStack(store.cargo()).getItemName());
		}
		ItemStack stack = withdraw(store, allowed);
		player.getInventory().add(stack);
		giveBack(store, stack);
		return status(store);
	}
}
