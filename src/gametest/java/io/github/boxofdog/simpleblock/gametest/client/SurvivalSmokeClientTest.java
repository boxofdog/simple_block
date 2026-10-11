package io.github.boxofdog.simpleblock.gametest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.gametest.mixin.HudAccessor;

/**
 * End-to-end smoke test in the real game client: a freshly created survival world, driven through
 * simulated keyboard/mouse input, with screenshots saved under {@code build/run/clientGameTest/screenshots}.
 * The rules themselves are covered in detail by the server game tests; this checks that the player
 * experiences them as intended (feedback messages, the bed screen, normal movement).
 */
public class SurvivalSmokeClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule minecraft:advance_time true");
			int ground = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			server.runCommand("tp @a 0.5 " + ground + " 0.5 0 30");
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("smoke-fresh-survival-world");

			bed(context, server, ground);
			shulkerBox(context, server, ground);
			movement(context, server, ground);
			elytra(context, server, ground);
		}
	}

	/** SF-01: lie down at midnight, get vanilla's "can't pass this night" message, time keeps running, Leave Bed works. */
	private static void bed(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("time set midnight");
		server.runCommand("setblock 0 " + ground + " 2 minecraft:red_bed[facing=south,part=foot]");
		server.runCommand("setblock 0 " + ground + " 3 minecraft:red_bed[facing=south,part=head]");
		context.waitTicks(5);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(InBedChatScreen.class);
		long clockInBed = server.computeOnServer(s -> s.overworld().getOverworldClockTime());
		context.waitTicks(10);
		String message = overlayMessage(context);
		context.takeScreenshot("smoke-bed-at-midnight");
		assertTrue(Component.translatable("sleep.not_possible").getString().equals(message),
				"expected the 'no amount of rest' message, saw: " + message);

		context.waitTicks(150);
		long clockLater = server.computeOnServer(s -> s.overworld().getOverworldClockTime());
		boolean stillSleeping = context.computeOnClient(client -> client.player.isSleeping());
		BlockPos respawn = server.computeOnServer(s -> playerOf(s.overworld()).getRespawnConfig().respawnData().pos());
		SimpleBlockTestLog.info("CLIENT BED: message '{}', clock {} -> {} after 160 ticks in bed, still in bed: {}, respawn {}",
				message, clockInBed, clockLater, stillSleeping, respawn);
		assertTrue(stillSleeping, "player should still be in bed (no skip)");
		assertTrue(clockLater - clockInBed < 400, "clock should run normally, not jump to morning");
		assertTrue(respawn.equals(new BlockPos(0, ground, 3)), "respawn should be at the bed, was " + respawn);

		context.clickScreenButton("multiplayer.stopSleeping");
		context.waitTicks(10);
		assertTrue(!context.computeOnClient(client -> client.player.isSleeping()), "Leave Bed should get the player up");
		server.runCommand("fill 0 " + ground + " 2 0 " + ground + " 3 minecraft:air");
		server.runCommand("time set noon");
	}

	/** SF-04: right-clicking the ground with a shulker box does nothing except explain why. */
	private static void shulkerBox(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("clear @a");
		server.runCommand("give @a minecraft:red_shulker_box");
		context.waitTicks(5);
		BlockPos target = new BlockPos(0, ground - 1, 2);
		context.getInput().lookAt(target);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String message = overlayMessage(context);
		context.takeScreenshot("smoke-shulker-box-refused");
		boolean placed = server.computeOnServer(s -> !s.overworld().getBlockState(target.above()).isAir());
		int boxesHeld = context.computeOnClient(client -> client.player.getInventory().countItem(Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.RED)));
		SimpleBlockTestLog.info("CLIENT SHULKER: placed: {}, still holding: {}, message '{}'", placed, boxesHeld, message);
		assertTrue(!placed, "the shulker box was placed");
		assertTrue(boxesHeld == 1, "the shulker box should still be in the inventory");
		assertTrue(message.toLowerCase().contains("shulker"), "expected a shulker box message, saw: " + message);
		server.runCommand("clear @a");
	}

	/** ELY-03: walking, jumping and falling (with fall damage) are normal. */
	private static void movement(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 0");
		context.waitTicks(5);
		double startZ = context.computeOnClient(client -> client.player.getZ());
		context.getInput().holdKeyFor(options -> options.keyUp, 20);
		double walked = context.computeOnClient(client -> client.player.getZ()) - startZ;

		double groundY = context.computeOnClient(client -> client.player.getY());
		context.getInput().holdKeyFor(options -> options.keyJump, 3);
		double peak = groundY;
		for (int i = 0; i < 8; i++) {
			peak = Math.max(peak, context.computeOnClient(client -> client.player.getY()));
			context.waitTick();
		}

		server.runCommand("tp @a 0.5 " + (ground + 10) + " 0.5 0 0");
		// Wait for the teleport to arrive, then for the player to land back on the ground.
		context.waitFor(client -> client.player.getY() > ground + 5, 40);
		context.waitFor(client -> client.player.getY() < ground + 0.01 && client.player.onGround(), 200);
		context.waitTicks(5);
		float health = context.computeOnClient(client -> client.player.getHealth());
		SimpleBlockTestLog.info("CLIENT MOVEMENT: walked {} blocks in 20 ticks, jump height {}, health after 10-block fall {}",
				String.format("%.2f", walked), String.format("%.2f", peak - groundY), health);
		assertTrue(walked > 2.0, "player should walk forward");
		assertTrue(peak - groundY > 0.5, "player should jump");
		assertTrue(health < 20.0F, "a 10-block fall should still hurt");
		server.runCommand("effect give @a minecraft:instant_health 1 5");
	}

	/** SF-03: an elytra given by command does not let the player glide when jumping mid-air. */
	private static void elytra(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("item replace entity @a armor.chest with minecraft:elytra");
		server.runCommand("tp @a 0.5 " + (ground + 30) + " 0.5 0 0");
		context.waitTicks(4);
		boolean gliding = false;
		for (int attempt = 0; attempt < 3; attempt++) {
			context.getInput().pressKey(options -> options.keyJump);
			context.waitTicks(3);
			gliding |= context.computeOnClient(client -> client.player.isFallFlying());
			gliding |= server.computeOnServer(s -> playerOf(s.overworld()).isFallFlying());
		}

		context.takeScreenshot("smoke-elytra-jump-mid-air");
		SimpleBlockTestLog.info("CLIENT ELYTRA: wearing elytra, pressed jump 3 times mid-air, gliding at any point: {}", gliding);
		assertTrue(!gliding, "player glided with an elytra");
		server.runCommand("effect give @a minecraft:resistance 10 4");
		context.waitFor(client -> client.player.getY() < ground + 0.01 && client.player.onGround(), 300);
	}

	private static ServerPlayer playerOf(ServerLevel level) {
		return level.players().getFirst();
	}

	private static String overlayMessage(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			Component message = ((HudAccessor) client.gui.hud).simple_block$getOverlayMessage();
			return message == null ? "" : message.getString();
		});
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
