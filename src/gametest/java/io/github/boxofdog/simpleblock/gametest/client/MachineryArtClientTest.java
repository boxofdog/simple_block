package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.client.train.CobblestoneEngineRenderer;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.Couplings;

/**
 * M07 visual revisions (owner's brief sections 5A and 5B) in a real client, with before and after
 * screenshots ({@code m07-art-*} in {@code build/run/clientGameTest/screenshots}):
 * <ul>
 *   <li><b>Coal winch motor:</b> a manual drum on the ground and one hanging under a beam keep their
 *       stand; a furnace used on each (real key presses) replaces the motor-side stand with the
 *       furnace motor. The block state ({@code powered}) is seen by the client; the drum's shape, the
 *       loop-free use and the lever are as before.</li>
 *   <li><b>Cobblestone Engine:</b> made from a minecart and a furnace (real key presses), drawn inside
 *       the vanilla minecart: stopped, coupled to a freight cart, running, and standing on a curve and
 *       on a slope.</li>
 * </ul>
 * The yard, at ground level {@code g}: stone x 96..124, z -4..20. Drums along z 0 (ground) and z 6
 * (hanging under planks at g + 3); the engine's track along z 12, a curve at x 118 and a slope at z 16.
 */
public class MachineryArtClientTest implements FabricClientGameTest {
	private static final int TRACK_Z = 12;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(5);
			client.options.simulationDistance().set(5);
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			// The world is superflat: ground level is the same everywhere (measured near spawn, where it is loaded).
			int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			server.runCommand("tp @a 102.5 " + g + " -3.5 0 20");
			world.getConnection().waitForChunksRender();
			buildYard(server, g);
			context.waitTicks(20);

