package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.effigy.EffigyPlacementScreen;
import io.github.boxofdog.simpleblock.effigy.Effigies;
import io.github.boxofdog.simpleblock.effigy.EffigyBlockEntity;
import io.github.boxofdog.simpleblock.effigy.EffigyContent;
import io.github.boxofdog.simpleblock.effigy.EffigyPlacementMenu;
import io.github.boxofdog.simpleblock.effigy.EffigyType;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * M12 in a real client ({@code m12-*} screenshots): the effigy item's tooltip; a real use-key press
 * opens the confirmation screen (its warning and buttons), Cancel changes nothing, Place puts the
 * effigy facing the way the player looked (a non-cardinal angle), the client's block entity holding the
 * same angle; the four figures' models (the client's own model set) and the four effigies turned to
 * other angles; zombies fleeing a cone, the client seeing them sprint (vanilla's sprint dust). The
 * confirmation, placement and fear repeated with a dedicated server ({@code m12-server-*}).
 */
public class EffigyClientTest implements FabricClientGameTest {
	private static final int X = 160;
	private static final int Z = 160;

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
			tooltip(context);
			confirmAndPlace(context, server, g, "m12");
			figures(context, server, g);
			fear(context, server, g, "m12");
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			confirmAndPlace(context, server, g, "m12-server");
			fear(context, server, g, "m12-server");
		}
	}

	private static int yard(TestServerContext server) {
		int g = server.computeOnServer(s -> {
			s.overworld().getChunk(X >> 4, Z >> 4);
			return s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
		});
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("difficulty normal");
		server.runCommand("fill " + (X - 24) + " " + (g - 1) + " " + (Z - 24) + " " + (X + 24) + " " + (g - 1) + " " + (Z + 24) + " minecraft:grass_block");
		server.runCommand("fill " + (X - 24) + " " + g + " " + (Z - 24) + " " + (X + 24) + " " + (g + 10) + " " + (Z + 24) + " minecraft:air");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z - 6.5) + " 0 20");
		return g;
	}

	private static void tooltip(ClientGameTestContext context) {
		List<String> lines = context.computeOnClient(client -> {
			List<String> out = new ArrayList<>();
			for (EffigyType type : EffigyType.values()) {
				Item item = EffigyContent.effigyItem(type);
				List<Component> tooltip = new ItemStack(item).getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
				out.add(tooltip.stream().map(Component::getString).reduce((a, b) -> a + " | " + b).orElse(""));
			}
			return out;
		});
		SimpleBlockTestLog.info("CLIENT M12 TOOLTIPS: {}", lines);
		assertTrue(lines.get(0).equals("Zombie Effigy | Scares away Zombies in a 100-block conical radius"), "zombie tooltip: " + lines.get(0));
		assertTrue(lines.get(3).equals("Enderman Effigy | Scares away Endermen in a 100-block conical radius"), "enderman tooltip: " + lines.get(3));
	}

	private static boolean effigyAt(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(s -> s.overworld().getBlockEntity(pos) instanceof EffigyBlockEntity);
	}

	private static void confirmAndPlace(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:zombie_effigy");
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z - 6.5) + " 0 20");
		context.waitTicks(5);
		// A spot ahead and to the right, within reach: the look is off the four sides (about 31 degrees).
		BlockPos spot = new BlockPos(X - 1, g, Z - 4);
		lookAtPoint(context, new Vec3(spot.getX() + 0.5, g - 0.02, spot.getZ() + 0.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(EffigyPlacementScreen.class);
		context.waitTicks(5);
		String title = context.computeOnClient(client -> ((EffigyPlacementScreen) client.gui.screen()).getTitle().getString());
		EffigyType shownType = context.computeOnClient(client -> ((EffigyPlacementScreen) client.gui.screen()).getMenu().type());
		context.takeScreenshot(shots + "-confirm-screen");
		boolean before = effigyAt(server, spot);
		context.clickScreenButton("gui.cancel");
		context.waitTicks(5);
		boolean afterCancel = effigyAt(server, spot);
		int heldAfterCancel = server.computeOnServer(s -> s.overworld().players().getFirst().getMainHandItem().getCount());
		boolean closed = context.computeOnClient(client -> client.gui.screen() == null);

		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(EffigyPlacementScreen.class);
		context.waitTicks(3);
		float clientYaw = context.computeOnClient(client -> client.player.getYRot());
		context.clickScreenButton("simple_block.effigy.place");
		context.waitTicks(10);
		float serverYaw = server.computeOnServer(s -> s.overworld().getBlockEntity(spot) instanceof EffigyBlockEntity e ? e.yaw() : Float.NaN);
		float seenYaw = context.computeOnClient(client -> client.level.getBlockEntity(spot) instanceof EffigyBlockEntity e ? e.yaw() : Float.NaN);
		boolean used = server.computeOnServer(s -> s.overworld().players().getFirst().getMainHandItem().isEmpty());
		server.runCommand("tp @a " + (spot.getX() - 3.5) + " " + (g + 1) + " " + (spot.getZ() - 3.5) + " -40 15");
		context.waitTicks(10);
		context.takeScreenshot(shots + "-placed");
		SimpleBlockTestLog.info("CLIENT M12 CONFIRM ({}): use-key opened '{}' ({}); cancel: effigy {} -> {}, item {} kept, screen closed {}; place at the "
			+ "player's look {}: server angle {}, client angle {}, item used {}", shots, title, shownType, before, afterCancel, heldAfterCancel, closed,
			clientYaw, serverYaw, seenYaw, used);
		assertTrue(shownType == EffigyType.ZOMBIE && title.equals("Zombie Effigy"), "screen for the zombie effigy: " + title + " " + shownType);
		assertTrue(!before && !afterCancel && heldAfterCancel == 1 && closed, "cancel changes nothing");
		assertTrue(Math.abs(serverYaw - clientYaw) < 0.01F && Math.abs(seenYaw - serverYaw) < 0.01F, "angle: look " + clientYaw + ", server "
			+ serverYaw + ", client " + seenYaw);
		assertTrue(Math.abs(Math.floorMod((int) serverYaw, 90)) > 5 && Math.abs(Math.floorMod((int) serverYaw, 90)) < 85, "a non-cardinal angle: " + serverYaw);
		assertTrue(used, "item used up");
		server.runCommand("setblock " + spot.getX() + " " + spot.getY() + " " + spot.getZ() + " minecraft:air");
	}

	private static void figures(ClientGameTestContext context, TestServerContext server, int g) {
		List<String> models = context.computeOnClient(client -> {
			List<String> out = new ArrayList<>();
			var set = client.getModelManager().getBlockStateModelSet();
			for (EffigyType type : EffigyType.values()) {
				var figure = EffigyContent.FIGURE_MODEL.figure(type);
				boolean missing = set.get(figure) == set.missingModel();
				out.add(type.getSerializedName() + "=" + set.getParticleMaterial(figure).sprite().contents().name() + (missing ? " MISSING" : "")
					+ " block=" + set.getParticleMaterial(EffigyContent.effigyBlock(type).defaultBlockState()).sprite().contents().name()
					+ " compressed=" + set.getParticleMaterial(EffigyContent.compressedBlock(type).defaultBlockState()).sprite().contents().name());
			}
			return out;
		});
		SimpleBlockTestLog.info("CLIENT M12 MODELS: {}", models);
		for (int i = 0; i < 4; i++) {
			String name = EffigyType.values()[i].getSerializedName();
			assertTrue(models.get(i).equals(name + "=simple_block:block/effigy/" + name + "_effigy block=simple_block:block/effigy/" + name
				+ "_effigy compressed=simple_block:block/" + name + "_block"), "models: " + models.get(i));
		}
		// The four in a row facing the camera (south), then each turned to another angle.
		float[] yaws = {180.0F, 180.0F, 180.0F, 180.0F};
		row(context, server, g, yaws);
		context.takeScreenshot("m12-figures-front");
		server.runCommand("tp @a " + (X + 9.5) + " " + (g + 1) + " " + (Z + 4.5) + " 135 10");
		context.waitTicks(10);
		context.takeScreenshot("m12-figures-side");
		row(context, server, g, new float[] {150.0F, 215.0F, 90.0F, 333.0F});
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 6) + " " + (Z - 4.5) + " 0 50");
		context.waitTicks(10);
		context.takeScreenshot("m12-figures-turned");
		for (int i = 0; i < 4; i++) {
			int x = X - 6 + 4 * i;
			server.runCommand("setblock " + x + " " + g + " " + (Z + 4) + " minecraft:air");
		}
		// The compressed blocks.
		for (int i = 0; i < 4; i++) {
			server.runCommand("setblock " + (X - 6 + 4 * i) + " " + g + " " + (Z + 4) + " simple_block:" + EffigyType.values()[i].getSerializedName() + "_block");
		}
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 2) + " " + (Z - 2.5) + " 0 25");
		context.waitTicks(10);
		context.takeScreenshot("m12-compressed-blocks");
		for (int i = 0; i < 4; i++) {
			server.runCommand("setblock " + (X - 6 + 4 * i) + " " + g + " " + (Z + 4) + " minecraft:air");
		}
	}

	private static void row(ClientGameTestContext context, TestServerContext server, int g, float[] yaws) {
		server.runOnServer(s -> {
			for (int i = 0; i < 4; i++) {
				BlockPos pos = new BlockPos(X - 6 + 4 * i, g, Z + 4);
				s.overworld().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				s.overworld().setBlockAndUpdate(pos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				EffigyPlacementMenu.place(s.overworld(), pos, EffigyType.values()[i], yaws[i], null);
			}
		});
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 1) + " " + (Z - 3.5) + " 0 8");
		context.waitTicks(15);
		float[] seen = context.computeOnClient(client -> {
			float[] out = new float[4];
			for (int i = 0; i < 4; i++) {
				out[i] = client.level.getBlockEntity(new BlockPos(X - 6 + 4 * i, g, Z + 4)) instanceof EffigyBlockEntity e ? e.yaw() : Float.NaN;
			}
			return out;
		});
		for (int i = 0; i < 4; i++) {
			assertTrue(seen[i] == yaws[i], "client angle of effigy " + i + ": " + seen[i] + " vs " + yaws[i]);
		}
	}

	/** Zombies (in helmets, for the daylight) inside a zombie effigy's cone run out of it; the client sees them sprint. */
	private static void fear(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		BlockPos effigy = new BlockPos(X, g, Z - 18);
		List<Integer> ids = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			EffigyPlacementMenu.place(level, effigy, EffigyType.ZOMBIE, 0.0F, null);
			List<Integer> out = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				Mob zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
				zombie.snapTo(X + 0.5 + (i - 1) * 1.5, g, Z - 10.5, 0.0F, 0.0F);
				zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
				zombie.setPersistenceRequired();
				level.addFreshEntity(zombie);
				out.add(zombie.getId());
			}
			return out;
		});
		server.runCommand("tp @a " + (X + 0.5) + " " + (g + 3) + " " + (Z - 22.5) + " 0 20");
		boolean[] sprintSeen = new boolean[1];
		boolean shot = false;
		for (int t = 0; t < 60; t++) {
			context.waitTick();
			boolean sprinting = context.computeOnClient(client -> {
				for (int id : ids) {
					var entity = client.level.getEntity(id);
					if (entity != null && entity.isSprinting()) {
						return true;
					}
				}
				return false;
			});
			sprintSeen[0] |= sprinting;
			if (sprinting && !shot && t > 8) {
				context.takeScreenshot(shots + "-fear-fleeing");
				shot = true;
			}
		}
		if (!shot) {
			context.takeScreenshot(shots + "-fear-fleeing");
		}
		int[] outside = {0};
		for (int t = 0; t < 400 && outside[0] < ids.size(); t += 10) {
			context.waitTicks(10);
			outside[0] = server.computeOnServer(s -> {
				int n = 0;
				for (int id : ids) {
					if (s.overworld().getEntity(id) instanceof Mob mob && !Effigies.frightens(mob)) {
						n++;
					}
				}
				return n;
			});
		}
		context.takeScreenshot(shots + "-fear-left");
		server.runOnServer(s -> {
			for (int id : ids) {
				if (s.overworld().getEntity(id) instanceof Mob mob) {
					mob.discard();
				}
			}
		});
		server.runCommand("setblock " + effigy.getX() + " " + effigy.getY() + " " + effigy.getZ() + " minecraft:air");
		SimpleBlockTestLog.info("CLIENT M12 FEAR ({}): 3 zombies in a zombie effigy's cone; client saw sprinting {}; {} of 3 out of the cone", shots,
			sprintSeen[0], outside[0]);
		assertTrue(sprintSeen[0], "the client should see the fleeing zombies sprint");
		assertTrue(outside[0] == ids.size(), "all three zombies should leave the cone: " + outside[0]);
	}
}
