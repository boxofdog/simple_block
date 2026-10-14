package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.drumOf;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtHook;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.RemovedRocks;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M06 in the real client, in a fresh survival world, every action a simulated key press:
 * <ul>
 *   <li><b>Coal winch:</b> a cable loop as in M05 (one coil), a furnace used on the drum, coal put in,
 *       the body inspected with an empty hand, and the lever clicked NEUTRAL, PULL, FEED.</li>
 *   <li><b>Loading Track:</b> placed by the player; the winch hauls a rod cart onto it: it docks, front
 *       (gate) the track's way, and the lever goes back to NEUTRAL. The track's post releases it.</li>
 *   <li><b>Train at the station:</b> an engine and two coupled freight carts run onto a loading track:
 *       the whole train is held and the engine switched off; release; started again it passes over.</li>
 *   <li><b>No shoving:</b> the player walks into a freight cart (it stays put) and a cushioned one (it
 *       is pushed).</li>
 *   <li><b>Creative menu:</b> the creative inventory's search shows rails and the loading track, and no
 *       retired item; no tab lists one.</li>
 *   <li><b>A long line (section 7):</b> a 390-block line routed anchor by anchor; with the client's
 *       simulation distance at 5 chunks, the winch hauls the cart until it reaches the edge of the
 *       ticking world and stops there; the player goes far away (the drum unloads) and comes back,
 *       then the world is saved and reopened: line, cart, coal and partly burnt coal are exactly as
 *       they were, the lever at NEUTRAL, and PULL brings the cart back.</li>
 * </ul>
 * Screenshots go to {@code build/run/clientGameTest/screenshots} ({@code m06-*}).
 *
 * <p>The yard, at ground level {@code g}: stone x 18..52, z -4..14. The winch line runs along z 6: the
 * drum at x 20 facing east, rails x 22..50, anchors under planks at x 28, 34 and 40 (M07 playtest patch
 * §10: the last support straight over the loading track at x 40, no cable past it; in M06 it was at x
 * 44). The train track runs along z 0, x 22..50, a loading track at x 44. The long line runs along
 * z 30 from a drum at x 0 to x 390.
 */
