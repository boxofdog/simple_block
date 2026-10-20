package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.EngineSelector;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M15 in a real client ({@code m15-*} screenshots): the engine's rear lever at each detent; Space pressed
 * in a real client while riding the seat coupled to the engine stepping it (the client sees the lever move);
 * a two-wagon station: lids on capped hoppers (a lever off, a button) and none on an open one; the train
 * lined up by hand at SERVICE, berths two blocks apart, stops raised. Then over a dedicated server, the
 * client a remote player: Space from the seat steps the lever there too, and the client sees it.
 */
public class M15ClientTest implements FabricClientGameTest {
	private static final int X = 40;
	private static final int Z = 40;

	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> log = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			int g = server.computeOnServer(s -> {
				s.overworld().getChunk(X >> 4, Z >> 4);
				return s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
			});
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:spawn_monsters false");
			server.runCommand("time set noon");
			server.runCommand("gamemode creative @a");
			server.runCommand("fill " + (X - 4) + " " + (g - 1) + " " + (Z - 6) + " " + (X + 24) + " " + (g + 6) + " " + (Z + 6) + " minecraft:air");
			server.runCommand("fill " + (X - 4) + " " + (g - 2) + " " + (Z - 6) + " " + (X + 24) + " " + (g - 2) + " " + (Z + 6) + " minecraft:stone");
			server.runOnServer(s -> build(s, g));
			server.runCommand("tp @a " + (X + 2.5) + " " + g + " " + (Z + 3.5) + " 0 20");
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			int[] ids = server.computeOnServer(s -> train(s, g));
			int engineId = ids[0];
			int seatId = ids[1];
			context.waitTicks(10);

			// The lever at each detent, from behind the engine.
			for (EngineSelector.Mode mode : EngineSelector.Mode.values()) {
				server.runOnServer(s -> EngineSelector.set(engine(s, engineId), mode));
				Vec3 lever = server.computeOnServer(s -> EngineSelector.fromModel(engine(s, engineId), EngineSelector.LEVER_X, EngineSelector.LEVER_Y + 4.0,
					EngineSelector.LEVER_Z));
				Vec3 behind = server.computeOnServer(s -> {
					MinecartFurnace engine = engine(s, engineId);
					Vec3 out = EngineSelector.fromModel(engine, 32.0, 0.0, 8.0).subtract(engine.position()).normalize();
					return lever.add(out.scale(1.6)).add(0.0, 0.4, 0.0);
				});
				server.runCommand("tp @a " + behind.x + " " + (behind.y - 1.62) + " " + behind.z);
				context.waitTicks(5);
				lookAtPoint(context, lever.add(0.0, -0.1, 0.0));
				context.waitTicks(3);
				String seen = context.computeOnClient(client -> client.level.getEntity(engineId) instanceof MinecartFurnace engine
					? EngineSelector.mode(engine).name() : "none");
				log.add("lever " + mode + ": the client shows " + seen);
				context.takeScreenshot("m15-lever-" + mode.name().toLowerCase(java.util.Locale.ROOT));
				assertTrue(seen.equals(mode.name()), "the client sees the lever at " + mode + ": " + seen);
			}
			server.runOnServer(s -> EngineSelector.set(engine(s, engineId), EngineSelector.Mode.THROUGH));

