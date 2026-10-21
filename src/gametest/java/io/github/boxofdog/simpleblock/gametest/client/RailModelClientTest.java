package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * I-013 in a real client ({@code rail3d-*} screenshots): every shape of the ordinary rail, and the Loading
 * Rail folded and raised, is drawn with the 3D models on vanilla's rail texture (checked through the
 * client's own model set; the Loading Rail's stop alone keeps its own texture), and the track
 * meets the Loading Rail flush: a straight run through a Loading Rail, curves, a slope up and down,
 * and a cart rolling round a loop (the rails' behaviour is vanilla's, covered by the server tests).
 */
public class RailModelClientTest implements FabricClientGameTest {
	private static final int X = 40;
	private static final int Z = -30;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			int g = server.computeOnServer(s -> {
				s.overworld().getChunk(X >> 4, Z >> 4);
				return s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
			});
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("fill " + (X - 10) + " " + (g - 1) + " " + (Z - 10) + " " + (X + 10) + " " + (g - 1) + " " + (Z + 10) + " minecraft:stone");
			server.runCommand("fill " + (X - 10) + " " + g + " " + (Z - 10) + " " + (X + 10) + " " + (g + 6) + " " + (Z + 10) + " minecraft:air");
			world.getConnection().waitForChunksRender();
			run(context, server, g);
		}
	}

	private static void rail(TestServerContext server, int x, int y, int z, String shape) {
		server.runCommand("setblock " + x + " " + y + " " + z + " minecraft:rail[shape=" + shape + "]");
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, String name, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch);
		context.waitTicks(8);
		context.takeScreenshot(name);
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int g) {
		// The client's model for every shape is the 3D one, on vanilla's rail texture; the Loading Rail's too.
		List<String> models = context.computeOnClient(client -> {
			List<String> out = new ArrayList<>();
			for (RailShape shape : RailShape.values()) {
				BlockState state = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
				var model = client.getModelManager().getBlockStateModelSet().get(state);
				String sprite = client.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite().contents().name().toString();
				boolean missing = model == client.getModelManager().getBlockStateModelSet().missingModel();
				out.add(shape.getSerializedName() + "=" + sprite + (missing ? " MISSING" : ""));
			}
			for (boolean raised : new boolean[] {false, true}) {
				BlockState state = io.github.boxofdog.simpleblock.train.TrainContent.LOADING_TRACK.defaultBlockState()
					.setValue(io.github.boxofdog.simpleblock.train.LoadingTrackBlock.RAISED, raised);
				String sprite = client.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite().contents().name().toString();
				out.add((raised ? "loading_raised" : "loading_folded") + "=" + sprite);
			}
			return out;
		});
		SimpleBlockTestLog.info("CLIENT RAIL3D models: {}", models);
		assertTrue(models.size() == 12 && models.stream().allMatch(m -> m.endsWith("=minecraft:block/rail")), "3D rail models: " + models);

		// A straight north-south run with a Loading Rail in it, and an east-west one.
		for (int z = Z - 6; z <= Z + 2; z++) {
			if (z == Z - 2) {
				server.runCommand("setblock " + X + " " + g + " " + z + " simple_block:loading_track[facing=north,shape=north_south]");
			} else {
				rail(server, X, g, z, "north_south");
			}
		}
		for (int x = X + 2; x <= X + 7; x++) {
			if (x == X + 4) {
				server.runCommand("setblock " + x + " " + g + " " + (Z - 6) + " simple_block:loading_track[facing=east,shape=east_west]");
			} else {
				rail(server, x, g, Z - 6, "east_west");
			}
		}
		context.waitTicks(10);
		shot(context, server, "rail3d-joint-low", X + 0.5, g + 0.4, Z + 3.5, 180, 8);
		shot(context, server, "rail3d-joint-above", X + 2.5, g + 3.0, Z - 0.5, 150, 50);
		shot(context, server, "rail3d-loading-close", X + 1.3, g + 1.0, Z - 0.6, 135, 45);

		// A loop of curves and straights, with a slope up and down on its east side.
		int lx = X - 7;
		int lz = Z + 3;
		rail(server, lx, g, lz, "south_east");
		rail(server, lx + 1, g, lz, "east_west");
		rail(server, lx + 2, g, lz, "east_west");
		rail(server, lx + 3, g, lz, "south_west");
		rail(server, lx, g, lz + 1, "north_south");
		rail(server, lx + 3, g, lz + 1, "ascending_south");
		server.runCommand("setblock " + (lx + 3) + " " + g + " " + (lz + 2) + " minecraft:stone");
		rail(server, lx + 3, g + 1, lz + 2, "north_south");
		server.runCommand("setblock " + (lx + 3) + " " + g + " " + (lz + 3) + " minecraft:stone");
		rail(server, lx + 3, g + 1, lz + 3, "north_south");
		rail(server, lx + 3, g, lz + 4, "ascending_north");
		rail(server, lx, g, lz + 2, "north_south");
		rail(server, lx, g, lz + 3, "north_south");
		rail(server, lx, g, lz + 4, "north_south");
		rail(server, lx, g, lz + 5, "north_east");
		rail(server, lx + 1, g, lz + 5, "east_west");
		rail(server, lx + 2, g, lz + 5, "east_west");
		rail(server, lx + 3, g, lz + 5, "north_west");
		context.waitTicks(10);
		shot(context, server, "rail3d-loop-above", lx + 1.5, g + 6.0, lz - 1.5, 0, 60);
		shot(context, server, "rail3d-curve-close", lx + 2.5, g + 1.2, lz - 1.5, 45, 35);
		shot(context, server, "rail3d-slope-side", lx + 6.5, g + 1.0, lz + 3.0, 90, 10);

		// Vanilla behaviour unchanged: the shapes the server keeps, after placing, are the ones set.
		String shapes = server.computeOnServer(s -> {
			List<String> out = new ArrayList<>();
			for (BlockPos pos : List.of(new BlockPos(lx, g, lz), new BlockPos(lx + 3, g, lz + 1), new BlockPos(lx + 3, g, lz + 5))) {
				out.add(s.overworld().getBlockState(pos).getValue(RailBlock.SHAPE).getSerializedName());
			}
			return out.toString();
		});
		SimpleBlockTestLog.info("CLIENT RAIL3D: joint, loop, curve and slope screenshots; shapes kept {}", shapes);
	}
}
