package io.github.boxofdog.simpleblock.feature;

import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * M11 (owner, 2026-10-14): the mod shows no floating text: no action-bar messages and no labels over
 * things. What an action did shows in the world (a cart's fill, a gate, a lever, a sound). The
 * outcome each use used to announce is still worked out and noted here, never displayed, so tests can
 * check it and replacement feedback can be designed from it; every message taken off the screen is
 * listed in {@code docs/UX_TEXT_LOG.md}.
 */
public final class Feedback {
	private static final Map<Player, Component> LAST = new WeakHashMap<>();

	private Feedback() {
	}

	/** Notes the outcome of {@code player}'s action. Shows nothing. */
	public static void quiet(Player player, Component outcome) {
		LAST.put(player, outcome);
	}

	/** The outcome last noted for {@code player} (server side), or null. */
	public static @Nullable Component last(Player player) {
		return LAST.get(player);
	}
}
