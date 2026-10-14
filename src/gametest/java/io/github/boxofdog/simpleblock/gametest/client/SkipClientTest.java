package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.drumOf;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtHook;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.client.cable.CableDrumRenderer;
import io.github.boxofdog.simpleblock.client.mixin.AbstractMinecartRendererAccessor;
import io.github.boxofdog.simpleblock.client.skip.SkipScreen;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipCargo;
import io.github.boxofdog.simpleblock.skip.SkipMenu;
import io.github.boxofdog.simpleblock.skip.SkipStations;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M07 with the owner's playtest patch ({@code docs/MILESTONE_07_PLAYTEST_PATCH.md}), in a real client by
 * key presses and mouse clicks, in a shaft 36 blocks deep with no scaffolding:
 * <ul>
 *   <li><b>Placed:</b> the skip, placed by key on the shaft's floor, is its body only (1 x 3, no
 *       bridle); loaded by hand; its one-slot screen clicked with the mouse.</li>
 *   <li><b>The free hook:</b> the cable routed by hand at the top; its loose end put down under the head
 *       anchor, which stays an ordinary anchor; the coal winch's FEED lowers the hook all the way down
 *       the shaft onto the skip; the anchor is still ordinary.</li>
 *   <li><b>Hooking on, at the bottom:</b> the player at the foot of the shaft picks the hook up (the
 *       leash counts only distance across the ground) and uses it on the skip: the bridle appears (1 x 4)
 *       and the head anchor becomes a pulley.</li>
 *   <li><b>Loading:</b> PULL stops the skip with its bridle level with a docked cart; the cart's real
 *       front gate swings down into a ramp to the skip's intake (the lid swings open) and the ore goes
 *       in; then both close.</li>
 *   <li><b>Unloading:</b> FEED stops it with its chute's lip on the rim of a cart one level lower; the
 *       discharge gate slides open, the cart's front stays shut; screenshots from the side and the front,
 *       then with the skip facing each other way.</li>
 *   <li><b>Taking it off:</b> on the floor, sneak-use: the bridle goes, the anchor is an anchor again,
 *       the end hangs where the bridle's apex was.</li>
 *   <li><b>Unloaded chunks</b> (the hook wound while the player is far away: it does not move) and
 *       <b>save and reopen</b>.</li>
 * </ul>
 * Screenshots: {@code m07p-*}. The yard, at ground level {@code g}: stone floor x 196..228, z 0..14; the
 * shaft column x 212, z 6 (the skip faces north); the drum on a platform at (202, g + 33, 6), anchors under
 * planks at (207, g + 36, 6) and over the shaft at (212, g + 36, 6); Loading Rails facing south at
 * (212, g + 20, 5) (level with the bridle of a skip at g + 17) and (212, g, 5) (below the chute of a skip
 * at g + 11/16); a block to stand on at (212, g + 32, 9).
 */
