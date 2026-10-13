package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;

import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M06 over a real network connection: a dedicated server started by the test, the real client
 * connected to it as a remote player. The player places a Loading Track and a minecart with key
 * presses; the cart, sent rolling, docks, and the client sees its front (gate) the track's way; the
 * player walks into the docked cart without moving it, then uses the track's post to release it.
 * Screenshots: {@code build/run/clientGameTest/screenshots/m06-server-*}.
 */
public class DedicatedServerClientTest implements FabricClientGameTest {
	private static final int Z = 0;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 35, Z));
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill 18 " + (g - 1) + " -4 52 " + (g - 1) + " 6 minecraft:stone");
			server.runCommand("fill 18 " + g + " -4 52 " + (g + 6) + " 6 minecraft:air");
			server.runCommand("fill 22 " + g + " " + Z + " 50 " + g + " " + Z + " minecraft:rail[shape=east_west]");
			server.runCommand("setblock 40 " + g + " " + Z + " minecraft:air");
			server.runCommand("kill @e[type=!minecraft:player]");
			server.runCommand("tp @a 37.5 " + g + " " + (Z + 0.5) + " -90 40");
			connection.waitForChunksRender();

			// The loading track, by key press, facing east.
			server.runCommand("item replace entity @a weapon.mainhand with simple_block:loading_track");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(40.5, g, Z + 0.5));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			String track = server.computeOnServer(s -> s.overworld().getBlockState(new BlockPos(40, g, Z)).toString());
			assertTrue(track.contains("simple_block:loading_track") && track.contains("facing=east"), "placed over the network: " + track);

			// A minecart, by key press, sent rolling east.
			server.runCommand("tp @a 30.5 " + g + " " + (Z + 2.5) + " 180 30");
			server.runCommand("item replace entity @a weapon.mainhand with minecraft:minecart");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(30.5, g + 0.1, Z + 0.5));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			server.runCommand("clear @a");
			server.runCommand("execute as @e[type=minecraft:minecart] run data merge entity @s {Motion:[0.4d,0.0d,0.0d]}");
			int waited = 0;
			while (waited < 400 && !server.computeOnServer(s -> cart(s.overworld(), g) != null && Couplings.coupled(cart(s.overworld(), g)).simpleBlock$dock() != null)) {
				context.waitTicks(10);
				waited += 10;
			}
			double docked = server.computeOnServer(s -> cart(s.overworld(), g).getX());
			context.waitTicks(10);
			Direction clientFront = context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, new AABB(18, g - 1, Z - 1, 52, g + 3, Z + 2))
				.stream().findFirst().map(c -> LoadingTracks.freight(c).simpleBlock$front()).orElse(null));
			server.runCommand("tp @a 42.5 " + g + " " + (Z + 2.5) + " 0 0");
			context.waitTicks(5);
			lookAtPoint(context, new Vec3(40.9, g + 0.3, Z + 0.5));
			context.waitTicks(10);
			context.takeScreenshot("m06-server-docked");

			// The remote player walks into the docked cart from the west.
			server.runCommand("tp @a 37.5 " + g + " " + (Z + 0.5) + " -90 0");
			context.waitTicks(5);
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(30);
			context.getInput().releaseKey(options -> options.keyUp);
			context.waitTicks(10);
			double afterWalk = server.computeOnServer(s -> cart(s.overworld(), g).getX());

			// The post (north of an east-facing track) releases it.
			server.runCommand("tp @a 40.5 " + g + " " + (Z - 2.0) + " 0 30");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(40.5, g + 0.9, Z + 0.1));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			String message = overlayMessage(context);
			boolean free = server.computeOnServer(s -> Couplings.coupled(cart(s.overworld(), g)).simpleBlock$dock() == null);
			context.takeScreenshot("m06-server-released");
			SimpleBlockTestLog.info("CLIENT DEDICATED SERVER: loading track placed over the network ({}); cart docked after {} ticks at x {}, the client "
				+ "sees its front {}; walked into it 30 ticks: x {}; post used: '{}', released {}", track, waited, String.format("%.2f", docked), clientFront,
				String.format("%.2f", afterWalk), message, free);
			assertTrue(Math.abs(docked - 40.5) < 1.0E-3 && clientFront == Direction.EAST, "docked at " + docked + ", client front " + clientFront);
			assertTrue(Math.abs(afterWalk - docked) < 1.0E-3, "walking into the docked cart moved it: " + docked + " -> " + afterWalk);
			assertTrue(free, "the post should release the cart over the network: '" + message + "'");
		}
	}

	private static Minecart cart(net.minecraft.server.level.ServerLevel level, int g) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(18, g - 1, Z - 1, 52, g + 3, Z + 2)).stream().findFirst().orElse(null);
	}
}