			winch(context, server, g, new BlockPos(100, g, 0), new BlockPos(104, g, 0), false);
			winch(context, server, g, new BlockPos(100, g + 2, 6), new BlockPos(104, g + 2, 6), true);
			engine(context, server, g);
		}
	}

	private static void buildYard(TestServerContext server, int g) {
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode survival @a");
		server.runCommand("fill 96 " + (g - 1) + " -4 124 " + (g - 1) + " 20 minecraft:stone");
		server.runCommand("fill 96 " + g + " -4 124 " + (g + 8) + " 20 minecraft:air");
		for (int x : new int[] {100, 104}) {
			server.runCommand("setblock " + x + " " + g + " 0 simple_block:cable_drum[facing=east]");
			server.runCommand("setblock " + x + " " + (g + 3) + " 6 minecraft:oak_planks");
			server.runCommand("setblock " + x + " " + (g + 2) + " 6 simple_block:cable_drum[facing=east,hanging=true]");
		}
		server.runCommand("fill 98 " + g + " " + TRACK_Z + " 117 " + g + " " + TRACK_Z + " minecraft:rail[shape=east_west]");
		// A curve at x 118 turning south, then south to z 15, and a slope up at z 16.
		server.runCommand("setblock 118 " + g + " " + TRACK_Z + " minecraft:rail[shape=south_west]");
		server.runCommand("fill 118 " + g + " " + (TRACK_Z + 1) + " 118 " + g + " " + (TRACK_Z + 3) + " minecraft:rail[shape=north_south]");
		server.runCommand("setblock 118 " + g + " " + (TRACK_Z + 4) + " minecraft:stone");
		server.runCommand("setblock 118 " + g + " " + (TRACK_Z + 3) + " minecraft:rail[shape=ascending_south]");
		server.runCommand("setblock 118 " + (g + 1) + " " + (TRACK_Z + 4) + " minecraft:rail[shape=north_south]");
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

	/** Two manual drums side by side; the second gets a furnace by a real key press. Before and after shots. */
	private static void winch(ClientGameTestContext context, TestServerContext server, int g, BlockPos manual, BlockPos fitted, boolean hanging) {
		String where = hanging ? "hanging" : "ground";
		int eye = hanging ? g : g + 1;
		server.runCommand("tp @a " + (manual.getX() + 2.5) + " " + eye + " " + (manual.getZ() - 3.0) + " 0 " + (hanging ? -15 : 30));
		context.waitTicks(10);
		context.takeScreenshot("m07-art-winch-" + where + "-before");
		BlockState before = server.computeOnServer(s -> s.overworld().getBlockState(fitted));

		server.runCommand("tp @a " + (fitted.getX() + 0.5) + " " + g + " " + (fitted.getZ() - 2.5) + " 0 20");
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(fitted.getX() + 0.5, fitted.getY() + 0.6, fitted.getZ() + 0.5));
		use(context);
		server.runCommand("clear @a");
		BlockState after = server.computeOnServer(s -> s.overworld().getBlockState(fitted));
		boolean serverPowered = server.computeOnServer(s -> s.overworld().getBlockEntity(fitted) instanceof CableDrumBlockEntity drum && drum.powered());
		boolean clientState = context.computeOnClient(client -> client.level.getBlockState(fitted).getValue(CableDrumBlock.POWERED));
		boolean manualKept = !server.computeOnServer(s -> s.overworld().getBlockState(manual).getValue(CableDrumBlock.POWERED));
		boolean sameShape = server.computeOnServer(s -> sameShape(s.overworld(), fitted, before, after));

		server.runCommand("tp @a " + (manual.getX() + 2.5) + " " + eye + " " + (manual.getZ() - 3.0) + " 0 " + (hanging ? -15 : 30));
		context.waitTicks(10);
		context.takeScreenshot("m07-art-winch-" + where + "-after");
		// The motor side (west), close up, and the lever side (east).
		server.runCommand("tp @a " + (fitted.getX() - 1.8) + " " + eye + " " + (fitted.getZ() - 1.8) + " -45 " + (hanging ? -25 : 35));
		context.waitTicks(10);
		context.takeScreenshot("m07-art-winch-" + where + "-motor-side");
		server.runCommand("tp @a " + (fitted.getX() + 2.8) + " " + eye + " " + (fitted.getZ() - 1.8) + " 45 " + (hanging ? -25 : 35));
		context.waitTicks(10);
		context.takeScreenshot("m07-art-winch-" + where + "-lever-side");
		SimpleBlockTestLog.info("CLIENT ART WINCH ({}): before {}; furnace by key: after {}, drum powered {}, client state powered {}; manual drum kept "
			+ "its stand {}; shape unchanged {}", where, before, after, serverPowered, clientState, manualKept, sameShape);
		assertTrue(!before.getValue(CableDrumBlock.POWERED) && after.getValue(CableDrumBlock.POWERED) && serverPowered && clientState,
			where + ": the furnace should switch the frame to the motor");
		assertTrue(after.getValue(CableDrumBlock.HANGING) == hanging && manualKept && sameShape, where + ": orientation, manual drum and shape kept");
	}

	private static boolean sameShape(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
		return before.getShape(level, pos, CollisionContext.empty()).toAabbs().equals(after.getShape(level, pos, CollisionContext.empty()).toAabbs())
			&& before.getCollisionShape(level, pos).toAabbs().equals(after.getCollisionShape(level, pos).toAabbs());
	}

	/** An engine made by real key presses, coupled to a freight cart; stopped, running, on a curve and a slope. */
	private static void engine(ClientGameTestContext context, TestServerContext server, int g) {
		server.runCommand("tp @a 106.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		hold(server, context, "minecraft:minecart");
		lookAtPoint(context, new Vec3(106.5, g + 0.1, TRACK_Z + 0.5));
		use(context);
		hold(server, context, "minecraft:furnace");
		lookAtPoint(context, new Vec3(106.5, g + 0.35, TRACK_Z + 0.5));
		use(context);
		hold(server, context, "minecraft:coal");
		lookAtPoint(context, new Vec3(106.5, g + 0.35, TRACK_Z + 0.5));
		use(context);
		boolean engine = server.computeOnServer(s -> !engines(s.overworld(), g).isEmpty());
		boolean rendered = context.computeOnClient(client -> client.level.getEntitiesOfClass(MinecartFurnace.class, new AABB(96, g - 1, TRACK_Z - 1, 124, g + 4, TRACK_Z + 1))
			.stream().anyMatch(e -> client.getEntityRenderDispatcher().getRenderer(e) instanceof CobblestoneEngineRenderer));
		server.runCommand("tp @a 103.5 " + (g + 1) + " " + (TRACK_Z - 2.5) + " -50 25");
		context.waitTicks(10);
		context.takeScreenshot("m07-art-engine-stopped");
		server.runCommand("tp @a 106.5 " + (g + 1) + " " + (TRACK_Z - 3.0) + " 0 30");
		context.waitTicks(10);
		context.takeScreenshot("m07-art-engine-side");

		// A freight cart coupled behind it (west).
		server.runCommand("tp @a 105.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		hold(server, context, "minecraft:minecart");
		lookAtPoint(context, new Vec3(104.5, g + 0.1, TRACK_Z + 0.5));
		use(context);
		hold(server, context, "minecraft:iron_chain 2");
		lookAtPoint(context, new Vec3(106.4, g + 0.35, TRACK_Z + 0.5));
		use(context);
		lookAtPoint(context, new Vec3(104.6, g + 0.35, TRACK_Z + 0.5));
		use(context);
		server.runCommand("clear @a");
		context.waitTicks(10);
		boolean coupled = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Minecart.class, new AABB(96, g - 1, TRACK_Z - 1, 124, g + 4, TRACK_Z + 1))
			.stream().anyMatch(Couplings::hasCouplings));
		server.runCommand("tp @a 103.0 " + (g + 1.5) + " " + (TRACK_Z + 3.5) + " -150 25");
		context.waitTicks(10);
		context.takeScreenshot("m07-art-engine-coupled");

		// Started from the ground, from the west: it runs east; a shot while it moves, then stopped.
		server.runCommand("tp @a 107.5 " + g + " " + (TRACK_Z + 2.5) + " 180 30");
		context.waitTicks(5);
		Vec3 at = server.computeOnServer(s -> engines(s.overworld(), g).getFirst().getBoundingBox().getCenter());
		server.runCommand("tp @a " + (at.x - 1.5) + " " + g + " " + (TRACK_Z + 1.5) + " -90 20");
		context.waitTicks(5);
		lookAtPoint(context, at);
		use(context);
		context.waitTicks(10);
		boolean running = server.computeOnServer(s -> CobblestoneEngine.engine(engines(s.overworld(), g).getFirst()).simpleBlock$running());
		Vec3 now = server.computeOnServer(s -> engines(s.overworld(), g).getFirst().position());
		server.runCommand("tp @a " + (now.x + 1.0) + " " + (g + 1.5) + " " + (TRACK_Z - 3.0) + " 20 25");
		context.waitTicks(3);
		context.takeScreenshot("m07-art-engine-moving");
		Vec3 stopAt = server.computeOnServer(s -> engines(s.overworld(), g).getFirst().getBoundingBox().getCenter());
		server.runCommand("tp @a " + (stopAt.x - 1.5) + " " + g + " " + (TRACK_Z + 1.5) + " -90 20");
		context.waitTicks(2);
		lookAtPoint(context, server.computeOnServer(s -> engines(s.overworld(), g).getFirst().getBoundingBox().getCenter()));
		use(context);

		// Standing engines on the curve and on the slope (summoned: creation is checked above).
		server.runCommand("summon minecraft:furnace_minecart 118.5 " + g + " " + (TRACK_Z + 0.5));
		server.runCommand("summon minecraft:furnace_minecart 118.5 " + (g + 0.5) + " " + (TRACK_Z + 3.5));
		context.waitTicks(20);
		server.runCommand("tp @a 121.5 " + (g + 1.5) + " " + (TRACK_Z - 1.5) + " 120 30");
		context.waitTicks(10);
		context.takeScreenshot("m07-art-engine-curve");
		server.runCommand("tp @a 121.0 " + (g + 1.5) + " " + (TRACK_Z + 4.0) + " 90 20");
		context.waitTicks(10);
		context.takeScreenshot("m07-art-engine-slope");
		List<String> poses = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(MinecartFurnace.class, new AABB(117, g - 1, TRACK_Z - 1, 120, g + 3, TRACK_Z + 6))
			.stream().map(e -> String.format("(%.2f, %.2f, %.2f) xRot %.0f", e.getX(), e.getY(), e.getZ(), e.getXRot())).toList());
		SimpleBlockTestLog.info("CLIENT ART ENGINE: furnace on a minecart by key: engine {}, drawn by the engine renderer {}; coupled to a freight cart {}; "
			+ "started from the ground: running {}, x {} -> {}; standing on the curve and the slope: {}", engine, rendered, coupled, running,
			String.format("%.2f", at.x), String.format("%.2f", now.x), poses);
		assertTrue(engine && rendered && coupled && running && now.x > at.x + 0.05, "engine made, drawn, coupled and running");
	}

	private static List<MinecartFurnace> engines(ServerLevel level, int g) {
		return level.getEntitiesOfClass(MinecartFurnace.class, new AABB(96, g - 1, TRACK_Z - 1, 117, g + 4, TRACK_Z + 1));
	}
}
