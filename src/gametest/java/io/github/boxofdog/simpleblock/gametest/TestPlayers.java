package io.github.boxofdog.simpleblock.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Real {@link ServerPlayer}s (fake network connection) for game tests. Unlike vanilla's
 * {@code makeMockServerPlayerInLevel}, these are not hard-wired to creative mode, and they are
 * registered with the player list so level logic such as sleep tracking sees them.
 */
public final class TestPlayers {
	private TestPlayers() {
	}

	public static ServerPlayer create(GameTestHelper helper, GameType gameType, BlockPos relativePos) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "sb-test-player"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(gameType);
		Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos));
		player.teleportTo(level, pos.x, pos.y, pos.z, Set.of(), 0.0F, 0.0F, true);
		return player;
	}

	public static void remove(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}

	/** Removes every player left in a level (e.g. by an earlier failed test) so sleep counts are exact. */
	public static void removeAll(ServerLevel level) {
		for (ServerPlayer player : List.copyOf(level.players())) {
			remove(player);
		}
	}
}
