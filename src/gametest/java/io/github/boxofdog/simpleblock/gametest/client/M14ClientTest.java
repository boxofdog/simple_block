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
 * with his iron pickaxe in hand, and mid-swing; his hit box 1.25 blocks tall.
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
