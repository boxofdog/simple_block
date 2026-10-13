package io.github.boxofdog.simpleblock.feature;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;

/**
 * M06: a player cannot shove a minecart that cannot carry a passenger by walking into it (or by
 * being run into). That is every minecart except a cushioned one ({@link MinecartCushions}): freight
 * carts, empty or loaded, with or without a rod, Cobblestone Engines and the other vanilla carts.
 * Cushioned carts are pushed as in vanilla. Nothing else is touched: engines, cables, couplings,
 * gravity, other carts and mobs move carts as before. Hooks: {@code mixin/pushing}.
 */
public final class CartPushing {
	private CartPushing() {
	}

	/** Whether {@code pusher} bumping into {@code cart} may move it. */
	public static boolean moves(AbstractMinecart cart, Entity pusher) {
		return !(pusher instanceof Player) || cart instanceof Minecart minecart && MinecartCushions.isFitted(minecart);
	}
}
