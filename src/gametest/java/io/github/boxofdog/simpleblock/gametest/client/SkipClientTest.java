package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.drumOf;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtHook;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import java.util.ArrayList;
import java.util.List;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.client.skip.SkipScreen;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipCargo;
import io.github.boxofdog.simpleblock.skip.SkipMenu;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M07 in a real client, by key presses and mouse clicks:
 * <ul>
 *   <li><b>Placing and loading:</b> the skip placed from its item on the shaft's floor; raw iron loaded
 *       by hand to 512; its screen opened with an empty hand; real clicks on the cargo slot take one
 *       stack onto the cursor and put it back, and a shift-click moves a stack into the inventory.</li>
 *   <li><b>Hanging:</b> the cable routed by hand through two anchors to the head of the shaft; its end
 *       picked up and used on the skip.</li>
 *   <li><b>Unloaded chunks:</b> the coal winch set to PULL, the player goes far away (the skip's and the
 *       drum's chunks unload) and comes back: the skip has not moved while unloaded and the lever is at
 *       NEUTRAL.</li>
 *   <li><b>Stations:</b> PULL stops it at the top station, where a docked cart tips its load in through
 *       its ramp; FEED takes it down to the bottom station, where its discharge gate fills an empty cart.</li>
 *   <li><b>Save and reopen:</b> cargo, height and cable are as they were, and the winch lifts it again.</li>
 * </ul>
 * Screenshots: {@code m07-skip-*}. The yard, at ground level {@code g}: stone x 196..228, z 0..14; the drum
 * at (202, g, 6) facing east, anchors under planks at (207, g + 6, 6) and (212, g + 10, 6) (the head of
 * the shaft), the skip's column at x 212, z 6, facing north, with Loading Rails at (212, g, 5) and
 * (212, g + 8, 5) facing south; pillars to stand on at (207, g + 2, 9) and (212, g + 6, 9).
 */
public class SkipClientTest implements FabricClientGameTest {
	private static final int X = 212;
	private static final int Z = 6;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(5);
			client.options.simulationDistance().set(5);
		});
		TestWorldSave save;
		int g;
		Snapshot before;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			TestServerContext server = world.getServer();
			g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			server.runCommand("tp @a " + (X + 0.5) + " " + g + " 2.5 0 30");
			world.getConnection().waitForChunksRender();
			buildYard(server, g);
			context.waitTicks(20);

			placeAndLoad(context, server, g);
			hang(context, server, g);
			unloaded(context, world, server, g);
			stations(context, server, g);
			before = snapshot(server, g);
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			reopened(context, world.getServer(), g, before);
		}
	}

	private static void buildYard(TestServerContext server, int g) {
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @a");
		server.runCommand("fill 196 " + (g - 1) + " 0 228 " + (g - 1) + " 14 minecraft:stone");
		server.runCommand("fill 196 " + g + " 0 228 " + (g + 12) + " 14 minecraft:air");
		server.runCommand("setblock 202 " + g + " " + Z + " simple_block:cable_drum[facing=east]");
		server.runCommand("setblock 207 " + (g + 7) + " " + Z + " minecraft:oak_planks");
		server.runCommand("setblock 207 " + (g + 6) + " " + Z + " simple_block:cable_anchor");
		server.runCommand("setblock " + X + " " + (g + 11) + " " + Z + " minecraft:oak_planks");
		server.runCommand("setblock " + X + " " + (g + 10) + " " + Z + " simple_block:cable_anchor");
		server.runCommand("fill 207 " + g + " 9 207 " + (g + 2) + " 9 minecraft:stone");
		server.runCommand("fill " + X + " " + g + " 9 " + X + " " + (g + 6) + " 9 minecraft:stone");
		server.runCommand("setblock " + X + " " + g + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
		server.runCommand("setblock " + X + " " + (g + 7) + " " + (Z - 1) + " minecraft:stone");
		server.runCommand("setblock " + X + " " + (g + 8) + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	private static void use(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
	}

	private static void hold(TestServerContext server, ClientGameTestContext context, String item) {
		server.runCommand("item replace entity @a weapon.mainhand with " + item);
		context.waitTicks(5);
	}

	private static OreSkip skip(ServerLevel level, int g) {
		return level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 14, Z + 3)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no skip in the shaft"));
	}

	private static double skipY(TestServerContext server, int g) {
		return server.computeOnServer(s -> skip(s.overworld(), g).getY() - g);
	}

	private static CableLines.Line line(ServerLevel level, int g) {
		return CableLines.line(level, new BlockPos(202, g, Z));
	}

	// ---- Placing, loading, the screen ----

	private static void placeAndLoad(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " 2.5 0 30");
		hold(server, context, "simple_block:ore_skip");
		lookAtPoint(context, new Vec3(X + 0.5, g, Z + 0.5));
		use(context);
		String placed = server.computeOnServer(s -> skip(s.overworld(), g) + " facing " + skip(s.overworld(), g).facing());
		boolean clientSees = context.computeOnClient(client -> !client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 6, Z + 3)).isEmpty());
		server.runCommand("tp @a " + (X - 2.5) + " " + (g + 1) + " " + (Z - 3.5) + " -35 15");
		context.waitTicks(10);
		context.takeScreenshot("m07-skip-placed");
		assertTrue(placed.contains("facing north") && clientSees, "placed by key: " + placed + ", client sees it " + clientSees);

		// Raw iron by hand: 448 already in (set on the server), 64 more by key: 512.
		server.computeOnServer(s -> {
			skip(s.overworld(), g).setCargo(Items.RAW_IRON, SkipCargo.CAPACITY - 64);
			return null;
		});
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z - 2.0) + " 0 20");
		hold(server, context, "minecraft:raw_iron 64");
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.5));
		use(context);
		String loaded = overlayMessage(context);
		int count = server.computeOnServer(s -> skip(s.overworld(), g).count());
		assertTrue(count == SkipCargo.CAPACITY && loaded.contains("512 / 512"), "loaded by hand: " + count + " '" + loaded + "'");

		// The screen, with an empty hand; real clicks on its cargo slot.
		server.runCommand("clear @a");
		context.waitTicks(5);
		use(context);
		context.waitTicks(5);
		boolean open = context.computeOnClient(client -> client.gui.screen() instanceof SkipScreen);
		assertTrue(open, "an empty hand opens the skip's screen");
		int shown = context.computeOnClient(client -> ((SkipScreen) client.gui.screen()).getMenu().count());
		context.takeScreenshot("m07-skip-screen-512");
		double[] slot = context.computeOnClient(client -> {
			double scale = client.getWindow().getGuiScale();
			int left = (client.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (client.getWindow().getGuiScaledHeight() - 133) / 2;
			return new double[] {(left + SkipMenu.CARGO_X + 8) * scale, (top + SkipMenu.CARGO_Y + 8) * scale};
		});
		List<String> clicks = new ArrayList<>();
		context.getInput().setCursorPos(slot[0], slot[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		clicks.add(clickState(context, server, g, "left"));
		context.takeScreenshot("m07-skip-screen-stack-taken");
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		clicks.add(clickState(context, server, g, "left again"));
		context.getInput().holdShift();
		context.getInput().pressMouse(0);
		context.getInput().releaseShift();
		context.waitTicks(5);
		clicks.add(clickState(context, server, g, "shift"));
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		SimpleBlockTestLog.info("CLIENT SKIP: placed by key ({}); 64 raw iron loaded by hand: '{}'; screen shows {}; clicks on the cargo slot: {}",
			placed, loaded, shown, clicks);
		assertTrue(shown == SkipCargo.CAPACITY, "the screen shows " + shown);
		assertTrue(clicks.get(0).startsWith("left: skip 448 (client 448), cursor 64"), clicks.get(0));
		assertTrue(clicks.get(1).startsWith("left again: skip 512 (client 512), cursor 0"), clicks.get(1));
		assertTrue(clicks.get(2).startsWith("shift: skip 448 (client 448), cursor 0, inventory 64"), clicks.get(2));
		server.runCommand("clear @a");
		// Back to 300 for the stations.
		server.computeOnServer(s -> {
			skip(s.overworld(), g).setCargo(Items.RAW_IRON, 300);
			return null;
		});
	}

	private static String clickState(ClientGameTestContext context, TestServerContext server, int g, String what) {
		int skip = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int[] client = context.computeOnClient(c -> new int[] {((SkipScreen) c.gui.screen()).getMenu().count(), c.player.containerMenu.getCarried().getCount(),
			c.player.getInventory().countItem(Items.RAW_IRON)});
		int total = skip + client[1] + client[2];
		return what + ": skip " + skip + " (client " + client[0] + "), cursor " + client[1] + ", inventory " + client[2] + ", total " + total;
	}

	// ---- Routing the cable and hanging the skip ----

	private static void hang(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(202, g, Z);
		server.runCommand("tp @a 203.5 " + g + " " + (Z + 2.5) + " 180 30");
		hold(server, context, "simple_block:cable_coil");
		lookAtPoint(context, new Vec3(202.5, g + 0.85, Z + 0.5));
		use(context);
		server.runCommand("clear @a");
		context.waitTicks(5);
		lookAtHook(context);
		use(context);
		// Through the anchor at (207, g + 6) from the first pillar, then the head anchor from the second.
		server.runCommand("tp @a 207.5 " + (g + 3) + " 9.5 180 -40");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(207.5, g + 6.6, Z + 0.5));
		use(context);
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 7) + " 9.5 180 -40");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(X + 0.5, g + 10.6, Z + 0.5));
		use(context);
		List<BlockPos> route = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route());
		assertTrue(route.size() == 2, "routed through both anchors: " + route);
		// Down to the shaft's foot with the end in hand; use it on the skip.
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 2.5) + " 180 20");
		context.waitTicks(10);
		lookAtPoint(context, new Vec3(X + 0.5, g + 2.0, Z + 0.5));
		use(context);
		String message = overlayMessage(context);
		boolean hung = server.computeOnServer(s -> drumPos.equals(skip(s.overworld(), g).drum()));
		boolean clientCable = context.computeOnClient(client -> client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 14, Z + 3))
			.stream().anyMatch(skip -> skip.anchor() != null));
		server.runCommand("tp @a " + (X + 3.5) + " " + (g + 1) + " " + (Z + 3.5) + " 135 -15");
		context.waitTicks(10);
		context.takeScreenshot("m07-skip-hung-on-the-cable");
		SimpleBlockTestLog.info("CLIENT SKIP: cable routed by hand through {}; end used on the skip: '{}', hung {}, client draws its cable {}", route, message,
			hung, clientCable);
		assertTrue(hung && clientCable, "hung on the cable: " + hung + ", client " + clientCable + " ('" + message + "')");

		// A furnace and coal on the drum, by key.
		server.runCommand("tp @a 202.5 " + g + " " + (Z - 2.5) + " 0 40");
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(202.5, g + 0.85, Z + 0.5));
		use(context);
		hold(server, context, "minecraft:coal 4");
		for (int i = 0; i < 4; i++) {
			use(context);
		}
		server.runCommand("clear @a");
	}

	/** Clicks the coal winch's lever (on the drum's south side: it faces east) until it is at {@code lever}. */
	private static void lever(ClientGameTestContext context, TestServerContext server, int g, CableLines.Lever lever) {
		server.runCommand("tp @a 202.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(202.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		for (int i = 0; i < 3 && server.computeOnServer(s -> line(s.overworld(), g).lever()) != lever; i++) {
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(3);
		}
		CableLines.Lever now = server.computeOnServer(s -> line(s.overworld(), g).lever());
		assertTrue(now == lever, "lever: " + now + ", wanted " + lever);
	}

	// ---- The winch running while the skip is unloaded ----

	private static void unloaded(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server, int g) {
		lever(context, server, g, CableLines.Lever.PULL);
		// Straight away, far off: the shaft and the drum unload. Watch the skip until it is gone from the loaded world.
		server.runCommand("tp @a " + (X + 400.5) + " " + g + " " + (Z + 0.5));
		double left = Double.NaN;
		int ticks = 0;
		for (; ticks < 200; ticks++) {
			Double y = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 14, Z + 3))
				.stream().findFirst().map(skip -> skip.getY() - g).orElse(null));
			if (y == null) {
				break;
			}
			left = y;
			context.waitTicks(1);
		}
		int burn = server.computeOnServer(s -> line(s.overworld(), g).burn() + line(s.overworld(), g).coal() * 1600);
		boolean ticking = server.computeOnServer(s -> s.overworld().isPositionEntityTicking(new BlockPos(X, g, Z)));
		context.waitTicks(100);
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " 2.5 0 20");
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		double back = skipY(server, g);
		CableLines.Lever lever = server.computeOnServer(s -> line(s.overworld(), g).lever());
		String stop = server.computeOnServer(s -> line(s.overworld(), g).lastStop());
		int burnBack = server.computeOnServer(s -> line(s.overworld(), g).burn() + line(s.overworld(), g).coal() * 1600);
		SimpleBlockTestLog.info("CLIENT SKIP UNLOADED: PULL, then 400 blocks away: the skip left the loaded world after {} ticks at y {} (shaft entity-ticking {}); "
			+ "100 ticks later, back: y {}; lever {} ({}); coal burn {} -> {}", ticks, fmt(left), ticking, fmt(back), lever, stop, burn, burnBack);
		assertTrue(ticks < 200 && !ticking, "the shaft should unload with the player 400 blocks away");
		assertTrue(Math.abs(back - left) < 1.0E-6, "the skip does not move while unloaded: " + left + " -> " + back);
		assertTrue(lever == CableLines.Lever.NEUTRAL && burnBack == burn, "the winch stops (" + lever + ", " + stop + ") and burns nothing: " + burn + " -> " + burnBack);
	}

	// ---- Stations ----

	private static void stations(ClientGameTestContext context, TestServerContext server, int g) {
		// A cart with 200 raw iron at the top station (beside the bridle of a skip at g + 5); the bottom one comes later.
		server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (g + 8.1) + " " + (Z - 0.5));
		context.waitTicks(5);
		server.computeOnServer(s -> {
			MinecartCargo.cart(cart(s.overworld(), g + 8)).simpleBlock$setCargo(Items.RAW_IRON, 200);
			return null;
		});
		context.waitTicks(5);
		lever(context, server, g, CableLines.Lever.PULL);
		server.runCommand("tp @a " + (X - 2.5) + " " + (g + 8) + " " + (Z - 3.5) + " -40 20");
		boolean[] seen = new boolean[3];
		for (int i = 0; i < 300 && !seen[0]; i++) {
			context.waitTicks(1);
			seen[0] = context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, new AABB(X - 1, g + 7, Z - 2, X + 2, g + 10, Z))
				.stream().anyMatch(cart -> LoadingTracks.freight(cart).simpleBlock$gateOpen()));
		}
		context.waitTicks(6);
		boolean lid = context.computeOnClient(client -> client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 14, Z + 3))
			.stream().anyMatch(OreSkip::lidOpen));
		context.takeScreenshot("m07-skip-loading-ramp");
		context.waitTicks(80);
		double top = skipY(server, g);
		int skipCount = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int topCart = server.computeOnServer(s -> MinecartCargo.cart(cart(s.overworld(), g + 8)).simpleBlock$count());
		String stop = server.computeOnServer(s -> line(s.overworld(), g).lastStop());
		SimpleBlockTestLog.info("CLIENT SKIP STATION: PULL stopped at y {} ({}); client saw the cart's ramp down {} and the skip's lid open {}; "
			+ "skip 300 -> {}, top cart 200 -> {}", fmt(top), stop, seen[0], lid, skipCount, topCart);
		assertTrue(Math.abs(top - 5.0) < 1.0E-6 && "docked".equals(stop), "stopped at the top station: " + top + " (" + stop + ")");
		assertTrue(seen[0] && lid && skipCount == 500 && topCart == 0, "loaded through the ramp: " + seen[0] + "/" + lid + ", " + skipCount + "/" + topCart);

		// The bottom: an empty cart beside the chute; FEED down.
		server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (g + 0.1) + " " + (Z - 0.5));
		context.waitTicks(5);
		lever(context, server, g, CableLines.Lever.FEED);
		server.runCommand("tp @a " + (X - 2.5) + " " + (g + 1) + " " + (Z - 3.5) + " -40 10");
		for (int i = 0; i < 300 && !seen[2]; i++) {
			context.waitTicks(1);
			seen[2] = context.computeOnClient(client -> client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 14, Z + 3))
				.stream().anyMatch(OreSkip::gateOpen));
		}
		context.waitTicks(16);
		boolean bottomRamp = context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, new AABB(X - 1, g - 1, Z - 2, X + 2, g + 2, Z))
			.stream().anyMatch(cart -> LoadingTracks.freight(cart).simpleBlock$gateOpen()));
		context.takeScreenshot("m07-skip-unloading-chute");
		context.waitTicks(100);
		double bottom = skipY(server, g);
		int left = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int bottomCart = server.computeOnServer(s -> MinecartCargo.cart(cart(s.overworld(), g)).simpleBlock$count());
		SimpleBlockTestLog.info("CLIENT SKIP STATION: FEED down to y {}; client saw the discharge gate open {}, the cart's front stayed shut {}; skip 500 -> {}, "
			+ "bottom cart 0 -> {}", fmt(bottom), seen[2], !bottomRamp, left, bottomCart);
		assertTrue(Math.abs(bottom) < 1.0E-6 && seen[2] && !bottomRamp && left == 244 && bottomCart == 256,
			"unloaded through the chute: y " + bottom + ", gate " + seen[2] + ", ramp " + bottomRamp + ", " + left + "/" + bottomCart);
	}

	private static Minecart cart(ServerLevel level, int y) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(X, y - 0.5, Z - 1, X + 1, y + 1, Z)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no cart at y " + y));
	}

	// ---- Save and reopen ----

	private record Snapshot(double y, int count, boolean hung, boolean lineHoist, int burn) {
	}

	private static Snapshot snapshot(TestServerContext server, int g) {
		return server.computeOnServer(s -> {
			OreSkip skip = skip(s.overworld(), g);
			CableLines.Line line = line(s.overworld(), g);
			return new Snapshot(skip.getY() - g, skip.count(), skip.drum() != null, skip.getUUID().equals(line.hoist()), line.burn() + line.coal() * 1600);
		});
	}

	private static void reopened(ClientGameTestContext context, TestServerContext server, int g, Snapshot before) {
		context.waitTicks(20);
		Snapshot after = snapshot(server, g);
		lever(context, server, g, CableLines.Lever.PULL);
		context.waitTicks(40);
		double lifted = skipY(server, g);
		SimpleBlockTestLog.info("CLIENT SKIP REOPEN: before {}; after saving and reopening {}; PULL 40 ticks: y {} -> {}", before, after, fmt(after.y()), fmt(lifted));
		assertTrue(after.equals(before), "kept through save and reopen: " + before + " / " + after);
		assertTrue(lifted > after.y() + 0.5, "the winch lifts it after reopening: " + after.y() + " -> " + lifted);
	}

	private static String fmt(double value) {
		return String.format("%.2f", value);
	}
}
