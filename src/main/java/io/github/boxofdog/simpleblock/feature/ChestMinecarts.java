package io.github.boxofdog.simpleblock.feature;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

/**
 * FRT-11: chest minecarts cannot be obtained or used as storage or transport; the cargo-carrying
 * minecart ({@link MinecartCargo}) replaces them.
 *
 * <ul>
 *   <li>Acquisition: the recipe and its recipe-book advancement are disabled by data overrides;
 *       kept out of the creative menu by {@link RetiredItems}. No vanilla loot table or trade contains one.</li>
 *   <li>Placement by hand or by dispenser is refused where both create the cart
 *       ({@code mixin/chestcart/AbstractMinecartMixin}).</li>
 *   <li>The chest minecarts mineshafts generate stay as sealed loot: they never open
 *       ({@code MinecartChestMixin}), hoppers, hopper minecarts and droppers do not see them
 *       ({@code HopperBlockEntityMixin}), and breaking one spills its loot and drops a minecart and
 *       a chest, never a chest-minecart item ({@code VehicleEntityMixin}).</li>
 * </ul>
 *
 * <p>The entity type and item stay registered (mineshafts and commands still use them). Other
 * minecart types and pack animals are untouched.
 */
public final class ChestMinecarts {
	private ChestMinecarts() {
	}

	/** Tells a player why nothing happened. Server-side only; a no-op for dispensers and clients. */
	public static void notifyDisabled(@Nullable Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			Feedback.quiet(serverPlayer, Component.translatableWithFallback("message.simple_block.chest_minecart_disabled",
					"Chest minecarts don't work in this world: load a minecart instead"));
		}
	}
}
