package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.drumOf;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtHook;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.Couplings;

/**
 * M05 in the real client, in a fresh survival world, every action a simulated key press:
 * <ul>
 *   <li><b>Cable loop:</b> wind a drum, hang anchors, route the cable, close the loop with an anchor,
 *       fit a rod to a minecart; then hold the use key on the drum's crank to haul the cart out, let go
 *       (it holds), and hold it while sneaking to bring it back. A drum is also hung under a plank.</li>
 *   <li><b>Cobblestone Engine:</b> place a minecart and use a furnace on it, fuel it with coal, couple a
 *       cushioned cart to it with a chain, start and stop it from the ground with an empty hand, then
 *       get in the cushioned cart and drive with the forward and back keys.</li>
 * </ul>
 * Then the player goes far enough away for the yard's chunks to unload and comes back, and the world
 * is saved and reopened: the loop, the held rod cart and the coupled train must be exactly as they were,
 * with nothing duplicated, and the crank must still haul. Screenshots go to
 * {@code build/run/clientGameTest/screenshots} ({@code loop-*}, {@code engine-*}).
 *
 * <p>The yard, at ground level {@code g}: stone x 18..52, z -4..14. The cable line runs along z 6: the
 * drum at x 20 facing east, rails x 22..50, anchors under planks at x 28, 36 and 44. The engine's track
 * runs along z 0, x 22..50.
 */
