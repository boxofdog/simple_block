package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.feature.CreeperRetaliation;
import io.github.boxofdog.simpleblock.feature.SpiderJockeys;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * M09 in a real client ({@code m09-*} screenshots in {@code build/run/clientGameTest/screenshots}):
 * <ul>
 *   <li>the jockey's ledge grab: a skeleton (no AI, so the test steers) riding a spider pushed up a
 *       wall under a one-block overhang; the client sees the synced grab pose while the pair is hauled
 *       out and up round the lip, and the skeleton drawn with its arms raised;</li>
 *   <li>creeper retaliation, at midnight (by day a creeper dissolves): the player on top of a planks pillar shoots a creeper (an arrow attributed to the
 *       player); it walks to the foot of the pillar and explodes there, and the client sees the
 *       pillar's base gone.</li>
 * </ul>
 * The ledge grab is repeated with a dedicated server ({@code m09-server-*}): the pose crosses a real
 * network connection. The yard, at ground level {@code g}: stone x 0..20, z 0..16; the wall x 7..13,
 * z 8..9, 10 high, its overhang at z 7, 4 above the ground.
 */
public class HostileClientTest implements FabricClientGameTest {
	private static Spider spider;
	private static Skeleton skeleton;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(5);
			client.options.simulationDistance().set(5);
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			int g = yard(server);
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			ledgeGrab(context, server, g, "m09-ledge");
			creeper(context, server, g);
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			ledgeGrab(context, server, g, "m09-server-ledge");
		}
	}

	private static int yard(TestServerContext server) {
		int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("fill 0 " + (g - 1) + " 0 20 " + (g - 1) + " 16 minecraft:stone");
		server.runCommand("fill 0 " + g + " 0 20 " + (g + 12) + " 16 minecraft:air");
		server.runCommand("fill 7 " + g + " 8 13 " + (g + 9) + " 9 minecraft:stone");
		server.runCommand("fill 7 " + (g + 4) + " 7 13 " + (g + 4) + " 7 minecraft:stone");
		server.runCommand("gamemode creative @a");
		return g;
	}

	private static void ledgeGrab(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		int started = SpiderJockeys.haulsStarted;
		int cleared = SpiderJockeys.haulsCleared;
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			spider = EntityTypes.SPIDER.create(level, EntitySpawnReason.EVENT);
			spider.snapTo(10.5, g, 7.25, 0.0F, 0.0F);
			spider.setPersistenceRequired();
			level.addFreshEntity(spider);
			skeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
			skeleton.snapTo(10.5, g, 5.5, 0.0F, 0.0F);
			skeleton.setPersistenceRequired();
			skeleton.setNoAi(true);
			// Daylight for the pictures: a helmet keeps it from burning.
			skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			level.addFreshEntity(skeleton);
			skeleton.startRiding(spider, true, true);
		});
		// Side on, a little away from the wall.
		server.runCommand("tp @a 3.5 " + (g + 2) + " 4.5 -90 10");
		context.waitTicks(5);
		lookAtPoint(context, new Vec3(10.5, g + 3.5, 7.0));
		int skeletonId = server.computeOnServer(s -> skeleton.getId());
		boolean seenGrab = false;
		boolean shotCleared = false;
		int grabTicks = 0;
		for (int tick = 0; tick < 240; tick++) {
			server.runOnServer(s -> spider.getMoveControl().setWantedPosition(10.5, g + 10, 8.5, 1.0));
			context.waitTick();
			boolean grabbing = context.computeOnClient(client -> client.level.getEntity(skeletonId) instanceof SpiderJockeys.Grabbing grab
				&& grab.simpleBlock$grabbing());
			if (grabbing) {
				grabTicks++;
				if (!seenGrab) {
					seenGrab = true;
					context.takeScreenshot(shots + "-grab");
				}
			}
			double y = server.computeOnServer(s -> spider.getY());
			if (seenGrab && !shotCleared && y >= g + 5) {
				shotCleared = true;
				context.takeScreenshot(shots + "-over-the-lip");
			}
		}
		context.takeScreenshot(shots + "-wall-top");
		double top = server.computeOnServer(s -> spider.getY()) - g;
		int haulsStarted = SpiderJockeys.haulsStarted - started;
		int haulsCleared = SpiderJockeys.haulsCleared - cleared;
		server.runOnServer(s -> {
			skeleton.discard();
			spider.discard();
		});
		SimpleBlockTestLog.info("CLIENT M09 LEDGE ({}): jockey pushed up a wall under a one-block overhang for 240 ticks; client saw the grab pose {} ticks; "
			+ "hauls started {}, cleared {}; spider ended {} blocks above the ground", shots, grabTicks, haulsStarted, haulsCleared, String.format("%.1f", top));
		assertTrue(seenGrab, "the client should see the skeleton's grab pose");
		assertTrue(haulsCleared >= 1, "the jockey should be hauled round the overhang");
		assertTrue(top >= 5, "the jockey should get above the overhang, ended " + top);
	}

	private static void creeper(ClientGameTestContext context, TestServerContext server, int g) {
		// A player's tower of planks (stone shrugs off a creeper by vanilla rules).
		server.runCommand("fill 3 " + g + " 3 3 " + (g + 6) + " 3 minecraft:oak_planks");
		server.runCommand("tp @a 3.5 " + (g + 7) + " 3.5 0 60");
		// Midnight: a creeper in daylight dissolves (M02). Night vision for the pictures.
		server.runCommand("time set midnight");
		server.runCommand("effect give @a minecraft:night_vision infinite 0 true");
		context.waitTicks(10);
		Creeper[] creeper = new Creeper[1];
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			creeper[0] = EntityTypes.CREEPER.create(level, EntitySpawnReason.EVENT);
			creeper[0].snapTo(16.5, g, 12.5, 0.0F, 0.0F);
			creeper[0].setPersistenceRequired();
			level.addFreshEntity(creeper[0]);
		});
		context.waitTicks(10);
		// Down from the top of the pillar, towards the side the creeper comes from.
		lookAtPoint(context, new Vec3(6.5, g, 6.5));
		boolean provoked = server.computeOnServer(s -> {
			Arrow arrow = new Arrow(s.overworld(), s.overworld().players().getFirst(), new ItemStack(Items.ARROW), null);
			creeper[0].hurtServer(s.overworld(), s.overworld().damageSources().arrow(arrow, s.overworld().players().getFirst()), 1.0F);
			return CreeperRetaliation.grudge(creeper[0]) != null;
		});
		boolean shotAtFoot = false;
		double[] footDistance = {-1};
		for (int tick = 0; tick < 400 && !server.computeOnServer(s -> creeper[0].isRemoved()); tick++) {
			if (tick == 20) {
				context.takeScreenshot("m09-creeper-coming");
			}
			if (!shotAtFoot && server.computeOnServer(s -> creeper[0].isIgnited())) {
				shotAtFoot = true;
				footDistance[0] = server.computeOnServer(s -> creeper[0].position().subtract(3.5, creeper[0].getY(), 3.5).horizontalDistance());
				context.takeScreenshot("m09-creeper-at-the-foot");
			}
			context.waitTick();
		}
		context.waitTicks(20);
		context.takeScreenshot("m09-creeper-after");
		BlockPos base = new BlockPos(3, g, 3);
		boolean baseGone = context.computeOnClient(client -> client.level.getBlockState(base).isAir());
		boolean removed = server.computeOnServer(s -> creeper[0].isRemoved());
		server.runCommand("time set noon");
		SimpleBlockTestLog.info("CLIENT M09 CREEPER: player on a 7-high planks pillar shot a creeper: provoked {}, ignited {} ({} blocks from the pillar's X/Z), "
			+ "exploded {}, client sees the pillar's base gone {}", provoked, shotAtFoot, String.format("%.2f", footDistance[0]), removed, baseGone);
		assertTrue(provoked && removed, "the creeper should be provoked and explode");
		assertTrue(shotAtFoot && footDistance[0] < 2.0, "it should ignite at the foot of the pillar: " + footDistance[0]);
		assertTrue(baseGone, "the client should see the pillar's base blown away");
	}
}
