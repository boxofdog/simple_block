package io.github.boxofdog.simpleblock.feature;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import io.github.boxofdog.simpleblock.network.SaturationPayload;

/**
 * FOOD-01 HUD support: keeps the client's copy of its player's saturation exact.
 *
 * <p>Vanilla 26.3 sends saturation only inside its health update, and sends that update when health
 * or food level change or saturation reaches or leaves 0; in between, the client's value is stale.
 * After every server tick each player whose saturation differs from what was last sent gets a
 * {@link SaturationPayload} (saturation moves in steps, so this is a few packets a minute). Clients
 * without the mod never get it: the payload is only sent where the client registered it.
 */
public final class SaturationSync {
	private static final Map<ServerPlayer, Float> SENT = new WeakHashMap<>();

	private SaturationSync() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SaturationPayload.TYPE, SaturationPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				float saturation = player.getFoodData().getSaturationLevel();
				Float sent = SENT.get(player);
				if ((sent == null || sent != saturation) && ServerPlayNetworking.canSend(player, SaturationPayload.TYPE)) {
					ServerPlayNetworking.send(player, new SaturationPayload(saturation));
					SENT.put(player, saturation);
				}
			}
		});
	}
}
