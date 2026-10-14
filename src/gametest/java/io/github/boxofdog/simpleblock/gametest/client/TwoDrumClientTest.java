package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.cable.CableRoutes;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * M07 playtest change 2 in a real client ({@code m07twin-*} screenshots), then over a dedicated server:
 * a coal winch's cable run through two anchors onto a second, empty drum. The client draws the cable
 * ending on the second drum (no return wheel), shows its barrel wound and the winch's lever on it
 * without the motor; a right-click on that lever sets it to PULL from its end (FEED at the winch) and
 * hauls a gripping cart towards it.
 */
public class TwoDrumClientTest implements FabricClientGameTest {
	private static final int Z = 10;
	private static final BlockPos HOME = new BlockPos(20, 0, Z);
	private static final BlockPos FAR = new BlockPos(40, 0, Z);
	private static final int[] ANCHORS = {26, 32};
	private static final int ANCHOR_UP = 3;

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
			run(context, server, g, "m07twin");
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			run(context, server, g, "m07twin-server");
		}
	}

	private static int yard(TestServerContext server) {
		int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 30, Z));
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("fill 16 " + (g - 1) + " 4 44 " + (g - 1) + " 16 minecraft:stone");
		server.runCommand("fill 16 " + g + " 4 44 " + (g + 10) + " 16 minecraft:air");
		server.runCommand("fill 21 " + g + " " + Z + " 39 " + g + " " + Z + " minecraft:rail[shape=east_west]");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a 21.5 " + g + " 12.5 0 0");
		return g;
	}

	private static void use(ServerPlayer player, BlockPos pos, Direction face, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		player.gameMode.useItemOn(player, (ServerLevel) player.level(), held, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	/** A right-click on a drum's crank or lever. */
	private static void crank(ServerPlayer player, BlockPos pos) {
		BlockState state = player.level().getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		Vec3 hit = Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, 9.5 / 16.0, 0.5 + side.getStepZ() * 0.55);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, (ServerLevel) player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		BlockPos home = HOME.above(g);
		BlockPos far = FAR.above(g);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = level.players().getFirst();
			use(player, home.below(), Direction.UP, new ItemStack(CableContent.CABLE_DRUM_ITEM));
			use(player, home, Direction.UP, new ItemStack(CableContent.CABLE_COIL));
			use(player, home, Direction.UP, new ItemStack(Items.FURNACE));
			use(player, home, Direction.UP, new ItemStack(Items.COAL));
			use(player, home, Direction.UP, new ItemStack(Items.COAL));
			use(player, far.below(), Direction.UP, new ItemStack(CableContent.CABLE_DRUM_ITEM));
			for (int x : ANCHORS) {
				BlockPos beam = new BlockPos(x, g + ANCHOR_UP + 1, Z);
				level.setBlockAndUpdate(beam, Blocks.OAK_PLANKS.defaultBlockState());
				use(player, beam, Direction.DOWN, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
			}
		});
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = level.players().getFirst();
			CableHook hook = CableClientTest.hookOf(level, home);
			player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
			hook.tick();
			for (int x : ANCHORS) {
				use(player, new BlockPos(x, g + ANCHOR_UP, Z), Direction.UP, ItemStack.EMPTY);
			}
			use(player, far, Direction.UP, ItemStack.EMPTY);
		});
		context.waitTicks(10);
		boolean attached = server.computeOnServer(s -> far.equals(CableClientTest.drumOf(s.overworld(), home).endDrum()));
		assertTrue(attached, "the cable should end on the second drum");

		// What the client sees: the strand ends at the second drum's exit; its barrel is wound; it shows a lever, no motor.
		Vec3 farExit = CableRoutes.drumPoint(far, false);
		Object[] seen = context.computeOnClient(client -> {
			CableDrumBlockEntity first = (CableDrumBlockEntity) client.level.getBlockEntity(home);
			CableDrumBlockEntity second = (CableDrumBlockEntity) client.level.getBlockEntity(far);
			BlockState state = client.level.getBlockState(far);
			List<Vec3> points = first.points();
			return new Object[] {points.getLast().distanceTo(farExit) < 1.0E-6, state.getValue(CableDrumBlock.WOUND), second.showsLever(),
				state.getValue(CableDrumBlock.POWERED), first.endDrum() != null};
		});
		server.runCommand("tp @a 36.5 " + (g + 1) + " " + (Z + 4.5) + " 0 0");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(38.5, g + 1.5, Z + 0.5));
		context.waitTicks(3);
		context.takeScreenshot(shots + "-second-drum");
		SimpleBlockTestLog.info("CLIENT M07TWIN ({}): strand ends on the second drum {}, its barrel wound {}, lever shown {}, motor {}, client knows "
			+ "the end drum {}", shots, seen[0], seen[1], seen[2], seen[3], seen[4]);
		assertTrue((boolean) seen[0] && (boolean) seen[1] && (boolean) seen[2] && !(boolean) seen[3] && (boolean) seen[4],
			"the client should draw the cable onto the second drum and show its lever without the motor");

		// The second drum's lever: PULL from its end hauls a gripping cart towards it.
		server.runOnServer(s -> {
			Minecart cart = EntityTypes.MINECART.create(s.overworld(), EntitySpawnReason.EVENT);
			cart.snapTo(28.5, g + 0.0625, Z + 0.5, 90.0F, 0.0F);
			ConnectingRods.fitted(cart).simpleBlock$setRod(true);
			s.overworld().addFreshEntity(cart);
		});
		context.waitTicks(10);
		server.runOnServer(s -> crank(s.overworld().players().getFirst(), far));
		context.waitTicks(4);
		Object[] lever = context.computeOnClient(client -> new Object[] {((CableDrumBlockEntity) client.level.getBlockEntity(far)).lever(),
			((CableDrumBlockEntity) client.level.getBlockEntity(home)).lever()});
		context.waitTicks(30);
		double x = server.computeOnServer(s -> {
			List<Minecart> carts = s.overworld().getEntitiesOfClass(Minecart.class, new AABB(far).inflate(20.0, 3.0, 2.0));
			return carts.isEmpty() ? -1.0 : carts.getFirst().getX();
		});
		lookAtPoint(context, new Vec3(38.5, g + 1.5, Z + 0.5));
		context.takeScreenshot(shots + "-pulled-to-second-drum");
		SimpleBlockTestLog.info("CLIENT M07TWIN ({}): right-click on the second drum's lever: the client shows it at {} and the winch's at {}; cart x 28.5 -> "
			+ "{}", shots, lever[0], lever[1], String.format("%.2f", x));
		assertTrue(lever[0] == CableLines.Lever.PULL && lever[1] == CableLines.Lever.FEED, "levers mirrored: second " + lever[0] + ", winch " + lever[1]);
		assertTrue(x > 30.0, "the cart should be hauled towards the second drum: x " + x);
		server.runOnServer(s -> s.overworld().getEntitiesOfClass(Minecart.class, new AABB(far).inflate(25.0, 3.0, 2.0)).forEach(Minecart::discard));
	}
}
