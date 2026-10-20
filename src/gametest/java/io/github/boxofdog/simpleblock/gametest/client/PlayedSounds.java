package io.github.boxofdog.simpleblock.gametest.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;

/**
 * Client tests: the sounds the client was asked to play ({@code mixin/SoundManagerMixin}), by name; and how
 * many lava particles (cinders) it was asked to show ({@code mixin/ClientParticleCountMixin}).
 */
public final class PlayedSounds {
	public static final List<Identifier> PLAYED = new CopyOnWriteArrayList<>();
	public static final java.util.concurrent.atomic.AtomicInteger LAVA_PARTICLES = new java.util.concurrent.atomic.AtomicInteger();

	private PlayedSounds() {
	}
}
