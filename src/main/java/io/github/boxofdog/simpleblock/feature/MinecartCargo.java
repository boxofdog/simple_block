package io.github.boxofdog.simpleblock.feature;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * FRT-01..08: the ordinary minecart is a cargo cart. It carries up to {@link #CAPACITY} items of one
 * kind of mined resource, and nobody sits in it (players or mobs) unless a cushion is fitted
 * ({@link MinecartCushions}), which makes it a passenger cart instead.
 *
 * <ul>
 *   <li>What can be loaded: plain (component-free) stacks of the item tag
 *       {@code #simple_block:minecart_cargo}: ores, raw metals, coal, gems and stone.</li>
 *   <li>Use with cargo in hand loads as much of the stack as fits; use with an empty hand shows
 *       the cargo. Sneak + use unloads up to one stack through {@code Inventory.add}, which takes
 *       only what fits in the inventory and under the heavy-load cap ({@link HeavyLoad}); the rest
 *       stays in the cart.</li>
 *   <li>Destroying a cart (broken, killed, burnt, blown up, or hit by a creative player) drops the
 *       cargo as ordinary item stacks; the minecart item it leaves is the plain, empty one.</li>
 *   <li>The cargo is one item and a count in synced entity data, saved with the cart. Every change
 *       happens on the server within one tick, source first, so nothing is created or lost.</li>
 * </ul>
 *
 * <p>Hooks: {@code mixin/minecart/MinecartMixin} (cargo state, use, no riding, saving, spilling);
 * client {@code CargoMinecartRenderer} (label while looked at). Furnace, hopper, TNT and other
 * minecart types are unchanged; chest minecarts are disabled by {@link ChestMinecarts}.
 */
public final class MinecartCargo {
	/** Owner-set starting capacity (M03): items of one kind per cart. */
	public static final int CAPACITY = 256;

	public static final TagKey<Item> CARGO = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "minecart_cargo"));

	/** A minecart's cargo state; implemented on the vanilla {@link Minecart} by {@code MinecartMixin}. */
	public interface Cart {
		/** The kind of item loaded, {@link Items#AIR} when empty. */
		Item simpleBlock$cargo();

		int simpleBlock$count();

		/** Sets the cargo; a count of 0 (or air) empties the cart. */
		void simpleBlock$setCargo(Item item, int count);

		/** The colour of the fitted cushion ({@link MinecartCushions}), or null for a cargo cart. */
		@Nullable DyeColor simpleBlock$cushion();

		void simpleBlock$setCushion(@Nullable DyeColor color);
	}

	private MinecartCargo() {
	}

	public static Cart cart(Minecart minecart) {
		return (Cart) minecart;
	}

	/** Whether this stack may be loaded: a cargo item with no custom data (name, enchantments, ...). */
	public static boolean isCargo(ItemStack stack) {
		return !stack.isEmpty() && stack.is(CARGO) && stack.getComponentsPatch().isEmpty();
	}

	/** "Raw Iron 180 / 256", or "Empty 0 / 256". */
	public static Component status(Cart cart) {
		int count = cart.simpleBlock$count();
		Component what = count > 0 ? new ItemStack(cart.simpleBlock$cargo()).getItemName() : message("empty", "Empty");
		return message("status", "%s %s / %s", what, count, CAPACITY);
	}

	private static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.minecart." + key, fallback, args);
	}

	/**
	 * Replaces riding. Sneak + use unloads one stack, whatever is in hand. Otherwise the main hand
	 * loads what it holds, or with an empty hand just shows the cargo; a main hand holding something
	 * that isn't cargo gets an explanation, unless the off hand holds cargo, which then loads.
	 * (Unloading with an empty hand, as first specified, would put the stack in that same hand, and
	 * the next click would load it straight back.) Client and server choose the same hand; only the
	 * server moves items.
	 */
	public static InteractionResult interact(Minecart minecart, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		boolean unloading = player.isSecondaryUseActive();
		if (hand == InteractionHand.OFF_HAND) {
			// Only reached when the main hand passed: it held something that is not cargo.
			if (unloading || !isCargo(held)) {
				return InteractionResult.PASS;
			}
		} else if (!unloading && !held.isEmpty() && !isCargo(held) && isCargo(player.getOffhandItem())) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			Cart cart = cart(minecart);
			serverPlayer.sendOverlayMessage(unloading ? unload(cart, serverPlayer) : held.isEmpty() ? status(cart) : load(cart, held));
		}
		return InteractionResult.SUCCESS;
	}

	/** Loads as much of {@code held} as fits and returns the message to show. */
	static Component load(Cart cart, ItemStack held) {
		Item loaded = cart.simpleBlock$cargo();
		int count = cart.simpleBlock$count();
		if (!isCargo(held)) {
			return message("not_cargo", "%s isn't cargo: ores, raw metals, coal, gems and stone only", held.getHoverName());
		}
		if (count > 0 && !held.is(loaded)) {
			return message("other_cargo", "This cart carries %s: empty it before loading %s", new ItemStack(loaded).getItemName(), held.getItemName());
		}
		int moved = Math.min(CAPACITY - count, held.getCount());
		if (moved <= 0) {
			return message("full", "Full: %s", status(cart));
		}
		Item item = held.getItem();
		held.shrink(moved);
		cart.simpleBlock$setCargo(item, count + moved);
		return status(cart);
	}

	/** Moves up to one stack into the player's inventory, as much as fits, and returns the message to show. */
	static Component unload(Cart cart, ServerPlayer player) {
		Item item = cart.simpleBlock$cargo();
		int count = cart.simpleBlock$count();
		if (count <= 0) {
			return status(cart);
		}
		ItemStack stack = new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize()));
		int offered = stack.getCount();
		player.getInventory().add(stack);
		int moved = offered - stack.getCount();
		if (moved <= 0) {
			return HeavyLoad.allowedCount(player, new ItemStack(item)) <= 0
				? message("too_heavy", "Too heavy: you can't carry any more %s", new ItemStack(item).getItemName())
				: message("no_room", "No room in your inventory");
		}
		cart.simpleBlock$setCargo(item, count - moved);
		return status(cart);
	}

	/** Drops the cargo as ordinary stacks and empties the cart; does nothing the second time. */
	public static void dropCargo(Minecart minecart, ServerLevel level) {
		Cart cart = cart(minecart);
		Item item = cart.simpleBlock$cargo();
		int left = cart.simpleBlock$count();
		cart.simpleBlock$setCargo(Items.AIR, 0);
		while (left > 0) {
			int stack = Math.min(left, item.getDefaultMaxStackSize());
			minecart.spawnAtLocation(level, new ItemStack(item, stack));
			left -= stack;
		}
	}

	public static void save(Cart cart, ValueOutput output) {
		if (cart.simpleBlock$count() > 0) {
			output.store("simple_block:cargo", Item.CODEC, cart.simpleBlock$cargo().builtInRegistryHolder());
			output.putInt("simple_block:cargo_count", cart.simpleBlock$count());
		}
	}

	public static void read(Cart cart, ValueInput input) {
		int count = input.getIntOr("simple_block:cargo_count", 0);
		Item item = input.read("simple_block:cargo", Item.CODEC).map(holder -> holder.value()).orElse(Items.AIR);
		cart.simpleBlock$setCargo(item, count);
	}
}
