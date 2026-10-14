package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.InventoryScreenAddons;
import io.github.boxofdog.simpleblock.client.workwear.RearWarning;
import io.github.boxofdog.simpleblock.client.workwear.TrailBlazerGhosts;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.rope.RopeContent;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 workwear in a real client ({@code m10-*} screenshots), then over a dedicated server: Trail Blazer's
 * ghosts and their provenance colours, the Headlamp's light as the client sees it, the outfit worn
 * (lantern on the helmet, Overalls on the legs), Old Pro's rear warning (behind with a clear line: yes;
 * behind a wall: no; in front: no), and the locked, red-outlined armor slots.
 */
public class WorkwearClientTest implements FabricClientGameTest {
	private static final int X = 70;
	private static final int Z = 20;

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
			run(context, server, g, "m10");
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			run(context, server, g, "m10-server");
		}
	}

	private static int yard(TestServerContext server) {
		int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z));
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set midnight");
		server.runCommand("weather clear");
		server.runCommand("fill " + (X - 12) + " " + (g - 1) + " " + (Z - 14) + " " + (X + 12) + " " + (g - 1) + " " + (Z + 12) + " minecraft:stone");
		server.runCommand("fill " + (X - 12) + " " + g + " " + (Z - 14) + " " + (X + 12) + " " + (g + 8) + " " + (Z + 12) + " minecraft:air");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 0.5) + " 180 0");
		return g;
	}

	private static ServerPlayer playerOf(ServerLevel level) {
		return level.players().getFirst();
	}

	private static void use(ServerPlayer player, BlockPos pos, Direction face, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		player.gameMode.useItemOn(player, (ServerLevel) player.level(), held, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private static void wear(TestServerContext server, String head, String chest, String feet) {
		server.runCommand("item replace entity @a armor.head with " + head);
		server.runCommand("item replace entity @a armor.chest with " + chest);
		server.runCommand("item replace entity @a armor.feet with " + feet);
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		// Just after joining a dedicated server the commands can land before the player is ready: wear the
		// outfit until the client sees it.
		for (int i = 0; i < 10 && context.computeOnClient(client -> Workwear.bonus(client.player)) != Workwear.Bonus.TRAIL_BLAZER; i++) {
			wear(server, "simple_block:headlamp", "simple_block:overalls", "simple_block:spiked_boots");
			context.waitTicks(10);
		}
		// A wall north of the player; behind it a torch the player places, a torch and a ladder the "world" places, and a rope.
		server.runCommand("fill " + (X - 5) + " " + g + " " + (Z - 4) + " " + (X + 5) + " " + (g + 4) + " " + (Z - 4) + " minecraft:stone");
		BlockPos placedTorch = new BlockPos(X - 2, g, Z - 7);
		BlockPos worldTorch = new BlockPos(X + 2, g, Z - 7);
		BlockPos worldLadder = new BlockPos(X, g + 1, Z - 8);
		BlockPos ropeSupport = new BlockPos(X + 4, g + 4, Z - 7);
		server.runCommand("setblock " + worldTorch.getX() + " " + worldTorch.getY() + " " + worldTorch.getZ() + " minecraft:torch");
		server.runCommand("setblock " + worldLadder.getX() + " " + worldLadder.getY() + " " + (worldLadder.getZ() - 1) + " minecraft:stone");
		server.runCommand("setblock " + worldLadder.getX() + " " + worldLadder.getY() + " " + worldLadder.getZ() + " minecraft:ladder[facing=south]");
		server.runCommand("setblock " + ropeSupport.getX() + " " + ropeSupport.getY() + " " + ropeSupport.getZ() + " minecraft:stone");
		server.runOnServer(s -> {
			ServerPlayer player = playerOf(s.overworld());
			use(player, placedTorch.below(), Direction.UP, new ItemStack(Items.TORCH));
			use(player, ropeSupport, Direction.DOWN, new ItemStack(RopeContent.SPELUNKER_ROPE));
		});
		context.waitTicks(45);
		boolean[] colours = context.computeOnClient(client -> {
			List<TrailBlazerGhosts.Ghost> ghosts = TrailBlazerGhosts.ghosts();
			boolean placed = ghosts.stream().anyMatch(ghost -> ghost.pos().equals(placedTorch) && ghost.placed());
			boolean world = ghosts.stream().anyMatch(ghost -> ghost.pos().equals(worldTorch) && !ghost.placed());
			boolean ladder = ghosts.stream().anyMatch(ghost -> ghost.pos().equals(worldLadder) && !ghost.placed());
			boolean rope = ghosts.stream().anyMatch(ghost -> ghost.pos().equals(ropeSupport.below()) && ghost.placed());
			return new boolean[] {placed, world, ladder, rope, Workwear.bonus(client.player) == Workwear.Bonus.TRAIL_BLAZER};
		});
		int ghostCount = context.computeOnClient(client -> TrailBlazerGhosts.ghosts().size());
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.0, Z - 6.5));
		context.waitTicks(3);
		context.takeScreenshot(shots + "-trail-blazer");
		SimpleBlockTestLog.info("CLIENT M10 TRAIL BLAZER ({}): Trail Blazer {}; {} ghosts behind the wall: player's torch yellow {}, world torch blue {}, "
			+ "world ladder blue {}, rope yellow {}", shots, colours[4], ghostCount, colours[0], colours[1], colours[2], colours[3]);
		assertTrue(colours[4] && colours[0] && colours[1] && colours[2] && colours[3], "ghost provenance colours");

		// The Headlamp's light, as the client sees it, and the outfit worn.
		int[] light = context.computeOnClient(client -> {
			BlockPos head = BlockPos.containing(client.player.getEyePosition());
			return new int[] {client.level.getBlockState(head).is(Blocks.LIGHT) ? 1 : 0, client.level.getBrightness(LightLayer.BLOCK, head)};
		});
		lookAtPoint(context, new Vec3(X + 0.5, g, Z + 3.5));
		context.waitTicks(3);
		context.takeScreenshot(shots + "-headlamp-night");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(5);
		context.takeScreenshot(shots + "-outfit");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		SimpleBlockTestLog.info("CLIENT M10 HEADLAMP ({}): the client sees a light block at the head {}, block light {}", shots, light[0] == 1, light[1]);
		assertTrue(light[0] == 1 && light[1] >= 12, "the client should see the headlamp's light");

		// Old Pro's rear warning.
		wear(server, "simple_block:headlamp", "simple_block:overalls", "simple_block:mining_boots");
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 0.5) + " 180 0");
		server.runCommand("summon minecraft:zombie " + (X + 0.5) + " " + g + " " + (Z + 7.5) + " {NoAI:1b,PersistenceRequired:1b,Tags:[\"m10rear\"]}");
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.6, Z - 5.5));
		context.waitTicks(10);
		float behind = context.computeOnClient(client -> RearWarning.strength());
		context.takeScreenshot(shots + "-rear-warning");
		server.runCommand("fill " + (X - 3) + " " + g + " " + (Z + 4) + " " + (X + 3) + " " + (g + 3) + " " + (Z + 4) + " minecraft:stone");
		context.waitTicks(5);
		float walled = context.computeOnClient(client -> RearWarning.strength());
		server.runCommand("fill " + (X - 3) + " " + g + " " + (Z + 4) + " " + (X + 3) + " " + (g + 3) + " " + (Z + 4) + " minecraft:air");
		lookAtPoint(context, new Vec3(X + 0.5, g + 1.6, Z + 7.5));
		context.waitTicks(5);
		float ahead = context.computeOnClient(client -> RearWarning.strength());
		server.runCommand("kill @e[tag=m10rear]");
		SimpleBlockTestLog.info("CLIENT M10 REAR WARNING ({}): zombie 7 blocks behind, clear line {}; a wall between {}; turned to face it {}", shots,
			String.format("%.2f", behind), String.format("%.2f", walled), String.format("%.2f", ahead));
		assertTrue(behind > 0.3F && walled == 0.0F && ahead == 0.0F, "rear warning: behind " + behind + ", walled " + walled + ", ahead " + ahead);

		// The locked armor slots (Old Pro with 100 raw iron) and the bar under the panel (Overalls: the row is a backpack).
		server.runCommand("clear @a minecraft:raw_iron");
		server.runCommand("item replace entity @a hotbar.1 with minecraft:raw_iron 64");
		server.runCommand("item replace entity @a hotbar.2 with minecraft:raw_iron 36");
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(3);
		int locked = context.computeOnClient(client -> Workwear.lockedSlots(client.player).size());
		int[] bar = context.computeOnClient(client -> InventoryScreenAddons.barBounds((AbstractContainerScreen<?>) client.gui.screen()));
		int panelBottom = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			return ((io.github.boxofdog.simpleblock.client.mixin.AbstractContainerScreenAccessor) screen).simpleBlock$getTopPos()
				+ ((io.github.boxofdog.simpleblock.client.mixin.AbstractContainerScreenAccessor) screen).simpleBlock$getImageHeight();
		});
		context.takeScreenshot(shots + "-locked-slots");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(3);
		server.runCommand("clear @a minecraft:raw_iron");
		SimpleBlockTestLog.info("CLIENT M10 INVENTORY ({}): Old Pro carrying 100 raw iron: {} armor slots locked on the client; with the Overalls the load bar "
			+ "is under the panel (bar top {}, panel bottom {})", shots, locked, bar[1], panelBottom);
		assertTrue(locked == 4 && bar[1] > panelBottom, "locked slots and the bar under the panel");
	}
}