public class HaulageClientTest implements FabricClientGameTest {
	private static final int Z = 6;
	private static final int TRACK_Z = 0;
	private static final int[] ANCHOR_X = {28, 36, 44};

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		int g;
		Snapshot before;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			TestServerContext server = world.getServer();
			g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 35, Z));
			buildYard(server, g);
			server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
			world.getConnection().waitForChunksRender();

			loop(context, server, g);
			crank(context, server, g);
			hangingDrum(context, server, g);
			engine(context, server, g);
			drive(context, server, g);
			before = unloadAndReturn(context, world, g);
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			afterReopen(context, world.getServer(), g, before);
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
		for (int x : ANCHOR_X) {
			server.runCommand("setblock " + x + " " + (g + 3) + " " + Z + " minecraft:oak_planks");
		}
		server.runCommand("setblock 20 " + g + " " + Z + " simple_block:cable_drum[facing=east]");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ---- Cable loop ----

	/** Wind two coils, hang the anchors, route the cable and close the loop with an anchor on the last one. */
	private static void loop(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_coil 2");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 0.85, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_anchor 3");
		for (int x : ANCHOR_X) {
			server.runCommand("tp @a " + (x - 1.5) + " " + g + " " + (Z + 0.5) + " -90 -45");
			context.waitTicks(5);
			lookAtPoint(context, new Vec3(x + 0.5, g + 3, Z + 0.5));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
		}
		server.runCommand("clear @a");
		server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
		context.waitTicks(5);
		lookAtHook(context);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		for (int x : ANCHOR_X) {
			server.runCommand("tp @a " + (x - 2.5) + " " + g + " " + (Z + 0.5) + " -90 -30");
			context.waitTicks(5);
			context.getInput().lookAt(new BlockPos(x, g + 2, Z));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
		}
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		int routed = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route().size());
		assertTrue(routed == 3, "route before closing: " + routed + " anchors");

		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_anchor");
		context.waitTicks(5);
		context.getInput().lookAt(new BlockPos(44, g + 2, Z));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String closeMessage = overlayMessage(context);
		boolean looped = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).looped());
		int returnLength = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).returnLength());
		boolean clientLooped = context.computeOnClient(client -> client.level.getBlockEntity(drumPos) instanceof CableDrumBlockEntity drum && drum.looped());
		server.runCommand("tp @a 46.5 " + (g + 2) + " 9.5 135 15");
		context.waitTicks(10);
		context.takeScreenshot("loop-return-wheel");
		server.runCommand("tp @a 32.5 " + (g + 1) + " 0.5 0 5");
		context.waitTicks(10);
		context.takeScreenshot("loop-closed");
		SimpleBlockTestLog.info("CLIENT LOOP: anchor used on the last anchor: '{}'; looped {} (client {}), {} blocks of return cable", closeMessage,
			looped, clientLooped, returnLength);
		assertTrue(looped && clientLooped && returnLength > 0, "loop: server " + looped + ", client " + clientLooped + ", return " + returnLength);
	}

	/** Fit a rod to a minecart under the line, then haul it out and back by holding the use key on the crank. */
	private static void crank(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("tp @a 25.5 " + g + " 3.5 0 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:minecart");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(25.5, g + 0.1, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:copper_connecting_rod");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(25.5, g + 0.4, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		boolean gripping = server.computeOnServer(s -> cartAt(s.overworld(), g, Z) instanceof Minecart cart && ConnectingRods.isGripping(cart));
		assertTrue(gripping, "the rod cart under the loop should grip the working strand");
		server.runCommand("clear @a");

		// The crank is on the drum's south side (facing east): stand south of it and look at the handle's hub.
		server.runCommand("tp @a 20.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		double start = server.computeOnServer(s -> cartAt(s.overworld(), g, Z).getX());
		float travelBefore = context.computeOnClient(client -> ((CableDrumBlockEntity) client.level.getBlockEntity(drumPos)).travel);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(60);
		float clientSpeed = context.computeOnClient(client -> ((CableDrumBlockEntity) client.level.getBlockEntity(drumPos)).speed());
		float travelDuring = context.computeOnClient(client -> ((CableDrumBlockEntity) client.level.getBlockEntity(drumPos)).travel);
		context.takeScreenshot("loop-cranking");
		context.waitTicks(100);
		context.getInput().releaseKey(options -> options.keyUse);
		context.waitTicks(10);
		double out = server.computeOnServer(s -> cartAt(s.overworld(), g, Z).getX());
		context.waitTicks(30);
		double held = server.computeOnServer(s -> cartAt(s.overworld(), g, Z).getX());
		server.runCommand("tp @a " + (out - 2.0) + " " + (g + 1) + " 3.0 -40 10");
		context.waitTicks(5);
		context.takeScreenshot("loop-cart-hauled");

		server.runCommand("tp @a 20.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(120);
		context.getInput().releaseKey(options -> options.keyUse);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(10);
		double back = server.computeOnServer(s -> cartAt(s.overworld(), g, Z).getX());
		boolean stillLooped = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).looped());
		int routed = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route().size());
		SimpleBlockTestLog.info("CLIENT CRANK: held use on the crank 160 ticks: cart x {} -> {} (client spool speed {}, travel {} -> {}); let go: x {}; "
			+ "sneak-held 120 ticks: back to x {}; still looped {}, route {} anchors", fmt(start), fmt(out), clientSpeed, travelBefore, travelDuring,
			fmt(held), fmt(back), stillLooped, routed);
		assertTrue(out - start > 4.0, "holding use on the crank should haul the cart out: x " + start + " -> " + out);
		assertTrue(clientSpeed > 0.0F && travelDuring > travelBefore, "the client's spool should turn while cranked: speed " + clientSpeed);
		assertTrue(Math.abs(held - out) < 0.05, "let go, the cart should hold: " + out + " -> " + held);
		assertTrue(out - back > 4.0, "sneak-holding use on the crank should bring it back: x " + out + " -> " + back);
		assertTrue(stillLooped && routed == 3, "cranking should not open or dismantle the loop");
	}

	/** CRANK-02 in the client: a drum placed against a plank's underside hangs from it. */
	private static void hangingDrum(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("setblock 24 " + (g + 3) + " 10 minecraft:oak_planks");
		server.runCommand("tp @a 22.5 " + g + " 10.5 -90 -40");
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_drum");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(24.5, g + 3, 10.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String state = server.computeOnServer(s -> s.overworld().getBlockState(new BlockPos(24, g + 2, 10)).toString());
		server.runCommand("tp @a 21.5 " + (g + 1) + " 12.5 -125 10");
		context.waitTicks(10);
		context.takeScreenshot("loop-hanging-drum");
		SimpleBlockTestLog.info("CLIENT CRANK: drum placed against a plank's underside: {}", state);
		assertTrue(state.contains("simple_block:cable_drum") && state.contains("hanging=true"), "hanging drum: " + state);
		server.runCommand("clear @a");
	}

	// ---- Cobblestone Engine ----

	/** Place a minecart, make it an engine with a furnace, fuel it, couple a cushioned cart, start and stop it from the ground. */
	private static void engine(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("tp @a 30.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:minecart");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(30.5, g + 0.1, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:furnace");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(30.5, g + 0.35, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		boolean converted = server.computeOnServer(s -> cartAt(s.overworld(), g, TRACK_Z) instanceof MinecartFurnace);
		boolean clientEngine = context.computeOnClient(client -> !client.level.getEntitiesOfClass(MinecartFurnace.class,
			new AABB(18, g - 1, TRACK_Z - 2, 52, g + 4, TRACK_Z + 2)).isEmpty());
		server.runCommand("tp @a 28.0 " + (g + 1) + " " + (TRACK_Z + 3.0) + " -145 25");
		context.waitTicks(10);
		context.takeScreenshot("engine-converted");
		assertTrue(converted && clientEngine, "a furnace used on the minecart should make an engine: server " + converted + ", client " + clientEngine);

		server.runCommand("tp @a 30.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:coal");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(30.5, g + 0.35, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		int fuel = server.computeOnServer(s -> engineOf(s.overworld(), g).map(e -> CobblestoneEngine.engine(e).simpleBlock$fuel()).orElse(-1));
		assertTrue(fuel == CobblestoneEngine.FUEL_PER_ITEM, "fuel after a coal: " + fuel);

		// The seat: a second minecart, two blocks east, with a cushion.
		server.runCommand("tp @a 32.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:minecart");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(32.5, g + 0.1, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:red_cushion");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(32.5, g + 0.35, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);

		// Couple them: a chain on the engine, then on the seat.
		server.runCommand("tp @a 31.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:iron_chain 2");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(30.6, g + 0.35, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(32.4, g + 0.35, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(20);
		boolean coupled = server.computeOnServer(s -> seatOf(s.overworld(), g) != null && Couplings.hasCouplings(seatOf(s.overworld(), g)));
		int chainsLeft = context.computeOnClient(client -> client.player.getMainHandItem().getCount());
		boolean clientChain = context.computeOnClient(client -> client.level.getEntitiesOfClass(MinecartFurnace.class,
			new AABB(18, g - 1, TRACK_Z - 2, 52, g + 4, TRACK_Z + 2)).stream().anyMatch(e -> Couplings.coupled(e).simpleBlock$couplingId(0) >= 0
				|| Couplings.coupled(e).simpleBlock$couplingId(1) >= 0));
		server.runCommand("tp @a 31.5 " + (g + 1) + " " + (TRACK_Z + 2.2) + " 180 35");
		context.waitTicks(10);
		context.takeScreenshot("engine-chain-coupling");
		server.runCommand("tp @a 27.0 " + (g + 1.5) + " " + (TRACK_Z - 2.5) + " -60 20");
		context.waitTicks(10);
		context.takeScreenshot("engine-train-side");
		assertTrue(coupled && chainsLeft == 1 && clientChain, "coupled " + coupled + ", chains left " + chainsLeft + ", client sees the coupling " + clientChain);
		server.runCommand("clear @a");

		// From the ground: an empty-hand use starts it away from the player (west of it: east); another stops it.
		server.runCommand("tp @a 28.5 " + g + " " + (TRACK_Z + 1.5) + " -90 20");
		context.waitTicks(5);
		double before = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		lookAtPoint(context, new Vec3(30.5, g + 0.35, TRACK_Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(15);
		boolean running = server.computeOnServer(s -> CobblestoneEngine.engine(engineOf(s.overworld(), g).orElseThrow()).simpleBlock$running());
		double moving = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		Vec3 engineNow = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getBoundingBox().getCenter());
		lookAtPoint(context, engineNow);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(30);
		double stopped = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		double speed = server.computeOnServer(s -> CobblestoneEngine.engine(engineOf(s.overworld(), g).orElseThrow()).simpleBlock$speed());
		boolean smoke = server.computeOnServer(s -> CobblestoneEngine.engine(engineOf(s.overworld(), g).orElseThrow()).simpleBlock$running());
		SimpleBlockTestLog.info("CLIENT ENGINE: furnace on a minecart: engine (client sees it {}); coal: {} ticks of fuel; chain: coupled {} ({} chain "
			+ "left, client sees it {}); empty-hand use from the west: running {}, x {} -> {} in 15 ticks; used again: stopped at x {} (speed {}, "
			+ "running {})", clientEngine, fuel, coupled, chainsLeft, clientChain, running, fmt(before), fmt(moving), fmt(stopped), speed, smoke);
		assertTrue(running && moving > before + 0.1, "started from the west, it should run east: running " + running + ", x " + before + " -> " + moving);
		assertTrue(speed == 0.0 && !smoke, "used again, it should stop: speed " + speed + ", running " + smoke);
	}

	/** Get into the cushioned cart and drive the engine with the forward and back keys, looking along the track. */
	private static void drive(ClientGameTestContext context, TestServerContext server, int g) {
		Vec3 seat = server.computeOnServer(s -> seatOf(s.overworld(), g).getBoundingBox().getCenter());
		server.runCommand("tp @a " + seat.x + " " + g + " " + (TRACK_Z + 2.0) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, seat);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		boolean riding = context.computeOnClient(client -> client.player.getVehicle() instanceof Minecart cart && MinecartCushions.isFitted(cart));
		assertTrue(riding, "the player should be sitting in the cushioned cart");

		// Look west, along the track past the engine, and hold forward: the train runs west, the engine leading.
		context.getInput().lookAt(90.0F, 15.0F);
		context.waitTicks(2);
		double start = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(40);
		context.takeScreenshot("engine-driving-from-the-seat");
		double moving = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		context.getInput().releaseKey(options -> options.keyUp);
		context.getInput().holdKey(options -> options.keyDown);
		context.waitTicks(25);
		context.getInput().releaseKey(options -> options.keyDown);
		double speed = server.computeOnServer(s -> CobblestoneEngine.engine(engineOf(s.overworld(), g).orElseThrow()).simpleBlock$speed());
		double stopped = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		context.waitTicks(20);
		double held = server.computeOnServer(s -> engineOf(s.overworld(), g).orElseThrow().getX());
		SimpleBlockTestLog.info("CLIENT ENGINE: in the cushioned cart, forward looking west 40 ticks: engine x {} -> {}; back 25 ticks: speed {}, "
			+ "x {}; 20 ticks later x {}", fmt(start), fmt(moving), speed, fmt(stopped), fmt(held));
		assertTrue(start - moving > 1.5, "forward looking west should drive the train west: x " + start + " -> " + moving);
		assertTrue(speed == 0.0 && Math.abs(held - stopped) < 0.01, "back should stop it and it should hold: speed " + speed + ", " + stopped + " -> " + held);
		context.getInput().pressKey(options -> options.keyShift);
		context.waitTicks(5);
	}

	// ---- Unloading and reopening ----

	/** What must survive the yard's chunks unloading, or the world being saved and reopened. */
	private record Snapshot(boolean looped, int returnLength, int stored, int carts, int engines, double rodCartX, boolean gripping, double engineX,
		double seatX, int fuel, boolean coupled) {
		boolean matches(Snapshot other) {
			return this.looped == other.looped && this.returnLength == other.returnLength && this.stored == other.stored && this.carts == other.carts
				&& this.engines == other.engines && Math.abs(this.rodCartX - other.rodCartX) < 0.05 && this.gripping == other.gripping
				&& Math.abs(this.engineX - other.engineX) < 0.05 && Math.abs(this.seatX - other.seatX) < 0.05 && this.fuel == other.fuel
				&& this.coupled == other.coupled;
		}
	}

	private static Snapshot snapshot(TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		return server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			CableDrumBlockEntity drum = drumOf(level, drumPos);
			AABB yard = new AABB(18, g - 1, -4, 52, g + 4, 14);
			Minecart rodCart = (Minecart) cartAt(level, g, Z);
			MinecartFurnace engine = engineOf(level, g).orElseThrow();
			Minecart seat = seatOf(level, g);
			return new Snapshot(drum.looped(), drum.returnLength(), drum.stored(), level.getEntitiesOfClass(Minecart.class, yard).size(),
				level.getEntitiesOfClass(MinecartFurnace.class, yard).size(), rodCart.getX(), ConnectingRods.isGripping(rodCart), engine.getX(),
				seat.getX(), CobblestoneEngine.engine(engine).simpleBlock$fuel(), Couplings.hasCouplings(seat) && Couplings.hasCouplings(engine));
		});
	}

	/** Goes far enough away for the yard's chunks to unload, comes back, and checks the loop and the train are as they were. */
	private static Snapshot unloadAndReturn(ClientGameTestContext context, TestSingleplayerContext world, int g) {
		TestServerContext server = world.getServer();
		BlockPos drumPos = new BlockPos(20, g, Z);
		Snapshot before = snapshot(server, g);
		UUID[] ids = server.computeOnServer(s -> new UUID[] {engineOf(s.overworld(), g).orElseThrow().getUUID(), cartAt(s.overworld(), g, Z).getUUID()});
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a 4000 " + (g + 40) + " 4000");
		int waited = 0;
		boolean unloaded = false;
		while (waited < 3000 && !unloaded) {
			context.waitTicks(20);
			waited += 20;
			// The chunks gone, and the engine and the rod cart saved and taken out of the level.
			unloaded = server.computeOnServer(s -> !s.overworld().isLoaded(drumPos) && !s.overworld().isLoaded(new BlockPos(30, g, TRACK_Z))
				&& s.overworld().getEntity(ids[0]) == null && s.overworld().getEntity(ids[1]) == null);
		}
		assertTrue(unloaded, "the yard's chunks did not unload in " + waited + " ticks");
		server.runCommand("tp @a 35.5 " + (g + 2) + " 10.5 160 20");
		server.runCommand("gamemode survival @a");
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		Snapshot after = snapshot(server, g);
		context.takeScreenshot("loop-and-train-after-unload");
		SimpleBlockTestLog.info("CLIENT UNLOAD: yard chunks unloaded after {} ticks away; before {}; after {}", waited, before, after);
		assertTrue(before.matches(after), "after the chunks unloaded and loaded again: before " + before + ", after " + after);
		return after;
	}

	/** After saving and reopening the world: the same loop and train, and the crank still hauls. */
	private static void afterReopen(ClientGameTestContext context, TestServerContext server, int g, Snapshot before) {
		context.waitTicks(20);
		Snapshot after = snapshot(server, g);
		context.takeScreenshot("loop-and-train-after-reopen");
		assertTrue(before.matches(after), "after saving and reopening: before " + before + ", after " + after);
		server.runCommand("tp @a 20.5 " + g + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g + 9.5 / 16.0, Z + 0.5 + 0.55));
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(60);
		context.getInput().releaseKey(options -> options.keyUse);
		context.waitTicks(10);
		double hauled = server.computeOnServer(s -> cartAt(s.overworld(), g, Z).getX());
		SimpleBlockTestLog.info("CLIENT REOPEN: after save and reopen {}; cranked 60 ticks: rod cart x {} -> {}", after, fmt(before.rodCartX()),
			fmt(hauled));
		assertTrue(hauled - before.rodCartX() > 1.5, "the crank should still haul after reopening: x " + before.rodCartX() + " -> " + hauled);
	}

	// ---- Helpers ----

	private static AbstractMinecart cartAt(ServerLevel level, int g, int z) {
		return level.getEntitiesOfClass(AbstractMinecart.class, new AABB(18, g - 1, z - 1, 52, g + 4, z + 1)).stream().findFirst().orElse(null);
	}

	private static Optional<MinecartFurnace> engineOf(ServerLevel level, int g) {
		return level.getEntitiesOfClass(MinecartFurnace.class, new AABB(18, g - 1, TRACK_Z - 1, 52, g + 4, TRACK_Z + 1)).stream().findFirst();
	}

	private static Minecart seatOf(ServerLevel level, int g) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(18, g - 1, TRACK_Z - 1, 52, g + 4, TRACK_Z + 1)).stream().findFirst().orElse(null);
	}

	private static String fmt(double value) {
		return String.format("%.2f", value);
	}
}