			// Space, pressed in the client while riding the seat coupled to the engine.
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().startRiding(s.overworld().getEntity(seatId)));
			context.waitTicks(10);
			List<String> steps = new ArrayList<>();
			for (int press = 0; press < 3; press++) {
				// A real press: held a few ticks (the client reads Space as held, not as a click), then let go.
				context.getInput().holdKey(options -> options.keyJump);
				context.waitTicks(4);
				context.getInput().releaseKey(options -> options.keyJump);
				context.waitTicks(5);
				steps.add(server.computeOnServer(s -> EngineSelector.mode(engine(s, engineId)).name()) + "/"
					+ context.computeOnClient(client -> client.level.getEntity(engineId) instanceof MinecartFurnace engine ? EngineSelector.mode(engine).name() : "none"));
			}
			log.add("Space from the seat (server/client): " + steps);
			assertTrue(steps.equals(List.of("NEUTRAL/NEUTRAL", "SERVICE/SERVICE", "THROUGH/THROUGH")), "Space steps the lever once a press: " + steps);
			server.runCommand("execute as @a run ride @s dismount");
			context.waitTicks(5);

			// The station: lids, and the train lined up by hand at SERVICE.
			server.runCommand("tp @a " + (X + 15.5) + " " + (g + 2) + " " + (Z + 4.5) + " 150 30");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(X + 13.5, g + 0.5, Z + 0.5));
			context.waitTicks(5);
			context.takeScreenshot("m15-station-lids");
			server.runOnServer(s -> EngineSelector.set(engine(s, engineId), EngineSelector.Mode.SERVICE));
			context.waitTicks(10);
			String docked = server.computeOnServer(s -> {
				MinecartFurnace engine = engine(s, engineId);
				List<String> cars = new ArrayList<>();
				for (var car : Couplings.consist(engine)) {
					cars.add(String.format(java.util.Locale.ROOT, "%.1f%s", car.getX() - X, Couplings.coupled(car).simpleBlock$dock() != null ? "D" : ""));
				}
				return cars + " stops " + s.overworld().getBlockState(new BlockPos(X + 14, g, Z)).getValue(LoadingTrackBlock.RAISED) + "/"
					+ s.overworld().getBlockState(new BlockPos(X + 12, g, Z)).getValue(LoadingTrackBlock.RAISED);
			});
			log.add("SERVICE by hand over the station: " + docked);
			context.takeScreenshot("m15-station-docked");
			assertTrue(docked.contains("14.5D") && docked.contains("12.5D") && docked.endsWith("true/true"), "lined up and docked: " + docked);
		}

		// A dedicated server: Space from the seat over the network.
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z));
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("fill " + (X - 4) + " " + (g - 1) + " " + (Z - 6) + " " + (X + 24) + " " + (g + 6) + " " + (Z + 6) + " minecraft:air");
			server.runCommand("fill " + (X - 4) + " " + (g - 2) + " " + (Z - 6) + " " + (X + 24) + " " + (g - 2) + " " + (Z + 6) + " minecraft:stone");
			server.runOnServer(s -> build(s, g));
			server.runCommand("tp @a " + (X + 2.5) + " " + g + " " + (Z + 3.5) + " 0 20");
			connection.waitForChunksRender();
			int[] ids = server.computeOnServer(s -> train(s, g));
			context.waitTicks(10);
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().startRiding(s.overworld().getEntity(ids[1])));
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(4);
			context.getInput().releaseKey(options -> options.keyJump);
			context.waitTicks(10);
			String onServer = server.computeOnServer(s -> EngineSelector.mode(engine(s, ids[0])).name());
			String onClient = context.computeOnClient(client -> client.level.getEntitiesOfClass(MinecartFurnace.class,
				new AABB(X - 8, g - 2, Z - 4, X + 24, g + 4, Z + 4)).stream().findFirst().map(engine -> EngineSelector.mode(engine).name()).orElse("none"));
			log.add("dedicated server: Space from the seat -> server " + onServer + ", remote client " + onClient);
			SimpleBlockTestLog.info("CLIENT M15: {}", log);
			assertTrue(onServer.equals("NEUTRAL") && onClient.equals("NEUTRAL"), "Space over the network: " + log);
		}
	}

	private static MinecartFurnace engine(MinecraftServer server, int id) {
		return (MinecartFurnace) server.overworld().getEntity(id);
	}

	/** Track along z = Z from x X to X + 20; Loading Rails at X + 12 and X + 14 (facing east); hoppers and controls. */
	private static void build(MinecraftServer server, int g) {
		ServerLevel level = server.overworld();
		for (int x = X; x <= X + 20; x++) {
			level.setBlockAndUpdate(new BlockPos(x, g - 1, Z), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(x, g, Z), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
		}
		for (int x : new int[] {X + 12, X + 14}) {
			level.setBlockAndUpdate(new BlockPos(x, g, Z), TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, Direction.EAST)
				.setValue(LoadingTrackBlock.SHAPE, RailShape.EAST_WEST));
		}
		// Over the front rail: a supplying hopper capped by a lever off; beside it, one receiving hopper with a
		// button (capped till pressed); beside the rear rail, an open receiving hopper (no control).
		BlockPos supply = new BlockPos(X + 14, g + 2, Z);
		level.setBlockAndUpdate(supply, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		((HopperBlockEntity) level.getBlockEntity(supply)).setItem(0, new ItemStack(Items.RAW_IRON, 16));
		level.setBlockAndUpdate(supply.north(2), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(supply.north(), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.SOUTH));
		BlockPos buttoned = new BlockPos(X + 14, g - 1, Z + 1);
		level.setBlockAndUpdate(buttoned, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		level.setBlockAndUpdate(buttoned.south(2), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(buttoned.south(), Blocks.STONE_BUTTON.defaultBlockState().setValue(net.minecraft.world.level.block.ButtonBlock.FACE, AttachFace.WALL)
			.setValue(net.minecraft.world.level.block.ButtonBlock.FACING, Direction.NORTH));
		level.setBlockAndUpdate(new BlockPos(X + 12, g - 1, Z + 1), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
	}

	/** An engine (front, east), a seat coupled to it, two wagons behind: ids of the engine and the seat. */
	private static int[] train(MinecraftServer server, int g) {
		ServerLevel level = server.overworld();
		double[] xs = {X + 14.3, X + 12.7, X + 11.1, X + 9.5};
		List<Minecart> cars = new ArrayList<>();
		MinecartFurnace engine = net.minecraft.world.entity.EntityTypes.FURNACE_MINECART.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		engine.snapTo(xs[3], g + 0.0625, Z + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(engine);
		CobblestoneEngine.engine(engine).simpleBlock$setFuel(CobblestoneEngine.FUEL_PER_ITEM);
		for (int i = 0; i < 3; i++) {
			Minecart cart = net.minecraft.world.entity.EntityTypes.MINECART.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			cart.snapTo(xs[i], g + 0.0625, Z + 0.5, 0.0F, 0.0F);
			level.addFreshEntity(cart);
			cars.add(cart);
		}
		// Front to back: wagon (x 14.3), wagon (12.7), seat (11.1), engine (9.5): the engine pushes from behind.
		MinecartCargo.cart(cars.get(1)).simpleBlock$setCargo(Items.RAW_COPPER, 40);
		MinecartCargo.cart(cars.get(2)).simpleBlock$setCushion(DyeColor.RED);
		link(cars.get(0), cars.get(1));
		link(cars.get(1), cars.get(2));
		link(cars.get(2), engine);
		return new int[] {engine.getId(), cars.get(2).getId()};
	}

	private static void link(net.minecraft.world.entity.vehicle.minecart.AbstractMinecart a, net.minecraft.world.entity.vehicle.minecart.AbstractMinecart b) {
		Couplings.coupled(a).simpleBlock$setCoupling(Couplings.coupled(a).simpleBlock$coupling(0) == null ? 0 : 1, b.getUUID());
		Couplings.coupled(b).simpleBlock$setCoupling(Couplings.coupled(b).simpleBlock$coupling(0) == null ? 0 : 1, a.getUUID());
	}
}
