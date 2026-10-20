package io.github.boxofdog.simpleblock.feature;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * FRT-01..08: the ordinary minecart is a cargo cart. It carries up to {@link #CAPACITY} items of one
 * kind of mined resource, and nobody sits in it (players or mobs) unless a cushion is fitted
 * ({@link MinecartCushions}), which makes it a passenger cart instead.
 *
 * <ul>
 *   <li>What can be loaded: plain (component-free) stacks of the item tag
 *       {@code #simple_block:minecart_cargo}: ores, raw metals, coal, gems, stone, and (M11) ingots,
 *       storage blocks, sand, gravel and dirt.</li>
 *   <li>M11: loading and unloading by hand is gradual. Each use with cargo in hand moves
 *       {@code minecart.hand_batch} items from the stack into the cart; sneak + use moves as many
 *       into the inventory through {@code Inventory.add}, which takes only what fits there and under
 *       the heavy-load cap ({@link HeavyLoad}). Holding use repeats it (vanilla repeats a held use
 *       every 4 ticks), and the server allows one batch per {@code minecart.hand_interval_ticks} for
 *       each player, so filling a cart takes time and its fill shows each step. Letting go stops it;
 *       nothing is taken in advance.</li>
 *   <li>M11: the cart says nothing: no label while looked at, no message when used. What it carries
 *       shows in it (client {@code CargoMinecartRenderer}).</li>
 *   <li>Destroying a cart (broken, killed, burnt, blown up, or hit by a creative player) drops the
 *       cargo as ordinary item stacks; the minecart item it leaves is the plain, empty one.</li>
 *   <li>The cargo is one item and a count in synced entity data, saved with the cart. Every change
 *       happens on the server within one tick, source first, so nothing is created or lost.</li>
 * </ul>
 *
 * <p>Hooks: {@code mixin/minecart/MinecartMixin} (cargo state, use, no riding, saving, spilling).
 * Furnace, hopper, TNT and other minecart types are unchanged; chest minecarts are disabled by
 * {@link ChestMinecarts}. M11 hoppers: {@code train/FreightHoppers}.
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

	/** The game time of each player's last hand batch (server). */
	private static final Map<Player, Long> LAST_BATCH = new WeakHashMap<>();

	private MinecartCargo() {
	}

	public static Cart cart(Minecart minecart) {
		return (Cart) minecart;
	}

	/** Whether this stack may be loaded: a cargo item with no custom data (name, enchantments, ...). */
	public static boolean isCargo(ItemStack stack) {
		return !stack.isEmpty() && stack.is(CARGO) && stack.getComponentsPatch().isEmpty();
	}

	/** How many of {@code stack}'s item the cart could take now: none if it holds something else. */
	public static int room(Cart cart, ItemStack stack) {
		if (!isCargo(stack) || cart.simpleBlock$count() > 0 && !stack.is(cart.simpleBlock$cargo())) {
			return 0;
		}
		return CAPACITY - cart.simpleBlock$count();
	}

	/**
	 * Replaces riding. Sneak + use unloads, whatever is in hand. Otherwise the main hand loads what it
	 * holds; an empty main hand opens the read-only cargo window (M13, BUG-014:
	 * {@link io.github.boxofdog.simpleblock.train.CargoInspectMenu}); a main hand holding something that
	 * isn't cargo passes to the off hand if that holds cargo, which then loads. Client and server choose
	 * the same hand; only the server moves items, a batch at a time.
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
		if (hand == InteractionHand.MAIN_HAND && !unloading && held.isEmpty()) {
			// M13 (BUG-014): an empty main hand inspects the cargo (read-only). Not while the use key is still held
			// from loading a stack that has just run out: a batch within HOLD_TICKS (and every repeat after it
			// while held) is the same hold, and opening the window would stop the player loading the next stack.
			if (player instanceof ServerPlayer serverPlayer) {
				long now = serverPlayer.level().getGameTime();
				Long last = LAST_BATCH.get(serverPlayer);
				if (last != null && now >= last && now - last <= HOLD_TICKS) {
					LAST_BATCH.put(serverPlayer, now);
					return InteractionResult.SUCCESS;
				}
				serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> io.github.boxofdog.simpleblock.train.CargoInspectMenu
					.server(id, minecart), net.minecraft.network.chat.Component.translatableWithFallback("container.simple_block.freight_cart", "Freight Cart")));
			}
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer && (unloading || isCargo(held)) && batchDue(serverPlayer)) {
			Cart cart = cart(minecart);
			int moved = unloading ? unload(cart, serverPlayer) : load(cart, held);
			if (moved > 0) {
				minecart.playSound(SoundEvents.GRAVEL_HIT, 0.5F, unloading ? 1.2F : 0.9F);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Ticks after a batch (or a held repeat) in which an empty-hand use continues the hold rather than inspecting. */
	public static final int HOLD_TICKS = 10;

	/** Whether this player may move another batch now (and notes that they do). */
	private static boolean batchDue(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long last = LAST_BATCH.get(player);
		if (last != null && now - last < Tuning.getInt("minecart.hand_interval_ticks") && now >= last) {
			return false;
		}
		LAST_BATCH.put(player, now);
		return true;
	}

	/** For tests: as if this player's last batch were long ago (holding use for as long as a test needs, within one tick). */
	public static void forgetPace(Player player) {
		LAST_BATCH.remove(player);
	}

	/** Loads one batch from {@code held}, as much as fits; returns how many moved. */
	static int load(Cart cart, ItemStack held) {
		int moved = Math.min(Math.min(room(cart, held), held.getCount()), Tuning.getInt("minecart.hand_batch"));
		if (moved <= 0) {
			return 0;
		}
		Item item = held.getItem();
		held.shrink(moved);
		cart.simpleBlock$setCargo(item, cart.simpleBlock$count() + moved);
		return moved;
	}

	/** Moves one batch into the player's inventory, as much as fits; returns how many moved. */
	static int unload(Cart cart, ServerPlayer player) {
		Item item = cart.simpleBlock$cargo();
		int count = cart.simpleBlock$count();
		if (count <= 0) {
			return 0;
		}
		ItemStack stack = new ItemStack(item, Math.min(count, Tuning.getInt("minecart.hand_batch")));
		int offered = stack.getCount();
		player.getInventory().add(stack);
		int moved = offered - stack.getCount();
		if (moved > 0) {
			cart.simpleBlock$setCargo(item, count - moved);
		}
		return moved;
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
