package io.github.boxofdog.simpleblock.gametest.client;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.gametest.mixin.HudAccessor;

/**
 * M04 in the real client, in a fresh survival world: place a cable drum, wind a coil on, hang anchors
 * under planks, pick up the cable end and run it through the anchors, fit a connecting rod to a
 * minecart and roll it under the cable, then (after saving and reopening the world) disconnect and
 * dismantle the route. Every action is a simulated key press; screenshots go to
 * {@code build/run/clientGameTest/screenshots} ({@code cable-*}, {@code minecart-rod-*}).
 *
 * <p>The line, at ground level {@code g}: a stone yard x 18..52, z -2..14; a powered track along z 6
 * from x 22 to 50; the drum at x 20 facing east; planks at y g+3 over x 28, 36 and 44 with anchors
 * under them, so the cable runs 2.75 blocks above the rails.
 */
public class CableClientTest implements FabricClientGameTest {
	private static final int Z = 6;
	private static final int[] ANCHOR_X = {28, 36, 44};

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		int g;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			TestServerContext server = world.getServer();
			g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 35, Z));
			buildYard(server, g);
			server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
			world.getConnection().waitForChunksRender();

			drum(context, server, g);
			drumCloseUps(context, server, g);
			anchors(context, server, g);
			route(context, server, g);
			rod(context, server, g);
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			afterReload(context, world.getServer(), g);
			disconnectAndDismantle(context, world.getServer(), g);
		}
	}

	private static void buildYard(TestServerContext server, int g) {
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @a");
		server.runCommand("fill 18 " + (g - 1) + " -2 52 " + (g - 1) + " 14 minecraft:stone");
		server.runCommand("fill 18 " + g + " -2 52 " + (g + 8) + " 14 minecraft:air");
		server.runCommand("fill 22 " + (g - 1) + " " + Z + " 50 " + (g - 1) + " " + Z + " minecraft:redstone_block");
		server.runCommand("fill 22 " + g + " " + Z + " 50 " + g + " " + Z + " minecraft:powered_rail[powered=true,shape=east_west]");
		for (int x : ANCHOR_X) {
			server.runCommand("setblock " + x + " " + (g + 3) + " " + Z + " minecraft:oak_planks");
		}
		// Not wood: an anchor is refused under it.
		server.runCommand("setblock 32 " + (g + 3) + " 9 minecraft:stone");
		server.runCommand("kill @e[type=!minecraft:player]");
	}

	// ---- Steps ----

	/** CAB-03 in the client: place the drum, wind a coil on; the barrel shows cable and the hook hangs in front. */
	private static void drum(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_drum");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(20.5, g, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String placed = server.computeOnServer(s -> s.overworld().getBlockState(drumPos).toString());
		context.takeScreenshot("cable-drum-placed");
		assertTrue(placed.contains("simple_block:cable_drum") && placed.contains("facing=east") && placed.contains("wound=false"),
			"expected a bare drum facing east, found " + placed);

		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_coil 2");
		context.waitTicks(5);
		context.getInput().lookAt(drumPos);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		boolean coil = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).hasCoil());
		boolean wound = server.computeOnServer(s -> s.overworld().getBlockState(drumPos).getValue(CableDrumBlock.WOUND));
		int clientHooks = context.computeOnClient(client -> client.level.getEntitiesOfClass(CableHook.class, new AABB(drumPos).inflate(2.0)).size());
		String message = overlayMessage(context);
		context.takeScreenshot("cable-drum-wound-hook");
		SimpleBlockTestLog.info("CLIENT CABLE: drum placed: {}; coil fitted: {}, wound barrel {}, hooks seen by the client {}, message '{}'", placed,
			coil, wound, clientHooks, message);
		assertTrue(coil && wound, "drum after one coil: coil " + coil + ", wound " + wound);
		assertTrue(clientHooks == 1, "the client should see the loose end's hook, saw " + clientHooks);
		server.runCommand("clear @a");
	}

	/**
	 * BUG-001: close-ups of a wound drum's sides (the spool flanges seen along the axle and at angles),
	 * where the owner saw flickering stripes.
	 */
	private static void drumCloseUps(ClientGameTestContext context, TestServerContext server, int g) {
		Vec3 centre = new Vec3(20.5, g + 0.6, Z + 0.5);
		double[][] views = {{20.5, g + 0.9, Z - 1.6}, {21.9, g + 1.2, Z - 1.0}, {19.0, g + 1.4, Z + 2.1}, {20.5, g + 2.0, Z + 2.0}};
		for (int i = 0; i < views.length; i++) {
			server.runCommand("tp @a " + views[i][0] + " " + (views[i][1] - 1.62) + " " + views[i][2]);
			context.waitTicks(5);
			lookAtPoint(context, centre);
			context.waitTicks(5);
			context.takeScreenshot("bug001-drum-side-" + i);
		}
		server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
		context.waitTicks(5);
	}

	/** CAB-02 in the client: right-click the underside of the planks to hang anchors; under stone it is refused. */
	private static void anchors(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:cable_anchor 4");
		for (int x : ANCHOR_X) {
			server.runCommand("tp @a " + (x - 1.5) + " " + g + " " + (Z + 0.5) + " -90 -45");
			context.waitTicks(5);
			// The middle of the plank's underside.
			lookAtPoint(context, new Vec3(x + 0.5, g + 3, Z + 0.5));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
		}
		server.runCommand("tp @a 30.5 " + g + " 9.5 -90 -45");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(32.5, g + 3, 9.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		List<Boolean> hung = server.computeOnServer(s -> java.util.Arrays.stream(ANCHOR_X)
			.mapToObj(x -> s.overworld().getBlockState(new BlockPos(x, g + 2, Z)).is(CableContent.CABLE_ANCHOR)).toList());
		boolean underStone = server.computeOnServer(s -> s.overworld().getBlockState(new BlockPos(32, g + 2, 9)).is(CableContent.CABLE_ANCHOR));
		int left = context.computeOnClient(client -> client.player.getInventory().countItem(CableContent.CABLE_ANCHOR_ITEM));
		server.runCommand("tp @a 36.5 " + (g + 1) + " 1.5 0 -5");
		context.waitTicks(10);
		context.takeScreenshot("cable-anchors-hung");
		SimpleBlockTestLog.info("CLIENT CABLE: anchors under planks {}; under stone {}; anchors left in hand {}", hung, underStone, left);
		assertTrue(hung.equals(List.of(true, true, true)), "anchors under the planks: " + hung);
		assertTrue(!underStone && left == 1, "under stone: placed " + underStone + ", left in hand " + left);
		server.runCommand("clear @a");
	}

	/** CAB-04..07 in the client: pick up the end, run it through the three anchors, put it down on the last. */
	private static void route(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
		context.waitTicks(5);
		lookAtHook(context);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		boolean held = server.computeOnServer(s -> hookOf(s.overworld(), drumPos).isHeld());
		String pickedUp = overlayMessage(context);
		context.takeScreenshot("cable-end-in-hand");
		assertTrue(held, "the end should be in hand after using the hook; message '" + pickedUp + "'");

		for (int x : ANCHOR_X) {
			server.runCommand("tp @a " + (x - 2.5) + " " + g + " " + (Z + 0.5) + " -90 -30");
			context.waitTicks(5);
			context.getInput().lookAt(new BlockPos(x, g + 2, Z));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
		}
		String routedMessage = overlayMessage(context);
		context.takeScreenshot("cable-end-dragged-to-last-anchor");
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		List<BlockPos> route = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route());
		boolean coil = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).hasCoil());
		int laid = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).laidOut());
		boolean stillHeld = server.computeOnServer(s -> hookOf(s.overworld(), drumPos).isHeld());
		List<BlockPos> clientRoute = context.computeOnClient(client -> client.level.getBlockEntity(drumPos) instanceof CableDrumBlockEntity drum
			? drum.route() : List.of());
		server.runCommand("tp @a 32.5 " + (g + 1) + " -0.5 0 5");
		context.waitTicks(10);
		context.takeScreenshot("cable-route-laid");
		server.runCommand("tp @a 18.5 " + (g + 2) + " 9.5 -120 15");
		context.waitTicks(10);
		context.takeScreenshot("cable-route-from-the-drum");
		SimpleBlockTestLog.info("CLIENT CABLE: picked up ('{}'); routed through {} (client sees {}), {} blocks laid out, coil {}; last message '{}'; "
			+ "end held after using the last anchor again: {}", pickedUp, route, clientRoute, laid, coil, routedMessage, stillHeld);
		assertTrue(route.size() == 3 && clientRoute.equals(route), "route: server " + route + ", client " + clientRoute);
		assertTrue(coil && laid > 20, "one coil, " + laid + " blocks laid out");
		assertTrue(!stillHeld, "using the last anchor should put the end down");
	}

	/** CAB-14..16 in the client: fit a rod to a minecart, roll it under the cable: it grips between the anchors. */
	private static void rod(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos rail = new BlockPos(22, g, Z);
		server.runCommand("tp @a 22.5 " + g + " 3.5 0 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:minecart");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(rail.getX() + 0.5, g + 0.1, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:copper_connecting_rod");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(rail.getX() + 0.5, g + 0.4, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		boolean fitted = server.computeOnServer(s -> cartOf(s.overworld(), g) != null && ConnectingRods.hasRod(cartOf(s.overworld(), g)));
		String fitMessage = overlayMessage(context);
		context.takeScreenshot("minecart-rod-fitted");
		assertTrue(fitted, "the rod should be fitted; message '" + fitMessage + "'");
		server.runCommand("clear @a");

		// Send it down the line; freeze the game while it is between the first two anchors.
		server.runCommand("tp @a 32.5 " + (g + 1) + " 1.5 0 0");
		server.runCommand("data merge entity @e[type=minecraft:minecart,limit=1] {Motion:[0.2d,0.0d,0.0d]}");
		boolean frozen = false;
		for (int tick = 0; tick < 200 && !frozen; tick++) {
			context.waitTicks(1);
			double x = server.computeOnServer(s -> cartOf(s.overworld(), g).getX());
			if (x > 31.0 && x < 34.0) {
				server.runCommand("tick freeze");
				frozen = true;
			}
		}
		assertTrue(frozen, "the cart never got between the first two anchors");
		context.waitTicks(3);
		float reach = server.computeOnServer(s -> ConnectingRods.fitted(cartOf(s.overworld(), g)).simpleBlock$reach());
		float clientReach = context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, new AABB(18, g - 1, 0, 52, g + 4, 12)).stream()
			.map(cart -> ConnectingRods.fitted(cart).simpleBlock$reach()).findFirst().orElse(-1.0F));
		context.takeScreenshot("minecart-rod-gripping");
		double cartX = server.computeOnServer(s -> cartOf(s.overworld(), g).getX());
		server.runCommand("tp @a " + (cartX - 2.0) + " " + (g + 1) + " 4.0 -60 0");
		context.waitTicks(3);
		lookAtPoint(context, new Vec3(cartX, g + 1.6, Z + 0.5));
		context.waitTicks(3);
		context.takeScreenshot("minecart-rod-gripping-close");
		server.runCommand("tick unfreeze");
		boolean passedAll = false;
		boolean grippingPast = true;
		for (int tick = 0; tick < 200 && !passedAll; tick++) {
			context.waitTicks(1);
			double x = server.computeOnServer(s -> cartOf(s.overworld(), g).getX());
			if (x > 46.0) {
				passedAll = true;
				grippingPast = server.computeOnServer(s -> ConnectingRods.isGripping(cartOf(s.overworld(), g)));
			}
		}
		SimpleBlockTestLog.info("CLIENT ROD: fitted ('{}'); between the first two anchors the rod grips at {} (client sees {}); past the last anchor: {}, "
			+ "still gripping {}", fitMessage, reach, clientReach, passedAll, grippingPast);
		assertTrue(reach > 2.0F && Math.abs(clientReach - reach) < 1.0E-4F, "reach under the cable: server " + reach + ", client " + clientReach);
		assertTrue(passedAll && !grippingPast, "past the last anchor: arrived " + passedAll + ", gripping " + grippingPast);
		server.runCommand("kill @e[type=minecraft:minecart]");
		server.runCommand("kill @e[type=minecraft:item]");
	}

	/** CAB-13 in the client: after saving and reopening, the route is there and the loose end hangs at the last anchor again. */
	private static void afterReload(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("tp @a 40.5 " + (g + 1) + " 2.5 30 10");
		context.waitTicks(20);
		List<BlockPos> route = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route());
		Vec3 hook = server.computeOnServer(s -> hookOf(s.overworld(), drumPos).position());
		Vec3 clientHook = context.computeOnClient(client -> client.level.getEntitiesOfClass(CableHook.class, new AABB(drumPos).inflate(40.0)).stream()
			.map(CableHook::position).findFirst().orElse(Vec3.ZERO));
		context.takeScreenshot("cable-route-after-reload");
		SimpleBlockTestLog.info("CLIENT CABLE: after save and reopen: route {}, end hangs at {} (client {})", route, hook, clientHook);
		assertTrue(route.size() == 3, "route after reopening: " + route);
		assertTrue(hook.distanceTo(new Vec3(44.5, g + 1.75, Z + 0.5)) < 0.01 && clientHook.distanceTo(hook) < 0.01,
			"end after reopening: server " + hook + ", client " + clientHook);
	}

	/** CAB-09..11 in the client: sneak-use the middle anchor, then sneak-use the drum twice. */
	private static void disconnectAndDismantle(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(20, g, Z);
		server.runCommand("clear @a");
		server.runCommand("tp @a 33.5 " + g + " 6.5 -90 -30");
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		context.getInput().lookAt(new BlockPos(36, g + 2, Z));
		context.waitTicks(1);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		List<BlockPos> afterDisconnect = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route());
		String disconnectMessage = overlayMessage(context);
		server.runCommand("tp @a 32.5 " + (g + 1) + " -0.5 0 5");
		context.waitTicks(10);
		context.takeScreenshot("cable-disconnected-at-second-anchor");

		server.runCommand("tp @a 23.5 " + g + " 6.5 90 20");
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		// The upper half of the drum: the loose end lies at its foot.
		lookAtPoint(context, new Vec3(20.5, g + 0.85, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String rewindMessage = overlayMessage(context);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(5);
		String takeOffMessage = overlayMessage(context);
		boolean hasCable = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).hasCable());
		boolean wound = server.computeOnServer(s -> s.overworld().getBlockState(drumPos).getValue(CableDrumBlock.WOUND));
		int coils = context.computeOnClient(client -> client.player.getInventory().countItem(CableContent.CABLE_COIL));
		context.takeScreenshot("cable-dismantled");
		SimpleBlockTestLog.info("CLIENT CABLE: sneak-use second anchor: route {} ('{}'); sneak-use drum: '{}', then '{}'; drum has cable {}, wound {}, "
			+ "coils in inventory {}", afterDisconnect, disconnectMessage, rewindMessage, takeOffMessage, hasCable, wound, coils);
		assertTrue(afterDisconnect.size() == 1, "route after disconnecting at the second anchor: " + afterDisconnect);
		assertTrue(!hasCable && !wound && coils == 1, "after dismantling: cable " + hasCable + ", wound " + wound + ", coils " + coils);
	}

	// ---- Helpers ----

	static CableDrumBlockEntity drumOf(ServerLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum) {
			return drum;
		}
		throw new AssertionError("no cable drum at " + pos);
	}

	static CableHook hookOf(ServerLevel level, BlockPos drum) {
		return level.getEntitiesOfClass(CableHook.class, new AABB(drum).inflate(40.0), hook -> hook.drumPos().equals(drum)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no cable end for the drum at " + drum));
	}

	private static Minecart cartOf(ServerLevel level, int g) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(18, g - 1, 0, 60, g + 4, 12)).stream().findFirst().orElse(null);
	}

	/** Turns the camera to the middle of the (hanging) hook nearest the player. */
	static void lookAtHook(ClientGameTestContext context) {
		Vec3 target = context.computeOnClient(client -> client.level.getEntitiesOfClass(CableHook.class, client.player.getBoundingBox().inflate(8.0))
			.stream().map(hook -> hook.getBoundingBox().getCenter()).findFirst().orElseThrow(() -> new AssertionError("no hook in sight")));
		lookAtPoint(context, target);
	}

	/** Turns the camera so the crosshair is on {@code target} (a world position). */
	static void lookAtPoint(ClientGameTestContext context, Vec3 target) {
		float[] angles = context.computeOnClient(client -> {
			Vec3 d = target.subtract(client.player.getEyePosition());
			float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
			float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
			return new float[] {yaw, pitch};
		});
		context.getInput().lookAt(angles[0], angles[1]);
		context.waitTicks(1);
	}

	/** Clears the action bar (what an earlier step left there), so a later {@link #overlayMessage} shows only what came since. */
	static void clearOverlay(ClientGameTestContext context) {
		context.runOnClient(client -> ((HudAccessor) client.gui.hud).simple_block$setOverlayMessage(null));
	}

	static String overlayMessage(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			Component message = ((HudAccessor) client.gui.hud).simple_block$getOverlayMessage();
			return message == null ? "" : message.getString();
		});
	}

	static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
