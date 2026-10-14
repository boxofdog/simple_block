package io.github.boxofdog.simpleblock.feature;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.CushionItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * A cushion fitted into a minecart makes it a passenger cart again. Vanilla's cushion is a seat
 * entity fixed to the block under it, so it can't ride along in a cart; instead the cart remembers
 * the fitted cushion's colour (synced, saved) and the client draws vanilla's cushion model inside
 * it ({@code CargoMinecartRenderer}).
 *
 * <ul>
 *   <li>Fitting: use a cushion on an <em>empty</em> minecart (no cargo). One cushion is used up.</li>
 *   <li>A cushioned cart behaves like the vanilla minecart: use it to get in, and a rolling cart
 *       picks up mobs it bumps into. It takes no cargo.</li>
 *   <li>Sneak + use takes the cushion back out (into the inventory, or at the player's feet if it
 *       is full), unless someone is sitting on it. The cart is a cargo cart again.</li>
 *   <li>Destroying a cushioned cart drops the cushion next to the minecart item.</li>
 * </ul>
 *
 * <p>The state is on {@link MinecartCargo.Cart}, implemented by {@code mixin/minecart/MinecartMixin}.
 */
public final class MinecartCushions {
	private MinecartCushions() {
	}

	public static @Nullable DyeColor fitted(Minecart minecart) {
		return MinecartCargo.cart(minecart).simpleBlock$cushion();
	}

	public static boolean isFitted(Minecart minecart) {
		return fitted(minecart) != null;
	}

	public static boolean isCushion(ItemStack stack) {
		return stack.getItem() instanceof CushionItem;
	}

	/** Whether this use is about the cushion: the cart has one, or the player is fitting one. */
	public static boolean handles(Minecart minecart, Player player, InteractionHand hand) {
		return isFitted(minecart) || isCushion(player.getItemInHand(hand)) && !player.isSecondaryUseActive();
	}

	private static Component message(String key, String fallback) {
		return Component.translatableWithFallback("simple_block.minecart.cushion." + key, fallback);
	}

	/**
	 * Fits or removes a cushion. Returns null when vanilla should handle the use: someone getting
	 * into a cushioned cart. Client and server decide the same way; only the server changes state.
	 */
	public static @Nullable InteractionResult interact(Minecart minecart, Player player, InteractionHand hand) {
		MinecartCargo.Cart cart = MinecartCargo.cart(minecart);
		DyeColor fitted = cart.simpleBlock$cushion();
		if (fitted != null && !player.isSecondaryUseActive()) {
			return null;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			Feedback.quiet(serverPlayer, fitted == null ? fit(minecart, cart, serverPlayer, player.getItemInHand(hand)) : remove(minecart, cart, serverPlayer, fitted));
		}
		return InteractionResult.SUCCESS;
	}

	private static Component fit(Minecart minecart, MinecartCargo.Cart cart, ServerPlayer player, ItemStack cushion) {
		if (cart.simpleBlock$count() > 0) {
			return message("cargo", "Unload the cart before fitting a cushion");
		}
		cart.simpleBlock$setCushion(cushion.getOrDefault(DataComponents.CUSHION_COLOR, DyeColor.WHITE));
		cushion.consume(1, player);
		minecart.playSound(SoundEvents.CUSHION_PLACE, 0.75F, 0.8F);
		return message("fitted", "Cushion fitted: this cart carries a rider now, not cargo");
	}

	private static Component remove(Minecart minecart, MinecartCargo.Cart cart, ServerPlayer player, DyeColor fitted) {
		if (minecart.isVehicle()) {
			return message("occupied", "Someone is sitting on the cushion");
		}
		cart.simpleBlock$setCushion(null);
		// Into the inventory, or at the player's feet if it is full.
		player.getInventory().placeItemBackInInventory(new ItemStack(Items.CUSHION.pick(fitted)), Prediction.SERVER_ONLY);
		minecart.playSound(SoundEvents.CUSHION_GET_UP, 0.75F, 1.0F);
		return message("removed", "Cushion taken out: this cart carries cargo again");
	}

	/** Drops the fitted cushion as an item and clears it; does nothing the second time. */
	public static void dropCushion(Minecart minecart, ServerLevel level) {
		MinecartCargo.Cart cart = MinecartCargo.cart(minecart);
		DyeColor fitted = cart.simpleBlock$cushion();
		if (fitted != null) {
			cart.simpleBlock$setCushion(null);
			minecart.spawnAtLocation(level, new ItemStack(Items.CUSHION.pick(fitted)));
		}
	}

	public static void save(MinecartCargo.Cart cart, ValueOutput output) {
		DyeColor fitted = cart.simpleBlock$cushion();
		if (fitted != null) {
			output.store("simple_block:cushion", DyeColor.CODEC, fitted);
		}
	}

	public static void read(MinecartCargo.Cart cart, ValueInput input) {
		cart.simpleBlock$setCushion(input.read("simple_block:cushion", DyeColor.CODEC).orElse(null));
	}
}
