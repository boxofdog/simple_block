package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.workwear.TrailBlazerGhosts;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * The 3D ladder in a real client ({@code ladder3d-*} screenshots): every facing is drawn with the stile-and-rung
 * model on vanilla's ladder texture (checked through the client's own model set), a ladder column on a wall
 * from the front and the side, and Trail Blazer's outline of a ladder behind a wall tracing that model: six
 * boxes, every line inside the stiles' and rungs' bounds, turned with the ladder's facing.
 */
public class LadderModelClientTest implements FabricClientGameTest {
	private static final int X = 10;
	private static final int Z = -60;
	/** The model's boxes facing north, in pixels: two stiles (12 edges each) and four rungs (12 each). */
	private static final int OUTLINE_EDGES = 72;

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
			server.runCommand("fill " + (X - 10) + " " + g + " " + (Z - 10) + " " + (X + 10) + " " + (g + 8) + " " + (Z + 10) + " minecraft:air");
			world.getConnection().waitForChunksRender();
			run(context, server, g);
		}
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, String name, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch);
		context.waitTicks(8);
		context.takeScreenshot(name);
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int g) {
		// The client's model for every facing: more than vanilla's one two-sided sheet, on vanilla's texture.
		List<String> models = context.computeOnClient(client -> {
			List<String> out = new ArrayList<>();
			var set = client.getModelManager().getBlockStateModelSet();
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				BlockState state = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
				List<BlockStateModelPart> parts = new ArrayList<>();
				set.get(state).collectParts(RandomSource.create(0), parts);
				int quads = 0;
				for (BlockStateModelPart part : parts) {
					for (Direction side : Direction.values()) {
						quads += part.getQuads(side).size();
					}
					quads += part.getQuads(null).size();
				}
				String sprite = set.getParticleMaterial(state).sprite().contents().name().toString();
				out.add(facing.getSerializedName() + "=" + sprite + "/" + quads + (set.get(state) == set.missingModel() ? " MISSING" : ""));
			}
			return out;
		});
		SimpleBlockTestLog.info("CLIENT LADDER3D models: {}", models);
		// Stiles 6 faces each, rungs 4: 28 quads.
		assertTrue(models.size() == 4 && models.stream().allMatch(m -> m.endsWith("=minecraft:block/ladder/28")), "3D ladder models: " + models);

		// A ladder column up a wall (on its south face, so the ladders face south), and one round the corner.
		server.runCommand("fill " + (X - 3) + " " + g + " " + (Z - 1) + " " + (X + 3) + " " + (g + 5) + " " + (Z - 1) + " minecraft:stone_bricks");
		server.runCommand("fill " + X + " " + g + " " + Z + " " + X + " " + (g + 5) + " " + Z + " minecraft:ladder[facing=south]");
		server.runCommand("fill " + (X + 4) + " " + g + " " + (Z - 1) + " " + (X + 4) + " " + (g + 5) + " " + (Z - 1) + " minecraft:ladder[facing=east]");
		context.waitTicks(10);
		shot(context, server, "ladder3d-front", X + 0.5, g + 1.0, Z + 2.5, 180, -10);
		shot(context, server, "ladder3d-angle", X + 2.2, g + 1.4, Z + 1.3, 130, 5);
		shot(context, server, "ladder3d-side", X + 2.5, g + 2.0, Z + 0.4, 90, 10);
		shot(context, server, "ladder3d-close", X + 0.9, g + 1.6, Z + 0.9, 160, 30);

		// Trail Blazer: the column's ladders behind a wall are outlined by the model's boxes.
		server.runCommand("item replace entity @a armor.head with simple_block:headlamp");
		server.runCommand("item replace entity @a armor.chest with simple_block:overalls");
		server.runCommand("item replace entity @a armor.feet with simple_block:spiked_boots");
		server.runCommand("fill " + (X - 3) + " " + g + " " + (Z + 2) + " " + (X + 3) + " " + (g + 5) + " " + (Z + 2) + " minecraft:stone");
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 1.0) + " " + (Z + 5.5) + " 180 -10");
		context.waitTicks(40);
		BlockPos ladder = new BlockPos(X, g + 1, Z);
		String outline = context.computeOnClient(client -> {
			if (Workwear.bonus(client.player) != Workwear.Bonus.TRAIL_BLAZER) {
				return "no Trail Blazer";
			}
			for (TrailBlazerGhosts.Ghost ghost : TrailBlazerGhosts.ghosts()) {
				if (!ghost.pos().equals(ladder)) {
					continue;
				}
				// Facing south: the model turned half round, so the stiles stand at z 0-2/16 against the wall to the north.
				double minZ = 1;
				double maxZ = 0;
				boolean inside = true;
				for (Vec3[] line : ghost.lines()) {
					for (Vec3 v : line) {
						minZ = Math.min(minZ, v.z);
						maxZ = Math.max(maxZ, v.z);
						inside &= v.x >= 2 / 16.0 - 1e-4 && v.x <= 14 / 16.0 + 1e-4 && v.y >= -1e-4 && v.y <= 1 + 1e-4;
					}
				}
				return ghost.lines().size() + " lines, z " + String.format(java.util.Locale.ROOT, "%.3f-%.3f", minZ, maxZ) + ", inside the stiles " + inside;
			}
			return "no ghost";
		});
		context.takeScreenshot("ladder3d-trail-blazer");
		SimpleBlockTestLog.info("CLIENT LADDER3D: front, angle, side and close screenshots; Trail Blazer outline of a ladder behind a wall: {}", outline);
		assertTrue(outline.equals(OUTLINE_EDGES + " lines, z 0.000-0.125, inside the stiles true"), "ladder outline traces the 3D model: " + outline);
	}
}
