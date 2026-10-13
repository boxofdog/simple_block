package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.overlayMessage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;

import io.github.boxofdog.simpleblock.client.skip.SkipScreen;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipMenu;

/**
 * M07 over a real network connection (a dedicated server started by the test, the real client
 * connected as a remote player): the skip placed and loaded by key presses, its screen opened and its
 * cargo slot clicked with the mouse (the server's slot, the remote screen and the cursor agree), and a
 * docked freight cart beside its chute filled through the discharge gate the client sees open.
 * Screenshots: {@code m07-server-*}.
 */
public class SkipServerClientTest implements FabricClientGameTest {
	private static final int X = 30;
	private static final int Z = 0;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 35, Z));
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @a");
			server.runCommand("fill 24 " + (g - 1) + " -6 36 " + (g - 1) + " 6 minecraft:stone");
			server.runCommand("fill 24 " + g + " -6 36 " + (g + 6) + " 6 minecraft:air");
			server.runCommand("setblock " + X + " " + g + " " + (Z - 1) + " simple_block:loading_track[facing=south,shape=north_south]");
			server.runCommand("kill @e[type=!minecraft:player]");
			// On the rail side (north), so the skip is placed facing it.
			server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z - 2.5) + " 0 30");
			connection.waitForChunksRender();

			// Placed by key, facing the player's way back (north, towards the Loading Rail).
			server.runCommand("item replace entity @a weapon.mainhand with simple_block:ore_skip");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(X + 0.5, g, Z + 0.4));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			String placed = server.computeOnServer(s -> skip(s.overworld(), g) + " facing " + skip(s.overworld(), g).facing());
			// Loaded by key: 64 raw iron.
			server.runCommand("item replace entity @a weapon.mainhand with minecraft:raw_iron 64");
			context.waitTicks(10);
			lookAtPoint(context, new Vec3(X + 0.5, g + 1.5, Z + 0.1));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			String loaded = overlayMessage(context);
			// The screen, and one click on the cargo slot.
			server.runCommand("clear @a");
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			int shown = context.computeOnClient(client -> client.gui.screen() instanceof SkipScreen screen ? screen.getMenu().count() : -1);
			double[] slot = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				int left = (client.getWindow().getGuiScaledWidth() - 176) / 2;
				int top = (client.getWindow().getGuiScaledHeight() - 133) / 2;
				return new double[] {(left + SkipMenu.CARGO_X + 8) * scale, (top + SkipMenu.CARGO_Y + 8) * scale};
			});
			context.getInput().setCursorPos(slot[0], slot[1]);
			context.waitTicks(2);
			context.getInput().pressMouse(1);
			context.waitTicks(10);
			context.takeScreenshot("m07-server-skip-screen");
			int afterRight = server.computeOnServer(s -> skip(s.overworld(), g).count());
			int[] client = context.computeOnClient(c -> new int[] {((SkipScreen) c.gui.screen()).getMenu().count(), c.player.containerMenu.getCarried().getCount()});
			context.getInput().pressMouse(0);
			context.waitTicks(10);
			int afterPut = server.computeOnServer(s -> skip(s.overworld(), g).count());
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitTicks(10);

			// A freight cart docked beside its chute is filled through the discharge gate.
			server.runCommand("summon minecraft:minecart " + (X + 0.5) + " " + (g + 0.1) + " " + (Z - 0.5));
			boolean gate = false;
			for (int i = 0; i < 100 && !gate; i++) {
				context.waitTicks(1);
				gate = context.computeOnClient(c -> c.level.getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).stream()
					.anyMatch(OreSkip::gateOpen));
			}
			server.runCommand("tp @a " + (X - 2.5) + " " + (g + 1) + " " + (Z - 2.5) + " -45 15");
			context.waitTicks(5);
			context.takeScreenshot("m07-server-skip-unloading");
			context.waitTicks(60);
			int cart = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X, g - 1, Z - 1, X + 1, g + 2, Z)).stream()
				.mapToInt(c -> MinecartCargo.cart(c).simpleBlock$count()).sum());
			int left = server.computeOnServer(s -> skip(s.overworld(), g).count());
			SimpleBlockTestLog.info("CLIENT DEDICATED SERVER SKIP: placed by key over the network ({}); loaded by key: '{}'; remote screen shows {}; right-click on "
				+ "the slot: server {}, client screen {}, cursor {}; left-click put back: server {}; a cart docked beside the chute: client saw the gate open {}, "
				+ "cart {} raw iron, skip {}", placed, loaded, shown, afterRight, client[0], client[1], afterPut, gate, cart, left);
			assertTrue(placed.contains("facing north") && loaded.contains("64 / 512") && shown == 64, "placed and loaded: " + placed + " '" + loaded + "' " + shown);
			assertTrue(afterRight == 32 && client[0] == 32 && client[1] == 32 && afterPut == 64, "slot clicks over the network: " + afterRight + "/"
				+ client[0] + "/" + client[1] + ", then " + afterPut);
			assertTrue(gate && cart == 64 && left == 0, "chute exchange over the network: gate " + gate + ", cart " + cart + ", skip " + left);
			server.computeOnServer(s -> {
				s.overworld().getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).forEach(e -> e.setCargo(Items.AIR, 0));
				return null;
			});
		}
	}

	private static OreSkip skip(ServerLevel level, int g) {
		return level.getEntitiesOfClass(OreSkip.class, new AABB(X - 1, g - 1, Z - 1, X + 2, g + 5, Z + 2)).stream().findFirst()
			.orElseThrow(() -> new AssertionError("no skip"));
	}
}
