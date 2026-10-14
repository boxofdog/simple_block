package io.github.boxofdog.simpleblock.gametest.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;

/** Client tests: the sounds the client was asked to play ({@code mixin/SoundManagerMixin}), by name. */
public final class PlayedSounds {
	public static final List<Identifier> PLAYED = new CopyOnWriteArrayList<>();

	private PlayedSounds() {
	}
}