public class FreightStationClientTest implements FabricClientGameTest {
	private static final int Z = 6;
	private static final int TRACK_Z = 0;
	private static final int[] ANCHOR_X = {28, 34, 40};
	private static final int LONG_Z = 30;
	private static final int LONG_END = 390;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(5);
			client.options.simulationDistance().set(5);
		});
		TestWorldSave save;
		int g;
		LongLine before;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			TestServerContext server = world.getServer();
			g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 35, Z));
			buildYard(server, g);
			server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
			world.getConnection().waitForChunksRender();

			winchLoop(context, server, g);
			winch(context, server, g);
			cableDock(context, server, g);
			trainDock(context, server, g);
			noShoving(context, server, g);
			creativeMenu(context, server, g);
			before = longLine(context, world, g);
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			afterReopen(context, world, g, before);
		}
	}

	private static void buildYard(TestServerContext server, int g) {
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @a");
		server.runCommand("fill 18 " + (g - 1) + " -4 52 " + (g - 1) + " 14 minecraft:stone");
		server.runCommand("fill 18 " + g + " -4 52 " + (g + 8) + " 14 minecraft:air");
		server.runCommand("fill 22 " + g + " " + Z + " 50 " + g + " " + Z + " minecraft:rail[shape=east_west]");
		server.runCommand("fill 22 " + g + " " + TRACK_Z + " 50 " + g + " " + TRACK_Z + " minecraft:rail[shape=east_west]");
		server.runCommand("fill 22 " + g + " 3 40 " + g + " 3 minecraft:rail[shape=east_west]");
		for (int x : ANCHOR_X) {
			server.runCommand("setblock " + x + " " + (g + 3) + " " + Z + " minecraft:oak_planks");
		}
		server.runCommand("setblock 20 " + g + " " + Z + " simple_block:cable_drum[facing=east]");
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

	// ---- The winch line (M05 loop, one coil) ----

	private static void winchLoop(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		hold(server, context, "simple_block:cable_coil");
		lookAtPoint(context, new Vec3(20.5, g + 0.85, Z + 0.5));
		use(context);
		hold(server, context, "simple_block:cable_anchor 4");
		for (int x : ANCHOR_X) {
			server.runCommand("tp @a " + (x - 1.5) + " " + g + " " + (Z + 0.5) + " -90 -45");
			context.waitTicks(5);
			lookAtPoint(context, new Vec3(x + 0.5, g + 3, Z + 0.5));
			use(context);
		}
		server.runCommand("clear @a");
		server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
		context.waitTicks(5);
		lookAtHook(context);
		use(context);
		for (int x : ANCHOR_X) {
			server.runCommand("tp @a " + (x - 2.5) + " " + g + " " + (Z + 0.5) + " -90 -30");
			context.waitTicks(5);
			context.getInput().lookAt(new BlockPos(x, g + 2, Z));
			use(context);
		}
		use(context);
		hold(server, context, "simple_block:cable_anchor");
		context.getInput().lookAt(new BlockPos(ANCHOR_X[2], g + 2, Z));
		use(context);
		boolean looped = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).looped());
		assertTrue(looped, "the winch line's loop should be closed");
		server.runCommand("clear @a");

		// A rod cart under the line.
		server.runCommand("tp @a 25.5 " + g + " 3.5 0 30");
		hold(server, context, "minecraft:minecart");
		lookAtPoint(context, new Vec3(25.5, g + 0.1, Z + 0.5));
		use(context);
		hold(server, context, "simple_block:copper_connecting_rod");
		lookAtPoint(context, new Vec3(25.5, g + 0.4, Z + 0.5));
		use(context);
		context.waitTicks(5);
		boolean gripping = server.computeOnServer(s -> cartOn(s.overworld(), g, Z) instanceof Minecart cart && ConnectingRods.isGripping(cart));
		assertTrue(gripping, "the rod cart should grip the working strand");
		server.runCommand("clear @a");
	}

	// ---- The coal winch ----

	private static void winch(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("tp @a 20.5 " + g + " " + (Z - 2.5) + " 0 40");
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(20.5, g + 0.85, Z + 0.5));
		use(context);
		boolean powered = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).powered());
		boolean clientPowered = context.computeOnClient(client -> client.level.getBlockEntity(drumPos) instanceof CableDrumBlockEntity drum && drum.powered());
		int furnaces = context.computeOnClient(client -> client.player.getMainHandItem().getCount());
		boolean stillLooped = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).looped());
		hold(server, context, "minecraft:coal 3");
		for (int i = 0; i < 3; i++) {
			use(context);
		}
		int coal = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).coal());
		server.runCommand("clear @a");
		context.waitTicks(5);
		use(context);
		String inspect = overlayMessage(context);
		CableLines.Lever afterInspect = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		server.runCommand("tp @a 18.0 " + (g + 1) + " 3.5 -40 25");
		context.waitTicks(10);
		context.takeScreenshot("m06-winch-furnace-side");
		SimpleBlockTestLog.info("CLIENT WINCH: furnace on the drum: powered {} (client {}), furnace used {}, loop kept {}; 3 coal in: {}; "
			+ "empty hand on the body: '{}', lever {}", powered, clientPowered, furnaces == 0, stillLooped, coal, inspect, afterInspect);
		assertTrue(powered && clientPowered && furnaces == 0 && stillLooped, "fitting the furnace");
		assertTrue(coal == 3, "coal in: " + coal);
		assertTrue(inspect.contains("3 coal") && afterInspect == CableLines.Lever.NEUTRAL, "inspecting: '" + inspect + "', lever " + afterInspect);

		// The lever is where the crank was: on the drum's south side (facing east).
		server.runCommand("tp @a 20.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		context.takeScreenshot("m06-winch-lever-neutral");
		// The owner's clicks (2026-10-13), a couple of ticks apart: right-click PULL, left-click FEED (straight
		// reversal), left-click again NEUTRAL.
		List<String> cycle = new ArrayList<>();
		for (String shot : List.of("m06-winch-lever-pull", "m06-winch-lever-feed", "m06-winch-lever-neutral-again")) {
			if (shot.endsWith("pull")) {
				context.getInput().pressKey(options -> options.keyUse);
			} else {
				context.getInput().pressKey(options -> options.keyAttack);
			}
			context.waitTicks(2);
			CableLines.Lever server1 = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
			CableLines.Lever client1 = context.computeOnClient(client -> ((CableDrumBlockEntity) client.level.getBlockEntity(drumPos)).lever());
			context.takeScreenshot(shot);
			cycle.add(server1 + "/" + client1);
		}
		SimpleBlockTestLog.info("CLIENT WINCH: lever right-click, left-click, left-click (server/client): {}", cycle);
		assertTrue(cycle.equals(List.of("PULL/PULL", "FEED/FEED", "NEUTRAL/NEUTRAL")), "lever cycle: " + cycle);
	}

	// ---- A cable-hauled cart docks ----

	private static void cableDock(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		BlockPos station = new BlockPos(40, g, Z);
		// The player puts the loading track down in place of the rail at x 40, facing east.
		server.runCommand("setblock 40 " + g + " " + Z + " minecraft:air");
		server.runCommand("tp @a 37.5 " + g + " " + (Z + 0.5) + " -90 40");
		hold(server, context, "simple_block:loading_track");
		lookAtPoint(context, new Vec3(40.5, g, Z + 0.5));
		use(context);
		String track = server.computeOnServer(s -> s.overworld().getBlockState(station).toString());
		assertTrue(track.contains("simple_block:loading_track") && track.contains("facing=east"), "placed: " + track);
		server.runCommand("clear @a");

		// A left-click: FEED, the cart is hauled east onto the track.
		server.runCommand("tp @a 20.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		context.getInput().pressKey(options -> options.keyAttack);
		context.waitTicks(2);
		CableLines.Lever second = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		assertTrue(second == CableLines.Lever.FEED, "left-click: FEED, was " + second);
		int waited = 0;
		while (waited < 600 && !server.computeOnServer(s -> cartOn(s.overworld(), g, Z) instanceof Minecart cart && Couplings.coupled(cart).simpleBlock$dock() != null)) {
			context.waitTicks(10);
			waited += 10;
		}
		context.waitTicks(5);
		String stop = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lastStop());
		CableLines.Lever lever = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		CableLines.Lever clientLever = context.computeOnClient(client -> ((CableDrumBlockEntity) client.level.getBlockEntity(drumPos)).lever());
		double x = server.computeOnServer(s -> cartOn(s.overworld(), g, Z).getX());
		Direction clientFront = context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class,
			new AABB(18, g - 1, Z - 1, 52, g + 4, Z + 1)).stream().findFirst().map(cart -> LoadingTracks.freight(cart).simpleBlock$front()).orElse(null));
		// From the south-east: the cart's front (east end, with its gate) and the track's post.
		server.runCommand("tp @a 42.5 " + g + " " + (Z + 2.5) + " 0 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(40.9, g + 0.3, Z + 0.5));
		context.waitTicks(10);
		context.takeScreenshot("m06-station-cable-docked");
		server.runCommand("tp @a 20.5 " + (g + 1) + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(10);
		context.takeScreenshot("m06-winch-lever-back-to-neutral");
		List<BlockPos> route = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route());
		SimpleBlockTestLog.info("CLIENT STATION: loading track placed facing east ({}); FEED: docked after {} ticks at x {}, lever {} (client {}) "
			+ "because '{}', client sees the cart's front {}; the cable's last support at x {} (patch §10: none past the track)", track, waited, x, lever,
			clientLever, stop, clientFront, route.getLast().getX());
		assertTrue(route.getLast().getX() == station.getX(), "the last support should be over the track: " + route);
		assertTrue("docked".equals(stop) && lever == CableLines.Lever.NEUTRAL && clientLever == CableLines.Lever.NEUTRAL, "lever after docking: " + lever + " (" + stop + ")");
		assertTrue(Math.abs(x - 40.5) < 1.0E-3 && clientFront == Direction.EAST, "docked at x " + x + ", client front " + clientFront);

		// M06 correction: the hinged stop stands behind the cart (west of an east-facing rail, on its left, north,
		// rail's inside edge). Close-up, then release by right-clicking the stop from behind the cart.
		boolean raised = server.computeOnServer(s -> s.overworld().getBlockState(station).getValue(LoadingTrackBlock.RAISED));
		boolean clientRaised = context.computeOnClient(client -> client.level.getBlockState(station).getValue(LoadingTrackBlock.RAISED));
		server.runCommand("tp @a 38.0 " + g + " " + (Z + 1.8) + " 0 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(40.0, g + 0.3, Z + 0.4));
		context.waitTicks(10);
		context.takeScreenshot("m06fix-stop-raised-behind-cart");
		server.runCommand("tp @a 37.3 " + g + " " + (Z + 0.3) + " -90 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(39.82, g + 0.4, Z + 0.32));
		String target = context.computeOnClient(client -> client.hitResult == null ? "none" : client.hitResult.getType() + " "
			+ (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult block ? block.getBlockPos().toShortString() : "entity"));
		use(context);
		String released = overlayMessage(context);
		boolean free = server.computeOnServer(s -> Couplings.coupled(cartOn(s.overworld(), g, Z)).simpleBlock$dock() == null);
		context.waitTicks(10);
		boolean folded = !server.computeOnServer(s -> s.overworld().getBlockState(station).getValue(LoadingTrackBlock.RAISED));
		boolean clientFolded = !context.computeOnClient(client -> client.level.getBlockState(station).getValue(LoadingTrackBlock.RAISED));
		server.runCommand("tp @a 38.0 " + g + " " + (Z + 1.8) + " 0 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(40.0, g + 0.3, Z + 0.4));
		context.waitTicks(10);
		context.takeScreenshot("m06fix-stop-folded-after-release");
		context.waitTicks(20);
		double still = server.computeOnServer(s -> cartOn(s.overworld(), g, Z).getX());
		SimpleBlockTestLog.info("CLIENT STATION: stop raised behind the docked cart {} (client {}); aimed at the stop from behind the cart: {}; "
			+ "used: '{}', released {}, stop folded {} (client {}); 40 ticks later x {} (nothing restarts)", raised, clientRaised, target, released, free,
			folded, clientFolded, still);
		assertTrue(raised && clientRaised, "the stop should be raised behind the docked cart");
		assertTrue(target.startsWith("BLOCK") && target.contains(station.toShortString()), "the raised stop should be the target, not the cart: " + target);
		assertTrue(free && folded && clientFolded && Math.abs(still - x) < 0.01, "release by the stop: free " + free + ", folded " + folded + ", x " + x
			+ " -> " + still);

		// BUG-002 in the client: PULL back to the drum's end, where the lever drops to NEUTRAL; a left-click is FEED.
		server.runCommand("tp @a 20.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(3);
		CableLines.Lever pulling = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		assertTrue(pulling == CableLines.Lever.PULL, "first click after release: PULL, was " + pulling);
		int pulled = 0;
		while (pulled < 600 && server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever()) != CableLines.Lever.NEUTRAL) {
			context.waitTicks(10);
			pulled += 10;
		}
		double atEnd = server.computeOnServer(s -> cartOn(s.overworld(), g, Z).getX());
		String endStop = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lastStop());
		context.getInput().pressKey(options -> options.keyAttack);
		context.waitTicks(2);
		CableLines.Lever afterEnd = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		String skipMessage = overlayMessage(context);
		context.waitTicks(40);
		double away = server.computeOnServer(s -> cartOn(s.overworld(), g, Z).getX());
		context.getInput().pressKey(options -> options.keyAttack);
		context.waitTicks(5);
		SimpleBlockTestLog.info("CLIENT BUG-002: PULL ran the cart back to x {} ({}); left-click: {} ('{}'); 40 ticks later x {}", atEnd, endStop,
			afterEnd, skipMessage, away);
		// Stopped at the drum's end: by the cable's end zone, or (since BUG-003/004 let the rod keep its grip lower
		// down as the cable dips to the drum) by the end of the track just before it.
		assertTrue(("end".equals(endStop) || "track_end".equals(endStop)) && afterEnd == CableLines.Lever.FEED && away - atEnd > 2.0,
			"at the drum's end a left-click should be FEED: "
			+ endStop + ", " + afterEnd + ", x " + atEnd + " -> " + away);
	}

	// ---- A train docks ----

	private static void trainDock(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos station = new BlockPos(44, g, TRACK_Z);
		server.runCommand("setblock 44 " + g + " " + TRACK_Z + " minecraft:air");
		server.runCommand("tp @a 46.5 " + g + " " + (TRACK_Z + 0.5) + " 90 40");
		hold(server, context, "simple_block:loading_track");
		lookAtPoint(context, new Vec3(44.5, g, TRACK_Z + 0.5));
		use(context);
		String track = server.computeOnServer(s -> s.overworld().getBlockState(station).toString());
		assertTrue(track.contains("facing=west"), "train station placed facing west: " + track);

		// Engine at x 26, freight carts at 28 and 30, coupled with chains; coal in the engine.
		server.runCommand("tp @a 28.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		for (int x : new int[] {26, 28, 30}) {
			hold(server, context, "minecraft:minecart");
			lookAtPoint(context, new Vec3(x + 0.5, g + 0.1, TRACK_Z + 0.5));
			use(context);
		}
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(26.5, g + 0.35, TRACK_Z + 0.5));
		use(context);
		hold(server, context, "minecraft:coal 2");
		lookAtPoint(context, new Vec3(26.5, g + 0.35, TRACK_Z + 0.5));
		use(context);
		hold(server, context, "minecraft:iron_chain 4");
		for (double[] pair : new double[][] {{26.6, 28.4}, {28.6, 30.4}}) {
			lookAtPoint(context, new Vec3(pair[0], g + 0.35, TRACK_Z + 0.5));
			use(context);
			lookAtPoint(context, new Vec3(pair[1], g + 0.35, TRACK_Z + 0.5));
			use(context);
		}
		context.waitTicks(20);
		context.takeScreenshot("m06-train-coupled");
		server.runCommand("clear @a");
		String cars = server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().map(c -> c.getType().toShortString() + "@" + fmt(c.getX())
			+ (Couplings.hasCouplings(c) ? "+" : "")).toList().toString());
		SimpleBlockTestLog.info("CLIENT TRAIN: cars on the train track: {}", cars);
		int coupled = server.computeOnServer(s -> (int) carts(s.overworld(), g, TRACK_Z).stream().filter(Couplings::hasCouplings).count());
		assertTrue(coupled == 3, "three coupled cars: " + coupled);

		// Start it from the west: it runs east to the station.
		server.runCommand("tp @a 23.5 " + g + " " + (TRACK_Z + 1.5) + " -90 20");
		context.waitTicks(5);
		Vec3 engine = server.computeOnServer(s -> engineOn(s.overworld(), g).getBoundingBox().getCenter());
		lookAtPoint(context, engine);
		use(context);
		int waited = 0;
		while (waited < 600 && !server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().anyMatch(c -> Couplings.coupled(c).simpleBlock$dock() != null))) {
			context.waitTicks(10);
			waited += 10;
		}
		context.waitTicks(10);
		List<Double> held = server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().map(AbstractMinecart::getX).sorted().toList());
		boolean running = server.computeOnServer(s -> CobblestoneEngine.engine(engineOn(s.overworld(), g)).simpleBlock$running());
		// From the south-west: the lead cart's front (west end, with its gate) and the train behind.
		server.runCommand("tp @a 42.0 " + g + " " + (TRACK_Z + 3.0) + " 0 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(44.2, g + 0.3, TRACK_Z + 0.5));
		context.waitTicks(10);
		context.takeScreenshot("m06-station-train-docked");
		context.waitTicks(30);
		List<Double> later = server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().map(AbstractMinecart::getX).sorted().toList());
		SimpleBlockTestLog.info("CLIENT TRAIN: started east; docked after {} ticks, cars at {} (40 ticks later {}), engine running {}", waited, held,
			later, running);
		assertTrue(!running && held.equals(later), "the train should be held with the engine off: running " + running + ", " + held + " -> " + later);

		// Release with the stop, raised behind the lead cart (east of a west-facing rail, on its south side), then start again.
		server.runCommand("tp @a 47.7 " + g + " " + (TRACK_Z + 0.68) + " 90 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(45.18, g + 0.4, TRACK_Z + 0.68));
		use(context);
		context.waitTicks(20);
		List<Double> released = server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().map(AbstractMinecart::getX).sorted().toList());
		server.runCommand("tp @a " + (released.getFirst() - 2.5) + " " + g + " " + (TRACK_Z + 1.5) + " -90 20");
		context.waitTicks(5);
		lookAtPoint(context, server.computeOnServer(s -> engineOn(s.overworld(), g).getBoundingBox().getCenter()));
		use(context);
		for (int t = 0; t < 400 && server.computeOnServer(s -> engineOn(s.overworld(), g).getX()) < 45.0; t += 10) {
			context.waitTicks(10);
		}
		List<Double> gone = server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().map(AbstractMinecart::getX).sorted().toList());
		boolean anyDocked = server.computeOnServer(s -> carts(s.overworld(), g, TRACK_Z).stream().anyMatch(c -> Couplings.coupled(c).simpleBlock$dock() != null));
		SimpleBlockTestLog.info("CLIENT TRAIN: post used: cars {} (did not move); started again: cars at {}, docked {}", released, gone, anyDocked);
		assertTrue(released.equals(later), "release should not move the train: " + later + " -> " + released);
		assertTrue(gone.get(0) > 44.5 && !anyDocked, "started again, the whole train should pass the station: " + gone);
		server.runCommand("kill @e[type=minecraft:furnace_minecart]");
		server.runCommand("kill @e[type=minecraft:minecart,z=-1,dz=2]");
	}

	// ---- No shoving ----

	private static void noShoving(ClientGameTestContext context, TestServerContext server, int g) {
		int z = 3;
		server.runCommand("tp @a 30.5 " + g + " " + (z + 2.5) + " 180 30");
		hold(server, context, "minecraft:minecart");
		lookAtPoint(context, new Vec3(30.5, g + 0.1, z + 0.5));
		use(context);
		server.runCommand("clear @a");
		double start = server.computeOnServer(s -> cartOn(s.overworld(), g, z).getX());
		server.runCommand("tp @a 27.5 " + g + " " + (z + 0.5) + " -90 0");
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(40);
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(10);
		double freight = server.computeOnServer(s -> cartOn(s.overworld(), g, z).getX());
		double player = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getX());

		server.runCommand("tp @a 30.5 " + g + " " + (z + 2.5) + " 180 30");
		hold(server, context, "minecraft:blue_cushion");
		lookAtPoint(context, server.computeOnServer(s -> cartOn(s.overworld(), g, z).getBoundingBox().getCenter()));
		use(context);
		server.runCommand("clear @a");
		double seatStart = server.computeOnServer(s -> cartOn(s.overworld(), g, z).getX());
		server.runCommand("tp @a " + (seatStart - 3.0) + " " + g + " " + (z + 0.5) + " -90 0");
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(40);
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(10);
		double seat = server.computeOnServer(s -> cartOn(s.overworld(), g, z).getX());
		SimpleBlockTestLog.info("CLIENT PUSH: walked 40 ticks into a freight cart: x {} -> {} (player stopped at x {}); into a cushioned cart: "
			+ "x {} -> {}", fmt(start), fmt(freight), fmt(player), fmt(seatStart), fmt(seat));
		assertTrue(Math.abs(freight - start) < 0.01, "walking into a freight cart should not move it: " + start + " -> " + freight);
		assertTrue(seat - seatStart > 0.5, "walking into a cushioned cart should push it: " + seatStart + " -> " + seat);
		server.runCommand("kill @e[type=minecraft:minecart,z=2,dz=2]");
	}

	// ---- The creative menu ----

	private static Set<Item> retired() {
		Set<Item> items = new java.util.LinkedHashSet<>(List.of(Items.ELYTRA, Items.SHULKER_BOX, Items.ENDER_CHEST, Items.CHEST_MINECART,
			Items.FURNACE_MINECART, Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL));
		items.addAll(Items.DYED_SHULKER_BOX.asList());
		items.addAll(RemovedRocks.items());
		return items;
	}

	private static List<String> search(ClientGameTestContext context, String term) {
		return context.computeOnClient(client -> {
			try {
				CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) client.gui.screen();
				Field box = CreativeModeInventoryScreen.class.getDeclaredField("searchBox");
				box.setAccessible(true);
				((EditBox) box.get(screen)).setValue(term);
				Method refresh = CreativeModeInventoryScreen.class.getDeclaredMethod("refreshSearchResults");
				refresh.setAccessible(true);
				refresh.invoke(screen);
				return screen.getMenu().items.stream().map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).toList();
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
	}

	private static void creativeMenu(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("gamemode creative @a");
		server.runCommand("tp @a 30.5 " + g + " 10.5 0 0");
		context.waitTicks(10);
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		boolean open = context.computeOnClient(client -> client.gui.screen() instanceof CreativeModeInventoryScreen);
		assertTrue(open, "the creative inventory should open");
		context.runOnClient(client -> {
			try {
				Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
				select.setAccessible(true);
				select.invoke(client.gui.screen(), CreativeModeTabs.searchTab());
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
		context.waitTicks(2);
		context.getInput().typeChars("rail");
		context.waitTicks(5);
		List<String> rails = context.computeOnClient(client -> ((CreativeModeInventoryScreen) client.gui.screen()).getMenu().items.stream()
			.map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).toList());
		context.takeScreenshot("m06-creative-search-rail");
		Set<String> retiredIds = retired().stream().map(item -> BuiltInRegistries.ITEM.getKey(item).getPath()).collect(Collectors.toSet());
		List<String> found = new ArrayList<>();
		for (String term : List.of("shulker", "granite", "diorite", "andesite", "tuff", "elytra", "ender chest", "chest minecart",
			"furnace minecart", "powered", "detector", "activator")) {
			search(context, term).stream().filter(retiredIds::contains).forEach(id -> found.add(term + ": " + id));
		}
		List<String> tracks = search(context, "loading rail");
		List<String> shulker = search(context, "shulker");
		context.takeScreenshot("m06-creative-search-shulker");
		List<String> inTabs = context.computeOnClient(client -> CreativeModeTabs.allTabs().stream()
			.flatMap(tab -> tab.getDisplayItems().stream().map(ItemStack::getItem))
			.filter(item -> retired().contains(item) || item instanceof net.minecraft.world.item.BlockItem block && block.getBlock() instanceof ShulkerBoxBlock)
			.map(item -> BuiltInRegistries.ITEM.getKey(item).getPath()).distinct().toList());
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		server.runCommand("gamemode survival @a");
		SimpleBlockTestLog.info("CLIENT CREATIVE: search 'rail': {}; 'loading rail': {}; 'shulker': {}; retired items found by any search: {}; in any tab: {}",
			rails, tracks, shulker, found, inTabs);
		assertTrue(rails.contains("rail") && rails.contains("loading_track") && tracks.contains("loading_track"),
			"'rail' should find rails and the Loading Rail, 'loading rail' the Loading Rail: " + rails + ", " + tracks);
		assertTrue(rails.stream().noneMatch(retiredIds::contains), "no retired rail: " + rails);
		assertTrue(found.isEmpty() && inTabs.isEmpty(), "retired items in the creative menu: search " + found + ", tabs " + inTabs);
	}

	// ---- A long line (section 7) ----

	/** What must survive: the line, its cart, the winch's coal and partly burnt coal. */
	private record LongLine(int anchors, boolean looped, int coal, int burn, double cartX, String stop) {
	}

	private static LongLine longLine(ClientGameTestContext context, TestSingleplayerContext world, int g) {
		TestServerContext server = world.getServer();
		BlockPos drumPos = new BlockPos(0, g, LONG_Z);
		// Build with the chunks force-loaded (commands only reach loaded chunks), then let them go.
		server.runCommand("forceload add -16 " + (LONG_Z - 8) + " " + (LONG_END + 8) + " " + (LONG_Z + 8));
		context.waitTicks(40);
		server.runCommand("fill -2 " + (g - 1) + " " + (LONG_Z - 2) + " " + (LONG_END + 2) + " " + (g - 1) + " " + (LONG_Z + 2) + " minecraft:stone");
		server.runCommand("fill -2 " + g + " " + (LONG_Z - 2) + " " + (LONG_END + 2) + " " + (g + 5) + " " + (LONG_Z + 2) + " minecraft:air");
		server.runCommand("fill 2 " + g + " " + LONG_Z + " " + LONG_END + " " + g + " " + LONG_Z + " minecraft:rail[shape=east_west]");
		List<Integer> anchors = new ArrayList<>();
		for (int x = 6; x <= LONG_END - 4; x += 10) {
			anchors.add(x);
			server.runCommand("setblock " + x + " " + (g + 3) + " " + LONG_Z + " minecraft:oak_planks");
		}
		server.runCommand("setblock 0 " + g + " " + LONG_Z + " simple_block:cable_drum[facing=east]");
		server.runCommand("tp @a 2.5 " + g + " " + (LONG_Z + 0.5) + " 90 30");
		world.getConnection().waitForChunksRender();
		hold(server, context, "simple_block:cable_coil");
		lookAtPoint(context, new Vec3(0.5, g + 0.85, LONG_Z + 0.5));
		use(context);
		hold(server, context, "simple_block:cable_anchor " + (anchors.size() + 1));
		for (int x : anchors) {
			server.runCommand("tp @a " + (x - 1.5) + " " + g + " " + (LONG_Z + 0.5) + " -90 -45");
			context.waitTicks(4);
			lookAtPoint(context, new Vec3(x + 0.5, g + 3, LONG_Z + 0.5));
			use(context);
		}
		server.runCommand("clear @a");
		server.runCommand("tp @a 3.5 " + g + " " + (LONG_Z + 0.5) + " 90 20");
		context.waitTicks(5);
		lookAtHook(context);
		use(context);
		for (int x : anchors) {
			server.runCommand("tp @a " + (x - 2.5) + " " + g + " " + (LONG_Z + 0.5) + " -90 -30");
			context.waitTicks(4);
			context.getInput().lookAt(new BlockPos(x, g + 2, LONG_Z));
			use(context);
		}
		use(context);
		int last = anchors.getLast();
		hold(server, context, "simple_block:cable_anchor");
		context.getInput().lookAt(new BlockPos(last, g + 2, LONG_Z));
		use(context);
		int routed = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route().size());
		int laid = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).laidOut());
		boolean looped = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).looped());
		assertTrue(routed == anchors.size() && looped, "long line: " + routed + " of " + anchors.size() + " anchors, looped " + looped);

		// The winch and a rod cart near the drum.
		server.runCommand("clear @a");
		server.runCommand("tp @a 0.5 " + g + " " + (LONG_Z - 2.5) + " 0 40");
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(0.5, g + 0.85, LONG_Z + 0.5));
		use(context);
		hold(server, context, "minecraft:coal 2");
		use(context);
		use(context);
		server.runCommand("tp @a 10.5 " + g + " " + (LONG_Z - 2.5) + " 0 30");
		hold(server, context, "minecraft:minecart");
		lookAtPoint(context, new Vec3(10.5, g + 0.1, LONG_Z + 0.5));
		use(context);
		hold(server, context, "simple_block:copper_connecting_rod");
		lookAtPoint(context, new Vec3(10.5, g + 0.4, LONG_Z + 0.5));
		use(context);
		server.runCommand("clear @a");

		// Natural chunk loading from here on: the player at the drum, simulation distance 5.
		server.runCommand("forceload remove all");
		server.runCommand("tp @a 0.5 " + g + " " + (LONG_Z + 2.5) + " 180 30");
		context.waitTicks(60);
		boolean farTicking = server.computeOnServer(s -> s.overworld().isPositionEntityTicking(new BlockPos(LONG_END - 10, g, LONG_Z)));
		assertTrue(!farTicking, "the far end of the line should not be ticking");
		lookAtPoint(context, new Vec3(0.5, g + 9.5 / 16.0, LONG_Z + 0.5 + 0.55));
		context.getInput().pressKey(options -> options.keyAttack);
		context.waitTicks(2);
		CableLines.Lever lever = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		assertTrue(lever == CableLines.Lever.FEED, "lever on the long line: " + lever);
		int waited = 0;
		while (waited < 1200 && server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever()) != CableLines.Lever.NEUTRAL) {
			context.waitTicks(10);
			waited += 10;
		}
		LongLine stopped = longState(server, g);
		int ticking = server.computeOnServer(s -> {
			int edge = 0;
			while (edge < LONG_END && s.overworld().isPositionEntityTicking(new BlockPos(edge, g, LONG_Z))) {
				edge++;
			}
			return edge;
		});
		server.runCommand("tp @a " + (stopped.cartX() - 4.0) + " " + (g + 2) + " " + (LONG_Z + 4.0) + " 0 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(stopped.cartX() + 3.0, g + 1.0, LONG_Z + 0.5));
		context.waitTicks(20);
		context.takeScreenshot("m06-long-line-stopped-at-the-edge");
		SimpleBlockTestLog.info("CLIENT LONG LINE: {} anchors routed by hand, {} blocks laid out from one coil, looped; FEED with the far end unloaded: "
			+ "stopped after {} ticks at x {} ({}); entity-ticking world ends at x {}; coal {}, burn {}", routed, laid, waited, fmt(stopped.cartX()),
			stopped.stop(), ticking, stopped.coal(), stopped.burn());
		assertTrue("unloaded".equals(stopped.stop()) && stopped.cartX() > 40.0 && stopped.cartX() < ticking + 2.0,
			"the cart should be hauled to the edge of the ticking world and stop there: " + stopped + ", edge x " + ticking);

		// Far away: the drum and the cart unload; the line stays.
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 1500 " + (g + 40) + " " + LONG_Z);
		int away = 0;
		while (away < 3000 && server.computeOnServer(s -> s.overworld().isLoaded(drumPos))) {
			context.waitTicks(20);
			away += 20;
		}
		boolean lineKept = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos) != null);
		assertTrue(!server.computeOnServer(s -> s.overworld().isLoaded(drumPos)) && lineKept, "drum unloaded after " + away + " ticks, line kept " + lineKept);
		server.runCommand("tp @a 0.5 " + g + " " + (LONG_Z + 2.5) + " 180 30");
		server.runCommand("gamemode survival @a");
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		LongLine back = longState(server, g);
		SimpleBlockTestLog.info("CLIENT LONG LINE: away {} ticks (drum unloaded, line kept); back: {}", away, back);
		assertTrue(same(stopped, back), "after unloading: before " + stopped + ", after " + back);
		return back;
	}

	private static LongLine longState(TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(0, g, LONG_Z);
		return server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			CableLines.Line line = CableLines.line(level, drumPos);
			CableDrumBlockEntity drum = drumOf(level, drumPos);
			AbstractMinecart cart = level.getEntitiesOfClass(Minecart.class, new AABB(-2, g - 1, LONG_Z - 1, LONG_END + 2, g + 4, LONG_Z + 1)).stream()
				.findFirst().orElse(null);
			return new LongLine(drum.route().size(), line.looped(), line.coal(), line.burn(), cart == null ? Double.NaN : cart.getX(),
				line.lastStop() == null ? "" : line.lastStop());
		});
	}

	private static boolean same(LongLine a, LongLine b) {
		return a.anchors() == b.anchors() && a.looped() == b.looped() && a.coal() == b.coal() && a.burn() == b.burn()
			&& Math.abs(a.cartX() - b.cartX()) < 0.05;
	}

	private static void afterReopen(ClientGameTestContext context, TestSingleplayerContext world, int g, LongLine before) {
		TestServerContext server = world.getServer();
		BlockPos drumPos = new BlockPos(0, g, LONG_Z);
		context.waitTicks(20);
		LongLine after = longState(server, g);
		CableLines.Lever lever = server.computeOnServer(s -> CableLines.line(s.overworld(), drumPos).lever());
		context.takeScreenshot("m06-long-line-after-reopen");
		SimpleBlockTestLog.info("CLIENT REOPEN: before {}; after saving and reopening {}, lever {}", before, after, lever);
		assertTrue(same(before, after) && lever == CableLines.Lever.NEUTRAL, "after reopening: before " + before + ", after " + after + ", lever " + lever);
		// PULL brings the cart back towards the drum.
		server.runCommand("tp @a 0.5 " + g + " " + (LONG_Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(0.5, g + 9.5 / 16.0, LONG_Z + 0.5 + 0.55));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(100);
		LongLine pulled = longState(server, g);
		SimpleBlockTestLog.info("CLIENT REOPEN: PULL 100 ticks: cart x {} -> {}, burn {} -> {}", fmt(after.cartX()), fmt(pulled.cartX()), after.burn(),
			pulled.burn());
		assertTrue(after.cartX() - pulled.cartX() > 5.0 && pulled.burn() < after.burn(), "PULL after reopening: " + after + " -> " + pulled);
	}

	// ---- Helpers ----

	private static AbstractMinecart cartOn(ServerLevel level, int g, int z) {
		return level.getEntitiesOfClass(AbstractMinecart.class, new AABB(18, g - 1, z - 0.6, 52, g + 4, z + 1.6)).stream()
			.filter(cart -> !(cart instanceof MinecartFurnace)).findFirst().orElse(null);
	}

	private static List<AbstractMinecart> carts(ServerLevel level, int g, int z) {
		return level.getEntitiesOfClass(AbstractMinecart.class, new AABB(18, g - 1, z - 0.6, 52, g + 4, z + 1.6));
	}

	private static MinecartFurnace engineOn(ServerLevel level, int g) {
		return level.getEntitiesOfClass(MinecartFurnace.class, new AABB(18, g - 1, TRACK_Z - 0.6, 52, g + 4, TRACK_Z + 1.6)).getFirst();
	}

	private static String fmt(double value) {
		return String.format("%.2f", value);
	}
}