public class SkipClientTest implements FabricClientGameTest {
	private static final int X = 212;
	private static final int Z = 6;
	private static final int TOP = 36;
	private static final int DRUM_Y = 33;
	private static final int LOAD_RAIL = 20;

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
			freeHook(context, server, g);
			hookOn(context, server, g);
			load(context, server, g);
			sideOn(context, server, g);
			unload(context, server, g);
			takeOff(context, server, g);
			unloaded(context, world, server, g);
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
		server.runCommand("gamerule minecraft:send_command_feedback false");
		server.runCommand("gamemode survival @a");
		server.runCommand("fill 196 " + (g - 1) + " 0 228 " + (g - 1) + " 14 minecraft:stone");
		server.runCommand("fill 196 " + g + " 0 228 " + (g + 40) + " 14 minecraft:air");
		server.runCommand("fill 198 " + (g + DRUM_Y - 1) + " 3 207 " + (g + DRUM_Y - 1) + " 9 minecraft:stone");
		server.runCommand("setblock 202 " + (g + DRUM_Y) + " " + Z + " simple_block:cable_drum[facing=east]");
		server.runCommand("setblock 207 " + (g + TOP + 1) + " " + Z + " minecraft:oak_planks");
		server.runCommand("setblock 207 " + (g + TOP) + " " + Z + " simple_block:cable_anchor");
		server.runCommand("setblock " + X + " " + (g + TOP + 1) + " " + Z + " minecraft:oak_planks");
		server.runCommand("setblock " + X + " " + (g + TOP) + " " + Z + " simple_block:cable_anchor");
		server.runCommand("setblock " + X + " " + (g + 32) + " 9 minecraft:stone");
		server.runCommand("setblock " + X + " " + g + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
		server.runCommand("setblock " + X + " " + (g + LOAD_RAIL - 1) + " " + (Z - 1) + " minecraft:stone");
		server.runCommand("setblock " + X + " " + (g + LOAD_RAIL) + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
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

	/** A camera position (spectator, so nothing falls or gets in the way), and a screenshot. */
	private static void shot(ClientGameTestContext context, TestServerContext server, String name, double x, double y, double z, float yaw, float pitch) {
		// Back where it was afterwards (left in survival mode in mid-air, the player would fall).
		String back = server.computeOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			return p.getX() + " " + p.getY() + " " + p.getZ() + " " + p.getYRot() + " " + p.getXRot();
		});
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch);
		context.waitTicks(4);
		context.takeScreenshot(name);
		server.runCommand("tp @a " + back);
		server.runCommand("gamemode survival @a");
	}

	/** Takes a cart away, empty (a broken or discarded cart spills its cargo, which the player would pick up). */
	private static void scrap(Minecart cart) {
		MinecartCargo.cart(cart).simpleBlock$setCargo(Items.AIR, 0);
		cart.discard();
	}

	private static OreSkip skip(ServerLevel level, int g) {
		return level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 40, Z + 3)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no skip in the shaft"));
	}

	private static double skipY(TestServerContext server, int g) {
		return server.computeOnServer(s -> skip(s.overworld(), g).getY() - g);
	}

	private static CableLines.Line line(ServerLevel level, int g) {
		return CableLines.line(level, new BlockPos(202, g + DRUM_Y, Z));
	}

	private static CableAnchorBlock.Return clientHead(ClientGameTestContext context, int g) {
		return context.computeOnClient(client -> client.level.getBlockState(new BlockPos(X, g + TOP, Z)).getValue(CableAnchorBlock.RETURN));
	}

	private static double clientSkipHeight(ClientGameTestContext context, int g) {
		return context.computeOnClient(client -> client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 40, Z + 3))
			.stream().findFirst().map(s -> s.getBoundingBox().getYsize()).orElse(-1.0));
	}

	/** The free hook's eye above the ground (server), or NaN when it is not in the loaded world. */
	private static double hookEye(TestServerContext server, int g) {
		return server.computeOnServer(s -> s.overworld().getEntitiesOfClass(CableHook.class, new AABB(X - 30, g - 2, Z - 30, X + 30, g + 45, Z + 30))
			.stream().findFirst().map(h -> h.getY() + h.getBbHeight() - g).orElse(Double.NaN));
	}

	// ---- Placing, loading, the screen ----

	private static void placeAndLoad(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " 2.5 0 30");
		hold(server, context, "simple_block:ore_skip");
		lookAtPoint(context, new Vec3(X + 0.5, g, Z + 0.5));
		use(context);
		String placed = server.computeOnServer(s -> skip(s.overworld(), g) + " facing " + skip(s.overworld(), g).facing());
		double height = clientSkipHeight(context, g);
		boolean bridled = server.computeOnServer(s -> skip(s.overworld(), g).bridled());
		shot(context, server, "m07p-skip-placed-no-bridle", X - 2.5, g + 1.5, Z - 3.5, -35, 15);
		assertTrue(placed.contains("facing north") && Math.abs(height - 3.0) < 1.0E-6 && !bridled, "placed by key: " + placed + ", client box height " + height
			+ ", bridle " + bridled);

		server.computeOnServer(s -> {
			skip(s.overworld(), g).setCargo(Items.RAW_IRON, SkipCargo.CAPACITY - 64);
			return null;
		});
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z - 2.0) + " 0 20");
		hold(server, context, "minecraft:raw_iron 64");
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.5));
		use(context);
		String loaded = overlayMessage(context);
		int afterLoad = server.computeOnServer(s -> skip(s.overworld(), g).count());
		server.runCommand("clear @a");
		context.waitTicks(5);
		use(context);
		context.waitTicks(5);
		boolean open = context.computeOnClient(client -> client.gui.screen() instanceof SkipScreen);
		assertTrue(open, "an empty hand opens the skip's screen");
		double[] slot = context.computeOnClient(client -> {
			double scale = client.getWindow().getGuiScale();
			int left = (client.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (client.getWindow().getGuiScaledHeight() - 133) / 2;
			return new double[] {(left + SkipMenu.CARGO_X + 8) * scale, (top + SkipMenu.CARGO_Y + 8) * scale};
		});
		context.getInput().setCursorPos(slot[0], slot[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		int afterTake = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int cursor = context.computeOnClient(c -> c.player.containerMenu.getCarried().getCount());
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		int afterPut = server.computeOnServer(s -> skip(s.overworld(), g).count());
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		SimpleBlockTestLog.info("CLIENT SKIP: placed by key ({}), client box {} tall, no bridle; loaded by hand: '{}'; slot click took a stack: skip {}, cursor {}; "
			+ "clicked back: skip {}", placed, height, loaded, afterTake, cursor, afterPut);
		assertTrue(loaded.isEmpty() && afterLoad == 512 && afterTake == 448 && cursor == 64 && afterPut == 512, "load (M11: no text) and slot clicks: '"
			+ loaded + "' " + afterLoad + "/" + afterTake
			+ "/" + cursor + "/" + afterPut);
		server.computeOnServer(s -> {
			skip(s.overworld(), g).setCargo(Items.RAW_IRON, 300);
			return null;
		});
	}

	// ---- The free hook ----

	private static void freeHook(ClientGameTestContext context, TestServerContext server, int g) {
		BlockPos drumPos = new BlockPos(202, g + DRUM_Y, Z);
		server.runCommand("tp @a 203.5 " + (g + DRUM_Y) + " " + (Z + 2.5) + " 180 30");
		hold(server, context, "simple_block:cable_coil");
		lookAtPoint(context, new Vec3(202.5, g + DRUM_Y + 0.85, Z + 0.5));
		use(context);
		server.runCommand("clear @a");
		context.waitTicks(5);
		lookAtHook(context);
		use(context);
		server.runCommand("tp @a 206.5 " + (g + DRUM_Y) + " 7.5 180 -40");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(207.5, g + TOP + 0.6, Z + 0.5));
		use(context);
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 33) + " 9.5 180 -40");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(X + 0.5, g + TOP + 0.6, Z + 0.5));
		use(context);
		// The head anchor again: the end put down, hanging under it.
		use(context);
		List<BlockPos> route = server.computeOnServer(s -> drumOf(s.overworld(), drumPos).route());
		context.waitTicks(10);
		double hanging = hookEye(server, g);
		CableAnchorBlock.Return head = clientHead(context, g);
		shot(context, server, "m07p-free-hook-under-ordinary-anchor", X + 3.5, g + TOP - 2.5, Z - 3.5, 45, -20);
		assertTrue(route.size() == 2 && Math.abs(hanging - (TOP + 0.25)) < 0.01 && head == CableAnchorBlock.Return.NONE,
			"routed through 2 anchors " + route + ", free hook under the anchor at " + hanging + ", anchor " + head);

		// The coal winch lowers it all the way down the shaft, onto the skip's top.
		server.runCommand("tp @a 202.5 " + (g + DRUM_Y) + " " + (Z - 2.5) + " 0 40");
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(202.5, g + DRUM_Y + 0.85, Z + 0.5));
		use(context);
		hold(server, context, "minecraft:coal 3");
		for (int i = 0; i < 3; i++) {
			use(context);
		}
		server.runCommand("clear @a");
		lever(context, server, g, CableLines.Lever.FEED);
		context.waitTicks(80);
		double midway = hookEye(server, g);
		CableAnchorBlock.Return headMidway = clientHead(context, g);
		shot(context, server, "m07p-free-hook-winched-down-anchor-ordinary", X + 4.5, g + TOP - 6, Z - 4.5, 45, -10);
		int waited = 0;
		while (waited < 600 && server.computeOnServer(s -> line(s.overworld(), g).lever()) != CableLines.Lever.NEUTRAL) {
			context.waitTicks(10);
			waited += 10;
		}
		double down = hookEye(server, g);
		String stop = server.computeOnServer(s -> line(s.overworld(), g).lastStop());
		CableAnchorBlock.Return headDown = clientHead(context, g);
		shot(context, server, "m07p-free-hook-down-on-the-skip", X + 2.5, g + 4.5, Z - 2.5, 45, 25);
		SimpleBlockTestLog.info("CLIENT SKIP FREE HOOK: routed by hand through {}; put down: eye at y {} under the anchor ({}); coal winch FEED: y {} after 80 "
			+ "ticks (anchor {}), stopped at y {} on the skip's top ({}), anchor {}", route, fmt(hanging), head, fmt(midway), headMidway, fmt(down), stop,
			headDown);
		assertTrue(midway < hanging - 5.0 && headMidway == CableAnchorBlock.Return.NONE, "lowering, anchor ordinary: " + midway + ", " + headMidway);
		assertTrue(Math.abs(down - 3.5) < 0.1 && "blocked".equals(stop) && headDown == CableAnchorBlock.Return.NONE,
			"down on the skip's top (eye 3.5): " + down + " (" + stop + "), anchor " + headDown);
	}

	/** Clicks the coal winch's lever (on the drum's south side: it faces east) until it is at {@code lever}. */
	private static void lever(ClientGameTestContext context, TestServerContext server, int g, CableLines.Lever lever) {
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a 202.5 " + (g + DRUM_Y) + " " + (Z + 2.5) + " 180 30");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(202.5, g + DRUM_Y + 9.5 / 16.0, Z + 0.5 + 0.55));
		for (int i = 0; i < 3 && server.computeOnServer(s -> line(s.overworld(), g).lever()) != lever; i++) {
			// The owner's clicks: right for PULL, left for FEED, the same click again for NEUTRAL.
			CableLines.Lever now = server.computeOnServer(s -> line(s.overworld(), g).lever());
			if (lever == CableLines.Lever.PULL || lever == CableLines.Lever.NEUTRAL && now == CableLines.Lever.PULL) {
				context.getInput().pressKey(options -> options.keyUse);
			} else {
				context.getInput().pressKey(options -> options.keyAttack);
			}
			context.waitTicks(3);
		}
		CableLines.Lever now = server.computeOnServer(s -> line(s.overworld(), g).lever());
		assertTrue(now == lever, "lever: " + now + ", wanted " + lever);
	}

	// ---- Hooking on at the bottom ----

	private static void hookOn(ClientGameTestContext context, TestServerContext server, int g) {
		// On the floor, far enough out to see over the skip's top (where the hook rests) and within reach.
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 2.9) + " 180 0");
		context.waitTicks(10);
		lookAtPoint(context, new Vec3(X + 0.5, g + 3.45, Z + 0.5));
		use(context);
		boolean held = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(CableHook.class, new AABB(X - 30, g - 2, Z - 30, X + 30, g + 45, Z + 30))
			.stream().anyMatch(CableHook::isHeld));
		context.waitTicks(10);
		boolean stillHeld = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(CableHook.class, new AABB(X - 30, g - 2, Z - 30, X + 30, g + 45, Z + 30))
			.stream().anyMatch(CableHook::isHeld));
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.5));
		use(context);
		String message = overlayMessage(context);
		context.waitTicks(10);
		boolean hung = server.computeOnServer(s -> skip(s.overworld(), g).bridled());
		double height = clientSkipHeight(context, g);
		CableAnchorBlock.Return head = clientHead(context, g);
		shot(context, server, "m07p-skip-bridle-on", X - 2.5, g + 3.5, Z - 3.5, -35, 20);
		shot(context, server, "m07p-anchor-turned-pulley", X + 2.3, g + TOP - 1.9, Z - 1.3, 45, -12);
		SimpleBlockTestLog.info("CLIENT SKIP HOOK-ON: at the foot of the {}-block shaft the hook was picked up (held {}, still held 10 ticks later {}) and used on "
			+ "the skip: '{}', bridle {}, client box {} tall, head anchor {}", TOP, held, stillHeld, message, hung, height, head);
		assertTrue(held && stillHeld, "the hook stays in the hand at the shaft's foot, " + TOP + " blocks under its anchor");
		assertTrue(hung && Math.abs(height - 4.0) < 1.0E-6 && head == CableAnchorBlock.Return.PULLEY, "hooked on: bridle " + hung + ", height " + height
			+ ", anchor " + head);
	}

	// ---- Loading at the bridle ----

	private static void load(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (g + LOAD_RAIL + 0.1) + " " + (Z - 0.5));
		context.waitTicks(5);
		server.computeOnServer(s -> {
			MinecartCargo.cart(cart(s.overworld(), g + LOAD_RAIL)).simpleBlock$setCargo(Items.RAW_IRON, 200);
			return null;
		});
		lever(context, server, g, CableLines.Lever.PULL);
		// Patch §9: the hoist cable's texture travels towards the drum while PULL lifts the skip (two frames
		// of the shaft's cable, the phase read between them).
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a " + (X + 1.7) + " " + (g + 9) + " " + (Z + 0.5) + " 90 -15");
		context.waitTicks(10);
		double pull1 = cablePhase(context, g);
		context.takeScreenshot("m07p-hoist-cable-pull-frame-1");
		context.waitTicks(3);
		double pull2 = cablePhase(context, g);
		context.takeScreenshot("m07p-hoist-cable-pull-frame-2");
		server.runCommand("tp @a " + (X + 3.5) + " " + (g + LOAD_RAIL - 0.6) + " " + (Z + 0.0) + " 90 5");
		float swing = 0.0F;
		int ticks = 0;
		for (; ticks < 600 && swing <= 0.0F; ticks++) {
			context.waitTicks(1);
			swing = gateSwing(context, g + LOAD_RAIL);
		}
		context.waitTicks(4);
		float half = gateSwing(context, g + LOAD_RAIL);
		context.takeScreenshot("m07p-load-gate-swinging-down");
		context.waitTicks(8);
		float full = gateSwing(context, g + LOAD_RAIL);
		// Patch §8 / cart redesign: in the cart's own model the end gate towards the skip is folded down, alone.
		float[] walls = gates(context, g + LOAD_RAIL);
		// Patch §9: stopped at the station (lever NEUTRAL), the cable stands still.
		double held1 = cablePhase(context, g);
		context.waitTicks(4);
		double held2 = cablePhase(context, g);
		float lid = context.computeOnClient(client -> client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 40, Z + 3))
			.stream().findFirst().map(s -> s.lidAnim).orElse(-1.0F));
		context.takeScreenshot("m07p-load-ramp-down-side");
		server.runCommand("tp @a " + (X + 2.5) + " " + (g + LOAD_RAIL + 0.4) + " " + (Z - 2.0) + " 39 22");
		context.waitTicks(3);
		context.takeScreenshot("m07p-load-ramp-down-front");
		// Patch §8: the cart's open end, from above the skip and from beside it: no end wall behind the ramp.
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + LOAD_RAIL + 1.2) + " " + (Z + 1.3) + " 180 62");
		context.waitTicks(3);
		context.takeScreenshot("m07p-load-cart-end-open-above");
		server.runCommand("tp @a " + (X + 1.9) + " " + (g + LOAD_RAIL - 0.9) + " " + (Z + 0.6) + " 140 8");
		context.waitTicks(3);
		context.takeScreenshot("m07p-load-cart-end-open-straight");
		context.waitTicks(80);
		float closed = gateSwing(context, g + LOAD_RAIL);
		double y = skipY(server, g);
		int skipCount = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int cartCount = server.computeOnServer(s -> MinecartCargo.cart(cart(s.overworld(), g + LOAD_RAIL)).simpleBlock$count());
		server.runCommand("gamemode survival @a");
		SimpleBlockTestLog.info("CLIENT SKIP LOAD: PULL stopped at y {}; the cart's gate began to swing after {} ticks: {} -> {} -> {} (client), the skip's lid "
			+ "{}; skip 300 -> {}, cart 200 -> {}; gate afterwards {}", fmt(y), ticks, fmt(swing), fmt(half), fmt(full), fmt(lid), skipCount, cartCount,
			fmt(closed));
		SimpleBlockTestLog.info("CLIENT SKIP CABLE (patch §9): PULL, cable texture at the route's end {} -> {} in 3 ticks (towards the drum); stopped at "
			+ "the station {} -> {} in 4 ticks. CLIENT FREIGHT GATE (patch §8, cart redesign): the docked cart's gates, as drawn {}", fmt(pull1),
			fmt(pull2), fmt(held1), fmt(held2), fmtGates(walls));
		assertTrue(pull2 - pull1 > 0.1 && Math.abs(held2 - held1) < 1.0E-6, "the cable moves while lifting and stands still when stopped: " + pull1 + " -> "
			+ pull2 + ", " + held1 + " -> " + held2);
		assertTrue((walls[0] > 112.0F) != (walls[1] > 112.0F) && walls[0] + walls[1] < 113.0F && walls[2] == 0.0F && walls[3] == 0.0F,
			"one end gate folded down, the others shut: " + fmtGates(walls));
		assertTrue(Math.abs(y - (LOAD_RAIL - 3)) < 1.0E-6, "stopped with the bridle level with the cart: " + y);
		assertTrue(half > 0.2F && half < 0.9F && full > 0.99F && lid > 0.99F, "the gate swings down (" + half + ", " + full + ") and the lid opens (" + lid + ")");
		assertTrue(skipCount == 500 && cartCount == 0 && closed < 0.01F, "loaded and closed: " + skipCount + "/" + cartCount + ", gate " + closed);
	}

	/** Patch §9: the hoist cable's texture row where the route ends (moves with the drum's travel). */
	private static double cablePhase(ClientGameTestContext context, int g) {
		return context.computeOnClient(client -> {
			CableDrumRenderer.RouteEnd end = CableDrumRenderer.routeEnd(client.level, new BlockPos(202, g + DRUM_Y, Z), 1.0F);
			return end == null ? Double.NaN : end.v();
		});
	}

	/**
	 * Patch §8 and the owner's cart redesign: the body the cart renderer draws for the cart on the rail at
	 * {@code railY}, set up exactly as for drawing it: how far each gate is turned on its hinge, in
	 * degrees (+x end, -x end, +z side, -z side).
	 */
	static float[] gates(ClientGameTestContext context, int railY) {
		return context.computeOnClient(client -> {
			Vec3 rail = new Vec3(X + 0.5, railY, Z - 0.5);
			Minecart cart = client.level.getEntitiesOfClass(Minecart.class, new AABB(X - 1, railY - 1, Z - 2, X + 2, railY + 2, Z)).stream()
				.min(java.util.Comparator.comparingDouble(c -> c.position().distanceToSqr(rail))).orElseThrow();
			return gates(client, cart);
		});
	}

	static float[] gates(net.minecraft.client.Minecraft client, net.minecraft.world.entity.vehicle.minecart.AbstractMinecart cart) {
		@SuppressWarnings("unchecked")
		var renderer = (net.minecraft.client.renderer.entity.EntityRenderer<net.minecraft.world.entity.vehicle.minecart.AbstractMinecart,
			net.minecraft.client.renderer.entity.state.MinecartRenderState>)
			client.getEntityRenderDispatcher().getRenderer(cart);
		net.minecraft.client.renderer.entity.state.MinecartRenderState state = renderer.createRenderState();
		renderer.extractRenderState(cart, state, 1.0F);
		net.minecraft.client.model.object.cart.MinecartModel model = ((AbstractMinecartRendererAccessor) renderer).simpleBlock$getModel();
		model.setupAnim(state);
		float[] turned = new float[4];
		String[] names = {"gate_end_plus", "gate_end_minus", "gate_side_plus", "gate_side_minus"};
		for (int i = 0; i < 4; i++) {
			var part = model.root().getChild(names[i]);
			turned[i] = (float) Math.toDegrees(Math.abs(part.xRot) + Math.abs(part.zRot));
		}
		return turned;
	}

	static String fmtGates(float[] turned) {
		return String.format("[+x end %.1f, -x end %.1f, +z side %.1f, -z side %.1f]", turned[0], turned[1], turned[2], turned[3]);
	}

	private static float gateSwing(ClientGameTestContext context, int railY) {
		return context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, new AABB(X - 1, railY - 1, Z - 2, X + 2, railY + 2, Z))
			.stream().findFirst().map(c -> LoadingTracks.freight(c).simpleBlock$gateSwing(1.0F)).orElse(-1.0F));
	}

	private static Minecart cart(ServerLevel level, int y) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(X - 1, y - 0.5, Z - 2, X + 2, y + 1, Z + 2)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no cart at y " + y));
	}

	// ---- Side-on: a coupled train tips into the skip through a long side gate (owner's cart redesign) ----

	private static void sideOn(ClientGameTestContext context, TestServerContext server, int g) {
		int y = g + LOAD_RAIL;
		// The loading step's cart taken away (empty), then the rail at the bridle turned to run across the
		// skip's front, an ordinary rail either side.
		server.computeOnServer(s -> {
			s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, y - 1, Z - 2, X + 4, y + 2, Z)).forEach(SkipClientTest::scrap);
			return null;
		});
		server.runCommand("fill " + (X - 2) + " " + (y - 1) + " " + (Z - 1) + " " + (X + 3) + " " + (y - 1) + " " + (Z - 1) + " minecraft:stone");
		server.runCommand("fill " + (X - 2) + " " + y + " " + (Z - 1) + " " + (X + 3) + " " + y + " " + (Z - 1) + " minecraft:rail[shape=east_west]");
		server.runCommand("setblock " + X + " " + y + " " + (Z - 1) + " simple_block:loading_track[facing=east,shape=east_west]");
		server.computeOnServer(s -> {
			skip(s.overworld(), g).setCargo(Items.AIR, 0);
			return null;
		});
		server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (y + 0.1) + " " + (Z - 0.5));
		server.runCommand("summon minecraft:minecart " + (X + 2.5) + " " + (y + 0.1) + " " + (Z - 0.5));
		context.waitTicks(5);
		server.computeOnServer(s -> {
			List<Minecart> train = s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, y - 1, Z - 2, X + 4, y + 2, Z));
			train.sort(java.util.Comparator.comparingDouble(Minecart::getX));
			for (Minecart cart : train) {
				MinecartCargo.cart(cart).simpleBlock$setCargo(Items.RAW_IRON, 150);
			}
			var player = s.getPlayerList().getPlayers().getFirst();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(Items.IRON_CHAIN, 4));
			player.interactOn(train.get(0), net.minecraft.world.InteractionHand.MAIN_HAND, Vec3.ZERO);
			player.interactOn(train.get(1), net.minecraft.world.InteractionHand.MAIN_HAND, Vec3.ZERO);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			return null;
		});
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a " + (X + 4.6) + " " + (y + 0.2) + " " + (Z - 0.1) + " 90 8");
		float[] open = null;
		for (int i = 0; i < 100; i++) {
			context.waitTicks(1);
			float[] turned = gates(context, y);
			if (turned[2] + turned[3] > 99.0F) {
				open = turned;
				break;
			}
		}
		context.takeScreenshot("m07c-side-on-train-gate-up-along");
		server.runCommand("tp @a " + (X + 2.4) + " " + (y + 1.9) + " " + (Z - 2.9) + " 30 38");
		context.waitTicks(3);
		context.takeScreenshot("m07c-side-on-train-gate-up-above");
		context.waitTicks(80);
		int skipCount = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int[] cars = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, y - 1, Z - 2, X + 4, y + 2, Z)).stream()
			.sorted(java.util.Comparator.comparingDouble(Minecart::getX)).mapToInt(c -> MinecartCargo.cart(c).simpleBlock$count()).toArray());
		boolean coupled = server.computeOnServer(s -> {
			List<Minecart> train = s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, y - 1, Z - 2, X + 4, y + 2, Z));
			return io.github.boxofdog.simpleblock.train.Couplings.coupled(train.get(0)).simpleBlock$coupling(0) != null;
		});
		float[] after = gates(context, y);
		SimpleBlockTestLog.info("CLIENT SKIP SIDE-ON (cart redesign): coupled train across the skip's front at the bridle; the docked car's gates while tipping {}; "
			+ "skip 0 -> {}, cars (west to east) {}, still coupled {}; gates afterwards {}", open == null ? "never open" : fmtGates(open), skipCount,
			java.util.Arrays.toString(cars), coupled, fmtGates(after));
		assertTrue(open != null && open[0] == 0.0F && open[1] == 0.0F, "a side gate opened, not an end gate: " + (open == null ? "none" : fmtGates(open)));
		assertTrue(skipCount == 150 && cars.length == 2 && cars[0] == 0 && cars[1] == 150 && coupled, "the docked car tipped sideways, the other "
			+ "untouched, still coupled: skip " + skipCount + ", cars " + java.util.Arrays.toString(cars));
		assertTrue(java.util.Arrays.equals(after, new float[4]), "gate shut afterwards: " + fmtGates(after));
		// Back as it was: the train gone, the end-on rail again, the skip's load as the load step left it.
		server.computeOnServer(s -> {
			s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, y - 1, Z - 2, X + 4, y + 2, Z)).forEach(SkipClientTest::scrap);
			skip(s.overworld(), g).setCargo(Items.RAW_IRON, 500);
			return null;
		});
		server.runCommand("fill " + (X - 2) + " " + (y - 1) + " " + (Z - 1) + " " + (X + 3) + " " + y + " " + (Z - 1) + " minecraft:air");
		server.runCommand("setblock " + X + " " + (y - 1) + " " + (Z - 1) + " minecraft:stone");
		server.runCommand("setblock " + X + " " + y + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
		server.runCommand("gamemode survival @a");
	}

	// ---- Unloading through the chute ----

	private static void unload(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (g + 0.1) + " " + (Z - 0.5));
		context.waitTicks(5);
		lever(context, server, g, CableLines.Lever.FEED);
		// Patch §9: FEED lowering the skip moves the cable's texture away from the drum.
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a " + (X + 1.7) + " " + (g + 9) + " " + (Z + 0.5) + " 90 -15");
		context.waitTicks(10);
		double feed1 = cablePhase(context, g);
		context.takeScreenshot("m07p-hoist-cable-feed-frame-1");
		context.waitTicks(3);
		double feed2 = cablePhase(context, g);
		context.takeScreenshot("m07p-hoist-cable-feed-frame-2");
		SimpleBlockTestLog.info("CLIENT SKIP CABLE (patch §9): FEED, cable texture at the route's end {} -> {} in 3 ticks (away from the drum)", fmt(feed1),
			fmt(feed2));
		assertTrue(feed1 - feed2 > 0.1, "the cable moves the other way lowering: " + feed1 + " -> " + feed2);
		server.runCommand("gamemode spectator @a");
		server.runCommand("tp @a " + (X + 2.9) + " " + (g - 0.1) + " " + (Z - 0.2) + " 90 15");
		float gate = 0.0F;
		for (int i = 0; i < 600 && gate < 1.0F; i++) {
			context.waitTicks(1);
			gate = context.computeOnClient(client -> client.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 2, g - 2, Z - 2, X + 3, g + 40, Z + 3))
				.stream().findFirst().map(s -> s.gateAnim).orElse(-1.0F));
		}
		double y = skipY(server, g);
		float cartGate = gateSwing(context, g);
		context.takeScreenshot("m07p-unload-lip-on-rim-side");
		server.runCommand("tp @a " + (X + 0.5) + " " + (g - 0.4) + " " + (Z - 2.6) + " 0 8");
		context.waitTicks(3);
		context.takeScreenshot("m07p-unload-lip-on-rim-front");
		context.waitTicks(100);
		int left = server.computeOnServer(s -> skip(s.overworld(), g).count());
		int bottomCart = server.computeOnServer(s -> MinecartCargo.cart(cart(s.overworld(), g)).simpleBlock$count());
		SimpleBlockTestLog.info("CLIENT SKIP UNLOAD: FEED stopped at y {} (rail + 11/16: the chute's lip on the rim of the cart one level lower); client saw the "
			+ "discharge gate open {}, the cart's gate {}; skip 500 -> {}, cart 0 -> {}", fmt(y), fmt(gate), fmt(cartGate), left, bottomCart);
		assertTrue(Math.abs(y - SkipStations.UNLOAD_OFFSET) < 1.0E-6, "stopped with the lip on the rim: " + y);
		assertTrue(gate > 0.99F && cartGate < 0.01F && left == 244 && bottomCart == 256, "unloaded through the chute: gate " + gate + ", cart gate " + cartGate
			+ ", " + left + "/" + bottomCart);

		// Patch §8 / cart redesign: the unloading cart's gates stay shut; the same cart with a cushion fitted
		// (a passenger cart) keeps them shut too.
		float[] freightWalls = gates(context, g);
		server.computeOnServer(s -> {
			MinecartCargo.cart(cart(s.overworld(), g)).simpleBlock$setCushion(net.minecraft.world.item.DyeColor.RED);
			return null;
		});
		context.waitTicks(5);
		float[] cushionWalls = gates(context, g);
		SimpleBlockTestLog.info("CLIENT FREIGHT GATE (patch §8, cart redesign): unloading freight cart's gates {}; with a cushion fitted {}",
			fmtGates(freightWalls), fmtGates(cushionWalls));
		assertTrue(java.util.Arrays.equals(freightWalls, new float[4]) && java.util.Arrays.equals(cushionWalls, new float[4]), "all gates shut: "
			+ fmtGates(freightWalls) + ", " + fmtGates(cushionWalls));
		server.computeOnServer(s -> {
			MinecartCargo.cart(cart(s.overworld(), g)).simpleBlock$setCushion(null);
			return null;
		});

		// The same contact facing each other way: the skip turned, a rail and a cart below its chute on that side.
		List<String> turns = new ArrayList<>();
		for (Direction facing : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST)) {
			BlockPos rail = new BlockPos(X, g, Z).relative(facing);
			server.computeOnServer(s -> {
				s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, g - 1, Z - 2, X + 3, g + 2, Z + 3)).forEach(SkipClientTest::scrap);
				skip(s.overworld(), g).setFacing(facing);
				skip(s.overworld(), g).setCargo(Items.RAW_IRON, 100);
				return null;
			});
			server.runCommand("setblock " + X + " " + g + " " + (Z - 1) + " minecraft:air");
			server.runCommand("setblock " + rail.getX() + " " + rail.getY() + " " + rail.getZ() + " simple_block:loading_track[facing="
				+ facing.getOpposite().getSerializedName() + ",shape=" + (facing.getAxis() == Direction.Axis.Z ? "north_south" : "east_west") + "]");
			server.runCommand("summon minecraft:minecart " + (rail.getX() + 0.5) + " " + (g + 0.1) + " " + (rail.getZ() + 0.5));
			context.waitTicks(30);
			Direction side = facing.getClockWise();
			shot(context, server, "m07p-unload-lip-on-rim-" + facing.getSerializedName(), X + 0.5 + side.getStepX() * 2.9 + facing.getStepX() * 0.3,
				g - 0.1, Z + 0.5 + side.getStepZ() * 2.9 + facing.getStepZ() * 0.3, side.getOpposite().toYRot(), 15);
			context.waitTicks(40);
			int got = server.computeOnServer(s -> MinecartCargo.cart(s.overworld().getEntitiesOfClass(Minecart.class, new AABB(rail)).getFirst())
				.simpleBlock$count());
			turns.add(facing + ": cart " + got);
			server.runCommand("setblock " + rail.getX() + " " + rail.getY() + " " + rail.getZ() + " minecraft:air");
		}
		server.computeOnServer(s -> {
			s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 2, g - 1, Z - 2, X + 3, g + 2, Z + 3)).forEach(SkipClientTest::scrap);
			skip(s.overworld(), g).setFacing(Direction.NORTH);
			return null;
		});
		server.runCommand("setblock " + X + " " + g + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
		SimpleBlockTestLog.info("CLIENT SKIP UNLOAD, every way: {}", turns);
		assertTrue(turns.stream().allMatch(t -> t.endsWith("cart 100")), "unloaded facing every way: " + turns);
	}

	// ---- Taking it off ----

	private static void takeOff(ClientGameTestContext context, TestServerContext server, int g) {
		lever(context, server, g, CableLines.Lever.FEED);
		int waited = 0;
		while (waited < 300 && server.computeOnServer(s -> line(s.overworld(), g).lever()) != CableLines.Lever.NEUTRAL) {
			context.waitTicks(10);
			waited += 10;
		}
		double floor = skipY(server, g);
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 2.5) + " 180 10");
		context.waitTicks(5);
		server.runCommand("clear @a");
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.5));
		context.waitTicks(1);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		String said = overlayMessage(context);
		context.waitTicks(10);
		boolean bridled = server.computeOnServer(s -> skip(s.overworld(), g).bridled());
		double height = clientSkipHeight(context, g);
		CableAnchorBlock.Return head = clientHead(context, g);
		double eye = hookEye(server, g);
		shot(context, server, "m07p-anchor-ordinary-again", X + 2.3, g + TOP - 1.9, Z - 1.3, 45, -12);
		shot(context, server, "m07p-skip-taken-off-hook-at-apex", X - 2.5, g + 3.5, Z - 3.5, -35, 20);
		SimpleBlockTestLog.info("CLIENT SKIP TAKE-OFF: FEED down to the floor (y {}); sneak-use: '{}', bridle {}, client box {} tall, head anchor {}, free hook eye at y {}",
			fmt(floor), said, bridled, height, head, fmt(eye));
		assertTrue(Math.abs(floor) < 1.0E-3 && !bridled && Math.abs(height - 3.0) < 1.0E-6 && head == CableAnchorBlock.Return.NONE
			&& Math.abs(eye - OreSkip.HEIGHT) < 0.01, "taken off: floor " + floor + ", bridle " + bridled + ", height " + height + ", anchor " + head + ", hook " + eye);
	}

	// ---- The winch while the free hook is unloaded ----

	private static void unloaded(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server, int g) {
		lever(context, server, g, CableLines.Lever.PULL);
		server.runCommand("tp @a " + (X + 400.5) + " " + g + " " + (Z + 0.5));
		double left = Double.NaN;
		int ticks = 0;
		for (; ticks < 200; ticks++) {
			double eye = hookEye(server, g);
			if (Double.isNaN(eye)) {
				break;
			}
			left = eye;
			context.waitTicks(1);
		}
		double drop = server.computeOnServer(s -> line(s.overworld(), g).drop());
		context.waitTicks(100);
		double dropAway = server.computeOnServer(s -> line(s.overworld(), g).drop());
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " 2.5 0 20");
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		double back = hookEye(server, g);
		CableLines.Lever lever = server.computeOnServer(s -> line(s.overworld(), g).lever());
		String stop = server.computeOnServer(s -> line(s.overworld(), g).lastStop());
		SimpleBlockTestLog.info("CLIENT SKIP UNLOADED: PULL, then 400 blocks away: the hook left the loaded world after {} ticks at y {} (cable paid out {}); "
			+ "100 ticks later {}; back: hook at y {}, lever {} ({})", ticks, fmt(left), fmt(drop), fmt(dropAway), fmt(back), lever, stop);
		assertTrue(ticks < 200 && Math.abs(drop - dropAway) < 1.0E-9 && Math.abs(back - left) < 0.01 && lever == CableLines.Lever.NEUTRAL,
			"the hook does not move while unloaded: " + left + " -> " + back + ", paid out " + drop + " -> " + dropAway + ", lever " + lever);
	}

	// ---- Save and reopen ----

	private record Snapshot(double skipY, int count, boolean bridled, double drop, int burn) {
	}

	private static Snapshot snapshot(TestServerContext server, int g) {
		return server.computeOnServer(s -> {
			OreSkip skip = skip(s.overworld(), g);
			CableLines.Line line = line(s.overworld(), g);
			return new Snapshot(skip.getY() - g, skip.count(), skip.bridled(), line.drop(), line.burn() + line.coal() * 1600);
		});
	}

	private static void reopened(ClientGameTestContext context, TestServerContext server, int g, Snapshot before) {
		context.waitTicks(20);
		Snapshot after = snapshot(server, g);
		double eye = hookEye(server, g);
		CableAnchorBlock.Return head = clientHead(context, g);
		SimpleBlockTestLog.info("CLIENT SKIP REOPEN: before {}; after saving and reopening {}; free hook at y {}, head anchor {}", before, after, fmt(eye), head);
		assertTrue(after.equals(before) && head == CableAnchorBlock.Return.NONE, "kept through save and reopen: " + before + " / " + after + ", anchor " + head);
	}

	private static String fmt(double value) {
		return String.format("%.2f", value);
	}
}
