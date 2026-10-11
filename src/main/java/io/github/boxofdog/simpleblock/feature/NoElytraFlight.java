package io.github.boxofdog.simpleblock.feature;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;

/**
 * SF-03: elytra (and anything else with the {@code minecraft:glider} component) cannot glide.
 *
 * <p>Fabric's {@link EntityElytraEvents#ALLOW} hooks {@code LivingEntity.canGlide()}, which the
 * server checks both when a player asks to start gliding and on every tick while gliding. Blocking
 * it there stops command- or creative-injected elytra on the logical server, regardless of what
 * the client predicts. The elytra item stays registered.
 *
 * <p>The End ship item frame, elytra's only natural source, is removed in
 * {@code mixin/elytra/EndCityPieceMixin}.
 */
public final class NoElytraFlight {
	private NoElytraFlight() {
	}

	public static void register() {
		EntityElytraEvents.ALLOW.register(entity -> false);
	}
}
