package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.coot.OldCoot;
import io.github.boxofdog.simpleblock.coot.OldCootContent;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * M14 the Old Coot in a real client ({@code m14-*} screenshots): spawned with his creative spawn egg by a real
 * use-key press; seen from the front, side, three-quarter and close up (the villager nose, the beard, the hat),
 * with his iron pickaxe in hand, and mid-swing; his hit box 1.25 blocks tall. Then at work: heard through rock
 * from a sealed booth while he digs out of sight (the client is sent his pickaxe blows and the stone breaking,
 * tied to blocks really gone); seen mid-swing at a stone face; seen swinging at the player he is angry with
 * (and the player hurt); and frame times with a dozen Coots in view against none.
 */
public class M14ClientTest implements FabricClientGameTest {
	private static final int X = 300;
	private static final int Z = 20;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			int g = server.computeOnServer(s -> {
				s.overworld().getChunk(X >> 4, Z >> 4);
				return s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
			});
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:spawn_monsters false");
			server.runCommand("time set noon");
			server.runCommand("fill " + (X - 8) + " " + (g - 1) + " " + (Z - 8) + " " + (X + 8) + " " + (g - 1) + " " + (Z + 8) + " minecraft:stone");
			server.runCommand("fill " + (X - 8) + " " + g + " " + (Z - 8) + " " + (X + 8) + " " + (g + 6) + " " + (Z + 8) + " minecraft:air");
			server.runCommand("gamemode creative @a");
			server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 4.5) + " 180 30");
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			appearance(context, server, g);
			work(context, server, g);
		}
	}

	private static void appearance(ClientGameTestContext context, TestServerContext server, int g) {
		List<String> log = new ArrayList<>();
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:old_coot_spawn_egg");
		context.waitTicks(3);
		String eggName = context.computeOnClient(client -> client.player.getMainHandItem().getHoverName().getString());
		boolean inTab = context.computeOnClient(client -> {
			// The tabs are filled when the creative inventory first opens; fill them as it would.
			net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(client.player.connection.enabledFeatures(), true,
				client.level.registryAccess());
			return net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB
				.getValueOrThrow(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS).getDisplayItems().stream()
				.anyMatch(stack -> stack.is(OldCootContent.SPAWN_EGG));
		});
		lookAtPoint(context, new Vec3(X + 0.5, g, Z + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		server.runCommand("clear @a");
		// Face him south, towards the camera, and keep him still for the pictures.
		server.runCommand("data merge entity @e[type=simple_block:old_coot,limit=1] {NoAI:1b,Rotation:[0f,0f]}");
		context.waitTicks(5);
		// Rotation turns only his head: square his body up with it too (it kept the egg's random facing).
		server.runOnServer(s -> s.overworld().getEntitiesOfClass(OldCoot.class, new AABB(X - 8, g - 2, Z - 8, X + 8, g + 6, Z + 8))
			.forEach(M14ClientTest::faceSouth));
		context.runOnClient(client -> client.level.getEntitiesOfClass(OldCoot.class, new AABB(X - 8, g - 2, Z - 8, X + 8, g + 6, Z + 8))
			.forEach(M14ClientTest::faceSouth));
		context.waitTicks(2);
		double[] box = context.computeOnClient(client -> {
			Entity coot = client.level.getEntitiesOfClass(OldCoot.class, new AABB(X - 8, g - 2, Z - 8, X + 8, g + 6, Z + 8)).getFirst();
			return new double[] {coot.getBbHeight(), coot.getBbWidth(), ((OldCoot) coot).getMainHandItem().is(net.minecraft.world.item.Items.IRON_PICKAXE) ? 1 : 0};
		});
		int coots = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(OldCoot.class, new AABB(X - 8, g - 2, Z - 8, X + 8, g + 6, Z + 8)).size());
		Object[][] views = {
			{"front", new Vec3(X + 0.5, g + 0.9, Z + 3.0)},
			{"face", new Vec3(X + 0.5, g + 1.0, Z + 1.8)},
			{"side", new Vec3(X + 3.0, g + 0.9, Z + 0.5)},
			{"three-quarter", new Vec3(X + 2.6, g + 1.6, Z + 2.6)},
		};
		for (Object[] view : views) {
			Vec3 eye = (Vec3) view[1];
			server.runCommand("tp @a " + eye.x + " " + (eye.y - 1.62) + " " + eye.z);
			context.waitTicks(5);
			lookAtPoint(context, new Vec3(X + 0.5, g + 0.75, Z + 0.5));
			context.waitTicks(3);
			context.takeScreenshot("m14-old-coot-" + view[0]);
		}
		// A swing, as when he mines or strikes: the pickaxe arm comes down.
		server.runOnServer(s -> s.overworld().getEntitiesOfClass(OldCoot.class, new AABB(X - 8, g - 2, Z - 8, X + 8, g + 6, Z + 8)).getFirst()
			.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true));
		context.waitTicks(3);
		float swing = context.computeOnClient(client -> client.level.getEntitiesOfClass(OldCoot.class,
			new AABB(X - 8, g - 2, Z - 8, X + 8, g + 6, Z + 8)).getFirst().getSwingAnimation(1.0F));
		context.takeScreenshot("m14-old-coot-swing");
		log.add("client swing progress at the swing picture " + swing);
		log.add("egg \"" + eggName + "\" in the spawn-egg tab " + inTab + "; a use-key press spawned " + coots + " Coot; hit box "
			+ box[0] + " tall, " + box[1] + " wide; iron pickaxe in hand " + (box[2] == 1));
		SimpleBlockTestLog.info("CLIENT M14 OLD COOT: {}", log);
		assertTrue(eggName.equals("Old Coot Spawn Egg") && inTab, "the egg: " + eggName + ", in the tab " + inTab);
		assertTrue(coots == 1 && Math.abs(box[0] - 1.25) < 1e-6 && box[2] == 1, "one Coot, 1.25 tall, pickaxe in hand: " + log);
		server.runCommand("kill @e[type=simple_block:old_coot]");
		server.runCommand("kill @e[type=item]");
	}

	/** Blocks this Coot has mined (read on the server). */
	private static int mined(TestServerContext server, int id) {
		return server.computeOnServer(s -> ((OldCoot) s.overworld().getEntity(id)).blocksMined);
	}

	private static void work(ClientGameTestContext context, TestServerContext server, int g) {
		List<String> log = new ArrayList<>();
		int rx = X + 30;
		// A block of rock with a room in it, and a sealed booth for the listener 9 blocks from the room's middle.
		server.runCommand("fill " + (rx - 12) + " " + (g - 1) + " " + (Z - 10) + " " + (rx + 10) + " " + (g + 4) + " " + (Z + 10) + " minecraft:stone");
		server.runCommand("fill " + (rx - 2) + " " + g + " " + (Z - 2) + " " + (rx + 2) + " " + (g + 2) + " " + (Z + 2) + " minecraft:air");
		server.runCommand("fill " + (rx - 9) + " " + g + " " + Z + " " + (rx - 9) + " " + (g + 1) + " " + Z + " minecraft:air");
		server.runCommand("tp @a " + (rx - 8.5) + " " + g + " " + (Z + 0.5) + " 90 0");
		context.waitTicks(10);
		int digger = server.computeOnServer(s -> {
			OldCoot coot = OldCootContent.OLD_COOT.spawn(s.overworld(), new net.minecraft.core.BlockPos(rx, g, Z),
				net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			coot.mineCooldown = 0;
			return coot.getId();
		});
		PlayedSounds.PLAYED.clear();
		int ticks = 0;
		while (mined(server, digger) < 3 && ticks < 2000) {
			context.waitTicks(20);
			ticks += 20;
		}
		boolean seen = context.computeOnClient(client -> {
			Entity coot = client.level.getEntitiesOfClass(OldCoot.class, new AABB(rx - 12, g - 2, Z - 12, rx + 12, g + 6, Z + 12)).getFirst();
			return client.player.hasLineOfSight(coot);
		});
		long hits = PlayedSounds.PLAYED.stream().filter(id -> id.getPath().equals("block.stone.hit") || id.getPath().equals("block.deepslate.hit")).count();
		long breaks = PlayedSounds.PLAYED.stream().filter(id -> id.getPath().equals("block.stone.break") || id.getPath().equals("block.deepslate.break")).count();
		int mined = mined(server, digger);
		log.add("out of sight (" + !seen + ") in " + ticks + " ticks he mined " + mined + " blocks; the listener's client played " + hits
			+ " pickaxe blows and " + breaks + " breaks");
		server.runCommand("kill @e[type=simple_block:old_coot]");
		server.runCommand("kill @e[type=item]");

		// Seen at work: a stone face in the open, the only solid thing at his feet's height within reach of his eye.
		server.runCommand("fill " + (X - 8) + " " + g + " " + (Z - 8) + " " + (X + 8) + " " + (g + 6) + " " + (Z + 8) + " minecraft:air");
		server.runCommand("fill " + (X - 1) + " " + g + " " + (Z - 3) + " " + (X + 1) + " " + (g + 2) + " " + (Z - 3) + " minecraft:stone");
		server.runCommand("tp @a " + (X + 4.5) + " " + g + " " + (Z - 1.5) + " 90 10");
		int miner = server.computeOnServer(s -> {
			OldCoot coot = OldCootContent.OLD_COOT.spawn(s.overworld(), new net.minecraft.core.BlockPos(X, g, Z - 1),
				net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			coot.mineCooldown = 0;
			return coot.getId();
		});
		boolean miningShot = false;
		for (int i = 0; i < 400 && !miningShot; i++) {
			context.waitTicks(1);
			miningShot = context.computeOnClient(client -> {
				Entity coot = client.level.getEntity(miner);
				return coot instanceof OldCoot c && c.getSwingAnimation(1.0F) > 0.15F && c.getSwingAnimation(1.0F) < 0.6F;
			});
		}
		if (miningShot) {
			lookAtPoint(context, new Vec3(X + 0.5, g + 0.9, Z - 1.5));
			context.takeScreenshot("m14-old-coot-mining");
		}
		log.add("mid-swing at the stone face: " + miningShot + " (" + mined(server, miner) + " blocks mined)");

		// Angry: he swings at the player and hurts them.
		server.runCommand("gamemode survival @a");
		server.runCommand("effect give @a minecraft:resistance 30 2 true");
		server.runCommand("tp @a " + (X + 2.5) + " " + g + " " + (Z - 1.5) + " 90 15");
		context.waitTicks(5);
		server.runOnServer(s -> serverCootOn(s, miner).provoke(s.getPlayerList().getPlayers().getFirst()));
		boolean attackShot = false;
		for (int i = 0; i < 200 && !attackShot; i++) {
			context.waitTicks(1);
			attackShot = context.computeOnClient(client -> {
				Entity coot = client.level.getEntity(miner);
				return coot instanceof OldCoot c && c.getSwingAnimation(1.0F) > 0.15F && c.getSwingAnimation(1.0F) < 0.6F;
			});
		}
		if (attackShot) {
			context.takeScreenshot("m14-old-coot-attack");
		}
		boolean hurt = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getLastHurtByMob() instanceof OldCoot);
		log.add("swinging at the player: " + attackShot + "; the player hurt by him: " + hurt);
		server.runCommand("gamemode creative @a");
		server.runCommand("effect clear @a");
		server.runCommand("kill @e[type=simple_block:old_coot]");
		server.runCommand("kill @e[type=item]");
		server.runCommand("fill " + (X - 8) + " " + g + " " + (Z - 8) + " " + (X + 8) + " " + (g + 6) + " " + (Z + 8) + " minecraft:air");

		// Frame times: none, then a dozen Coots in view.
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 7.5) + " 180 20");
		context.waitTicks(10);
		double[][] frames = new double[2][];
		for (int run = 0; run < 2; run++) {
			if (run == 1) {
				server.runOnServer(s -> {
					for (int i = 0; i < 12; i++) {
						OldCootContent.OLD_COOT.spawn(s.overworld(), new net.minecraft.core.BlockPos(X - 5 + i % 4 * 3, g, Z - 6 + i / 4 * 3),
							net.minecraft.world.entity.EntitySpawnReason.COMMAND);
					}
				});
				context.waitTicks(20);
			}
			FrameTimes.record();
			long t0 = System.nanoTime();
			context.waitTicks(200);
			double seconds = (System.nanoTime() - t0) / 1e9;
			frames[run] = FrameTimes.stop();
			log.add(String.format(java.util.Locale.ROOT, "%s Coots: %d frames in %.1f s (%.1f fps), render median %.2f ms, p95 %.2f ms, worst %.2f ms",
				run == 0 ? "no" : "12", (int) frames[run][0], seconds, frames[run][0] / seconds, frames[run][1], frames[run][2], frames[run][3]));
		}
		context.takeScreenshot("m14-old-coot-dozen");
		server.runCommand("kill @e[type=simple_block:old_coot]");
		server.runCommand("kill @e[type=item]");
		SimpleBlockTestLog.info("CLIENT M14 OLD COOT AT WORK: {}", log);
		assertTrue(!seen && mined >= 3 && hits >= mined * 3 && breaks >= 1, "heard working out of sight: " + log);
		assertTrue(miningShot && attackShot && hurt, "swings seen at work and in a fight: " + log);
	}

	private static OldCoot serverCootOn(net.minecraft.server.MinecraftServer s, int id) {
		return (OldCoot) s.overworld().getEntity(id);
	}

	private static void faceSouth(OldCoot coot) {
		coot.setYRot(0.0F);
		coot.setYHeadRot(0.0F);
		coot.setYBodyRot(0.0F);
		coot.yRotO = 0.0F;
		coot.yHeadRotO = 0.0F;
		coot.yBodyRotO = 0.0F;
	}
}
