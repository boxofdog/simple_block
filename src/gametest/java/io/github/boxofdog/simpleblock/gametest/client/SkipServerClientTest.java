package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtHook;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.client.skip.SkipScreen;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipMenu;
import io.github.boxofdog.simpleblock.skip.SkipStations;

/**
 * M07 over a real network connection (a dedicated server started by the test, the real client
 * connected as a remote player): the skip placed and loaded by key presses, its screen opened and its
 * cargo slot clicked with the mouse (the server's slot, the remote screen and the cursor agree), and a
 * skip hung by hand (owner's playtest patch: bridle and pulley as the remote client sees them), a cart at
 * its own level left alone, and a cart one level lower filled once the chute's lip is on its rim, through
 * the discharge gate the client sees open.
 * Screenshots: {@code m07-server-*}.
 */
public class SkipServerClientTest implements FabricClientGameTest {
	private static final int X = 30;
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
			server.runCommand("fill 24 " + (g - 1) + " -6 36 " + (g - 1) + " 6 minecraft:stone");
			server.runCommand("fill 24 " + g + " -6 36 " + (g + 8) + " 6 minecraft:air");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("setblock " + X + " " + g + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
			server.runCommand("kill @e[type=!minecraft:player]");
			// On the rail side (north), so the skip is placed facing it.
			server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z - 2.5) + " 0 30");
			connection.waitForChunksRender();

			// Placed by key, facing the player's way back (north, towards the Loading Rail).
			server.runCommand("item replace entity @a weapon.mainhand with simple_block:ore_skip");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(X + 0.5, g, Z + 0.4));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			String placed = server.computeOnServer(s -> skip(s.overworld(), g) + " facing " + skip(s.overworld(), g).facing());
			// Loaded by key: 64 raw iron.
			server.runCommand("item replace entity @a weapon.mainhand with minecraft:raw_iron 64");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.1));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			String loaded = overlayMessage(context);
			// The screen, and one click on the cargo slot.
			server.runCommand("clear @a");
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			int shown = context.computeOnClient(client -> client.gui.screen() instanceof SkipScreen screen ? screen.getMenu().count() : -1);
			double[] slot = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				int left = (client.getWindow().getGuiScaledWidth() - 176) / 2;
				int top = (client.getWindow().getGuiScaledHeight() - 133) / 2;
				return new double[] {(left + SkipMenu.CARGO_X + 8) * scale, (top + SkipMenu.CARGO_Y + 8) * scale};
			});
			context.getInput().setCursorPos(slot[0], slot[1]);
			context.waitTicks(2);
			context.getInput().pressMouse(1);
			context.waitTicks(10);
			context.takeScreenshot("m07-server-skip-screen");
			int afterRight = server.computeOnServer(s -> skip(s.overworld(), g).count());
			int[] client = context.computeOnClient(c -> new int[] {((SkipScreen) c.gui.screen()).getMenu().count(), c.player.containerMenu.getCarried().getCount()});
			context.getInput().pressMouse(0);
			context.waitTicks(10);
			int afterPut = server.computeOnServer(s -> skip(s.overworld(), g).count());
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitTicks(10);

			// Owner's playtest patch: hung by hand over the network (route, free hook winched down, picked
			// up and used on the skip), then a docked freight cart at its own level is left alone, and is
			// filled once the coal winch lifts the skip until its chute's lip is on the cart's rim.
			String hung = hang(context, server, g);
			boolean bridle = context.computeOnClient(c -> c.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).stream()
				.anyMatch(e -> e.bridled() && Math.abs(e.getBoundingBox().getYsize() - OreSkip.HEIGHT) < 1.0E-6));
			CableAnchorBlock.Return head = context.computeOnClient(c -> c.level.getBlockState(new BlockPos(X, g + HEAD, Z)).getValue(CableAnchorBlock.RETURN));
			server.computeOnServer(s -> {
				skip(s.overworld(), g).setCargo(Items.RAW_IRON, 64);
				return null;
			});
			server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (g + 0.1) + " " + (Z - 0.5));
			context.waitTicks(60);
			int sameLevel = server.computeOnServer(s -> cartCount(s.overworld(), g));
			lever(context, server, g, CableLines.Lever.PULL);
			boolean gate = false;
			for (int i = 0; i < 200 && !gate; i++) {
				context.waitTicks(1);
				gate = context.computeOnClient(c -> c.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).stream()
					.anyMatch(e -> e.gateAnim > 0.99F));
			}
			double stopped = server.computeOnServer(s -> skip(s.overworld(), g).getY() - g);
			server.runCommand("tp @a " + (X + 2.9) + " " + (g - 0.1) + " " + (Z - 0.2) + " 90 15");
			context.waitTicks(5);
			context.takeScreenshot("m07-server-skip-unloading");
			context.waitTicks(60);
			int cart = server.computeOnServer(s -> cartCount(s.overworld(), g));
			int left = server.computeOnServer(s -> skip(s.overworld(), g).count());
			SimpleBlockTestLog.info("CLIENT DEDICATED SERVER SKIP: placed by key over the network ({}); loaded by key: '{}'; remote screen shows {}; right-click on "
				+ "the slot: server {}, client screen {}, cursor {}; left-click put back: server {}; hung by hand: '{}', client sees the bridle (4 tall) {}, "
				+ "the head anchor {}; a cart docked at the skip's own level got {}; PULL stopped the skip at y {} (rail + 11/16), client saw the discharge "
				+ "gate open {}, cart {} raw iron, skip {}", placed, loaded, shown, afterRight, client[0], client[1], afterPut, hung, bridle, head, sameLevel,
				stopped, gate, cart, left);
			assertTrue(placed.contains("facing north") && loaded.contains("64 / 512") && shown == 64, "placed and loaded: " + placed + " '" + loaded + "' " + shown);
			assertTrue(afterRight == 32 && client[0] == 32 && client[1] == 32 && afterPut == 64, "slot clicks over the network: " + afterRight + "/"
				+ client[0] + "/" + client[1] + ", then " + afterPut);
			assertTrue(bridle && head == CableAnchorBlock.Return.PULLEY, "hung over the network: '" + hung + "', bridle " + bridle + ", anchor " + head);
			assertTrue(sameLevel == 0 && Math.abs(stopped - SkipStations.UNLOAD_OFFSET) < 1.0E-6 && gate && cart == 64 && left == 0,
				"chute exchange over the network: same level " + sameLevel + ", stopped " + stopped + ", gate " + gate + ", cart " + cart + ", skip " + left);
			server.computeOnServer(s -> {
				s.overworld().getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).forEach(e -> e.setCargo(Items.AIR, 0));
				return null;
			});
		}
	}

	/** The drum (coal winch) east of the skip, its one anchor over the skip's column. */
	private static final int DRUM_X = X - 5;
	private static final int HEAD = 5;

	/**
	 * Routes the cable by hand from the drum to the anchor over the skip, winches the free hook down onto
	 * the skip with the coal winch, picks it up from the floor and uses it on the skip. Returns what the
	 * player was told.
	 */
	private static String hang(ClientGameTestContext context, TestDedicatedServerContext server, int g) {
		server.runCommand("setblock " + DRUM_X + " " + g + " " + Z + " simple_block:cable_drum[facing=east]");
		server.runCommand("setblock " + X + " " + (g + HEAD + 1) + " " + Z + " minecraft:oak_planks");
		server.runCommand("setblock " + X + " " + (g + HEAD) + " " + Z + " simple_block:cable_anchor");
		server.runCommand("setblock " + (X + 2) + " " + (g + 2) + " " + Z + " minecraft:stone");
		server.runCommand("tp @a " + (DRUM_X + 1.5) + " " + g + " " + (Z + 2.5) + " 180 30");
		hold(context, server, "simple_block:cable_coil");
		lookAtPoint(context, new Vec3(DRUM_X + 0.5, g + 0.85, Z + 0.5));
		use(context);
		server.runCommand("clear @a");
		context.waitTicks(5);
		lookAtHook(context);
		use(context);
		// On a block beside the column, within reach of the anchor: hung on it, then the end put down.
		server.runCommand("tp @a " + (X + 2.5) + " " + (g + 3) + " " + (Z + 0.5) + " 90 -20");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(X + 0.5, g + HEAD + 0.6, Z + 0.5));
		use(context);
		use(context);
		// The coal winch lowers the free hook onto the skip's top.
		server.runCommand("tp @a " + (DRUM_X + 0.5) + " " + g + " " + (Z - 2.5) + " 0 40");
		hold(context, server, "minecraft:furnace");
		lookAtPoint(context, new Vec3(DRUM_X + 0.5, g + 0.85, Z + 0.5));
		use(context);
		hold(context, server, "minecraft:coal 3");
		for (int i = 0; i < 3; i++) {
			use(context);
		}
		server.runCommand("clear @a");
		lever(context, server, g, CableLines.Lever.FEED);
		for (int i = 0; i < 30 && server.computeOnServer(s -> line(s.overworld(), g).lever()) != CableLines.Lever.NEUTRAL; i++) {
			context.waitTicks(10);
		}
		// Picked up from the floor and used on the skip.
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 2.9) + " 180 0");
		context.waitTicks(10);
		lookAtPoint(context, new Vec3(X + 0.5, g + 3.45, Z + 0.5));
		use(context);
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.5));
		use(context);
		String said = overlayMessage(context);
		context.waitTicks(10);
		return said;
	}

	private static void use(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
	}

	private static void hold(ClientGameTestContext context, TestDedicatedServerContext server, String item) {
		server.runCommand("item replace entity @a weapon.mainhand with " + item);
		context.waitTicks(5);
	}

	private static CableLines.Line line(ServerLevel level, int g) {
		return CableLines.line(level, new BlockPos(DRUM_X, g, Z));
	}

	/** Sets the coal winch's lever by clicking it. */
	private static void lever(ClientGameTestContext context, TestDedicatedServerContext server, int g, CableLines.Lever lever) {
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a " + (DRUM_X + 0.5) + " " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(DRUM_X + 0.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		for (int i = 0; i < 3 && server.computeOnServer(s -> line(s.overworld(), g).lever()) != lever; i++) {
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(3);
		}
		CableLines.Lever now = server.computeOnServer(s -> line(s.overworld(), g).lever());
		assertTrue(now == lever, "lever: " + now + ", wanted " + lever);
	}

	private static int cartCount(ServerLevel level, int g) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(X, g - 1, Z - 1, X + 1, g + 2, Z)).stream()
			.mapToInt(c -> MinecartCargo.cart(c).simpleBlock$count()).sum();
	}

	private static OreSkip skip(ServerLevel level, int g) {
		return level.getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no skip"));
	}
}
