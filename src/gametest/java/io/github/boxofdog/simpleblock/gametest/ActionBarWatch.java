package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

/** Test only: the action-bar text packets sent to each player (M11: the mod sends none). */
public final class ActionBarWatch {
	private static final Map<ServerPlayer, List<Packet<?>>> SENT = new WeakHashMap<>();

	private ActionBarWatch() {
	}

	public static synchronized void note(ServerPlayer player, Packet<?> packet) {
		SENT.computeIfAbsent(player, p -> new ArrayList<>()).add(packet);
	}

	/** The action-bar packets sent to {@code player} so far. */
	public static synchronized List<Packet<?>> sent(ServerPlayer player) {
		return List.copyOf(SENT.getOrDefault(player, List.of()));
	}
}
