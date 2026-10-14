package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.client.cable.CableGeometry;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipContent;

/**
 * M07 playtest fixes in a real client ({@code m07pt-*} screenshots), then again over a dedicated
 * server connection ({@code m07pt-server-*}):
 * <ul>
 *   <li>BUG-005: a span climbing steeply from an anchor is drawn bent under the plank the anchor hangs
 *       from, so it never passes through it (the straight run would);</li>
 *   <li>BUG-003: a cart's connecting rod grips cable hung from anchors five blocks above the rail, and
 *       the client draws it stretched up to it;</li>
 *   <li>the heavy skip drop: a skip let fall from 8 blocks up lands, and the client is told to play its
 *       metallic clang on landing, not before.</li>
 * </ul>
 * The yard at ground level {@code g}: stone x 16..50, z 0..12; the drum at x 20, z 6; anchors under
 * planks at x 26 (2 up), 30 (7 up, so the span from x 26 climbs 5 in 4) and 34 and 40 (5 up); a track
 * under x 32..42.
 */
public class PlaytestFixesClientTest implements FabricClientGameTest {
	private static final int Z = 6;
	private static final BlockPos DRUM = new BlockPos(20, 0, Z);
	private static final int[][] ANCHORS = {{26, 2}, {30, 7}, {34, 5}, {40, 5}};

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(6);
			client.options.simulationDistance().set(6);
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			int g = yard(server);
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			run(context, server, g, "m07pt");
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			run(context, server, g, "m07pt-server");
		}
	}

	private static int yard(TestServerContext server) {
		int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 30, Z));
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("fill 16 " + (g - 1) + " 0 50 " + (g - 1) + " 12 minecraft:stone");
		server.runCommand("fill 16 " + g + " 0 50 " + (g + 12) + " 12 minecraft:air");
		server.runCommand("fill 32 " + g + " " + Z + " 42 " + g + " " + Z + " minecraft:rail[shape=east_west]");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a 21.5 " + g + " 8.5 0 0");
		return g;
	}

	private static void use(ServerPlayer player, BlockPos pos, Direction face, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		player.gameMode.useItemOn(player, (ServerLevel) player.level(), held, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		BlockPos drum = DRUM.above(g);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = level.players().getFirst();
			use(player, drum.below(), Direction.UP, new ItemStack(CableContent.CABLE_DRUM_ITEM));
			use(player, drum, Direction.UP, new ItemStack(CableContent.CABLE_COIL));
			for (int[] anchor : ANCHORS) {
				BlockPos beam = new BlockPos(anchor[0], g + anchor[1] + 1, Z);
				level.setBlockAndUpdate(beam, Blocks.OAK_PLANKS.defaultBlockState());
				use(player, beam, Direction.DOWN, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
			}
		});
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = level.players().getFirst();
			CableHook hook = CableClientTest.hookOf(level, drum);
			player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
			hook.tick();
			for (int[] anchor : ANCHORS) {
				use(player, new BlockPos(anchor[0], g + anchor[1], Z), Direction.UP, ItemStack.EMPTY);
			}
			use(player, new BlockPos(ANCHORS[3][0], g + ANCHORS[3][1], Z), Direction.UP, ItemStack.EMPTY);
		});
		context.waitTicks(10);
		int routed = server.computeOnServer(s -> CableClientTest.drumOf(s.overworld(), drum).route().size());
		assertTrue(routed == ANCHORS.length, "anchors routed: " + routed);

		// BUG-005: the climbing span from the low anchor, as the client draws it.
		BlockPos plank = new BlockPos(ANCHORS[0][0], g + ANCHORS[0][1] + 1, Z);
		AABB plankBox = new AABB(plank).deflate(0.01);
		boolean[] result = context.computeOnClient(client -> {
			CableDrumBlockEntity onClient = (CableDrumBlockEntity) client.level.getBlockEntity(drum);
			List<Vec3> points = onClient.points();
			List<Vec3> drawn = CableGeometry.drawnPath(client.level, points);
			return new boolean[] {crosses(points, plankBox), crosses(drawn, plankBox), drawn.size() > points.size()};
		});
		server.runCommand("tp @a 27.5 " + (g + 3) + " 1.5 0 15");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(27.5, g + 3.5, Z + 0.5));
		context.waitTicks(3);
		context.takeScreenshot(shots + "-incline-cable");
		SimpleBlockTestLog.info("CLIENT M07PT BUG-005 ({}): the straight run from the low anchor would pass through its plank {}; drawn with a bend {} it "
			+ "does {}", shots, result[0], result[2], result[1]);
		assertTrue(result[0], "control: the straight climbing run passes through the plank");
		assertTrue(result[2] && !result[1], "the drawn cable should bend under the plank, not pass through it");

		// BUG-003: a rod gripping the cable from anchors five blocks up.
		server.runOnServer(s -> {
			Minecart cart = EntityTypes.MINECART.create(s.overworld(), EntitySpawnReason.EVENT);
			cart.snapTo(37.5, g + 0.0625, Z + 0.5, 90.0F, 0.0F);
			ConnectingRods.fitted(cart).simpleBlock$setRod(true);
			s.overworld().addFreshEntity(cart);
		});
		context.waitTicks(10);
		float reach = context.computeOnClient(client -> {
			List<Minecart> carts = client.level.getEntitiesOfClass(Minecart.class, new AABB(new BlockPos(37, g, Z)).inflate(2.0));
			return carts.isEmpty() ? -1.0F : ConnectingRods.fitted(carts.getFirst()).simpleBlock$reach();
		});
		server.runCommand("tp @a 37.5 " + (g + 1) + " 0.5 0 -20");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(37.5, g + 3.0, Z + 0.5));
		context.waitTicks(3);
		context.takeScreenshot(shots + "-rod-five-up");
		SimpleBlockTestLog.info("CLIENT M07PT BUG-003 ({}): rod under cable from anchors 5 up: the client sees it gripping at {}", shots, reach);
		assertTrue(reach > 5.0F, "the rod should grip cable from anchors five up, reach " + reach);

		// The heavy skip drop: the clang is heard on landing, not before.
		Identifier clang = Identifier.withDefaultNamespace("block.anvil.land");
		PlayedSounds.PLAYED.clear();
		server.runOnServer(s -> {
			OreSkip skip = SkipContent.ORE_SKIP.create(s.overworld(), EntitySpawnReason.EVENT);
			skip.setFacing(Direction.NORTH);
			skip.setPos(46.5, g + 8.0, Z + 0.5);
			skip.setCargo(Items.RAW_IRON, 512);
			s.overworld().addFreshEntity(skip);
		});
		server.runCommand("tp @a 46.5 " + (g + 2) + " 0.5 0 0");
		lookAtPoint(context, new Vec3(46.5, g + 2.0, Z + 0.5));
		boolean heardEarly = false;
		int ticks = 0;
		boolean landed = false;
		for (; ticks < 100 && !landed; ticks++) {
			context.waitTick();
			double y = server.computeOnServer(s -> {
				List<OreSkip> skips = s.overworld().getEntitiesOfClass(OreSkip.class, new AABB(new BlockPos(46, g, Z)).inflate(1.0, 12.0, 1.0));
				return skips.isEmpty() ? -1.0 : skips.getFirst().getY();
			});
			landed = Math.abs(y - g) < 1.0E-3;
			if (!landed && PlayedSounds.PLAYED.contains(clang)) {
				heardEarly = true;
			}
		}
		context.waitTicks(5);
		boolean heard = PlayedSounds.PLAYED.contains(clang);
		context.takeScreenshot(shots + "-skip-landed");
		int cargo = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(OreSkip.class, new AABB(new BlockPos(46, g, Z)).inflate(1.0, 12.0, 1.0))
			.stream().mapToInt(OreSkip::count).sum());
		SimpleBlockTestLog.info("CLIENT M07PT DROP ({}): skip let fall 8 blocks landed after {} ticks; clang heard before landing {}, on landing {}; cargo {}",
			shots, ticks, heardEarly, heard, cargo);
		assertTrue(landed && heard && !heardEarly, "the client should hear the clang on landing only: landed " + landed + ", heard " + heard
			+ ", early " + heardEarly);
		assertTrue(cargo == 512, "cargo kept: " + cargo);
		server.runOnServer(s -> s.overworld().getEntitiesOfClass(OreSkip.class, new AABB(new BlockPos(46, g, Z)).inflate(2.0, 12.0, 2.0)).forEach(OreSkip::discard));
	}

	/** Whether any of the straight runs between consecutive {@code points} passes through {@code box}. */
	private static boolean crosses(List<Vec3> points, AABB box) {
		for (int i = 1; i < points.size(); i++) {
			Vec3 a = points.get(i - 1);
			Vec3 b = points.get(i);
			for (int k = 0; k <= 200; k++) {
				if (box.contains(a.lerp(b, k / 200.0))) {
					return true;
				}
			}
		}
		return false;
	}
}
