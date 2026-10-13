package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.mixin.AbstractMinecartRendererAccessor;
import io.github.boxofdog.simpleblock.client.train.GatedMinecartModel;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * The owner's M07 cart redesign in a real client ({@code m07c-*} screenshots): every minecart has the
 * same body ({@link GatedMinecartModel}): vanilla's floor and texture, four corner columns, a rim along
 * the top of each long side and four hinged gates. On an east-west track: a freight cart with every gate
 * shut, then one freight cart for each gate held open (east and west: the end gates folded down into
 * ramps; north and south: the side gates swung up under their rims), a cushioned cart and the
 * Cobblestone Engine (all shut). For each cart the gates are read from the model the renderer draws,
 * set up as for drawing.
 */
public class MinecartBodyClientTest implements FabricClientGameTest {
	private static final int Z = 2;
	private static final List<Direction> OPEN = List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(5);
			client.options.simulationDistance().set(5);
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill -2 " + (g - 1) + " -4 26 " + (g - 1) + " 10 minecraft:stone");
			server.runCommand("fill -2 " + g + " -4 26 " + (g + 6) + " 10 minecraft:air");
			server.runCommand("fill 0 " + g + " " + Z + " 24 " + g + " " + Z + " minecraft:rail[shape=east_west]");
			server.runCommand("fill 0 " + g + " " + (Z + 4) + " 10 " + g + " " + (Z + 4) + " minecraft:rail[shape=east_west]");
			server.runCommand("kill @e[type=!minecraft:player]");
			server.runCommand("gamemode spectator @a");
			server.runCommand("tp @a 12.5 " + (g + 4) + " -5.5 0 30");
			world.getConnection().waitForChunksRender();

			// Freight carts: one shut, one per gate held open.
			for (int i = 0; i < 5; i++) {
				server.runCommand("summon minecraft:minecart " + (2.5 + 4 * i) + " " + (g + 0.1) + " " + (Z + 0.5));
			}
			server.runCommand("summon minecraft:minecart 3.5 " + (g + 0.1) + " " + (Z + 4.5));
			server.runCommand("summon minecraft:furnace_minecart 7.5 " + (g + 0.1) + " " + (Z + 4.5));
			context.waitTicks(10);
			server.computeOnServer(s -> {
				for (int i = 0; i < 4; i++) {
					Minecart cart = freight(s.overworld(), 2.5 + 4 * (i + 1), g);
					LoadingTracks.freight(cart).simpleBlock$holdGateOpen(OPEN.get(i), 100000);
				}
				Minecart cushioned = s.overworld().getEntitiesOfClass(Minecart.class, new AABB(3, g - 1, Z + 4, 4, g + 2, Z + 5)).getFirst();
				MinecartCargo.cart(cushioned).simpleBlock$setCushion(DyeColor.RED);
				return null;
			});
			context.waitTicks(20);

			List<String> report = new ArrayList<>();
			float[][] seen = new float[7][];
			String[] names = {"freight, shut", "freight, east gate open", "freight, west gate open", "freight, north gate open", "freight, south gate open",
				"cushioned", "Cobblestone Engine"};
			context.runOnClient(client -> {
				List<AbstractMinecart> carts = new ArrayList<>();
				for (int i = 0; i < 5; i++) {
					double x = 2.5 + 4 * i;
					carts.add(client.level.getEntitiesOfClass(Minecart.class, new AABB(x - 0.5, g - 1, Z, x + 0.5, g + 2, Z + 1)).getFirst());
				}
				carts.add(client.level.getEntitiesOfClass(Minecart.class, new AABB(3, g - 1, Z + 4, 4, g + 2, Z + 5)).getFirst());
				carts.add(client.level.getEntitiesOfClass(MinecartFurnace.class, new AABB(7, g - 1, Z + 4, 8, g + 2, Z + 5)).getFirst());
				for (int i = 0; i < carts.size(); i++) {
					AbstractMinecart cart = carts.get(i);
					seen[i] = SkipClientTest.gates(client, cart);
					var model = ((AbstractMinecartRendererAccessor) client.getEntityRenderDispatcher().getRenderer(cart)).simpleBlock$getModel();
					report.add(names[i] + ": " + model.getClass().getSimpleName() + " " + SkipClientTest.fmtGates(seen[i]));
				}
			});

			context.takeScreenshot("m07c-all-carts-overview");
			shot(context, server, "m07c-freight-shut", 2.5 + 2.2, g + 1.3, Z + 0.5 + 2.2, 135, 25);
			shot(context, server, "m07c-east-end-gate-ramp", 6.5 + 2.6, g + 1.2, Z + 0.5 + 1.2, 115, 20);
			shot(context, server, "m07c-west-end-gate-ramp", 10.5 - 2.6, g + 1.2, Z + 0.5 + 1.2, -115, 20);
			shot(context, server, "m07c-north-side-gate-up", 14.5 + 1.0, g + 1.2, Z + 0.5 - 2.6, 20, 20);
			shot(context, server, "m07c-south-side-gate-up", 18.5 + 1.0, g + 1.2, Z + 0.5 + 2.6, 160, 20);
			shot(context, server, "m07c-side-gate-from-above", 18.5, g + 3.0, Z + 0.5 + 0.2, 180, 85);
			shot(context, server, "m07c-cushioned-and-engine-shut", 5.5, g + 1.8, Z + 4.5 + 3.0, 180, 20);

			SimpleBlockTestLog.info("CLIENT CART BODY (owner's cart redesign): {}", report);
			assertTrue(Arrays.equals(seen[0], new float[4]) && Arrays.equals(seen[5], new float[4]) && Arrays.equals(seen[6], new float[4]),
				"shut freight, cushioned and engine: all gates shut: " + report);
			// East/west are the ends of an east-west cart, north/south its sides: one gate each, right sort.
			for (int i = 1; i <= 4; i++) {
				float[] turned = seen[i];
				int open = 0;
				for (float t : turned) {
					open += t > 1.0F ? 1 : 0;
				}
				float ends = turned[0] + turned[1];
				float sides = turned[2] + turned[3];
				boolean end = i <= 2;
				assertTrue(open == 1 && (end ? Math.abs(ends - GatedMinecartModel.END_OPEN_DEGREES) < 0.1F : Math.abs(sides - GatedMinecartModel.SIDE_OPEN_DEGREES) < 0.1F),
					names[i] + ": " + SkipClientTest.fmtGates(turned));
			}
			assertTrue(report.stream().allMatch(line -> line.contains("GatedMinecartModel")), "every cart draws the gated body: " + report);
		}
	}

	private static Minecart freight(net.minecraft.server.level.ServerLevel level, double x, int g) {
		return level.getEntitiesOfClass(Minecart.class, new AABB(x - 0.5, g - 1, Z, x + 0.5, g + 2, Z + 1)).getFirst();
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, String name, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch);
		context.waitTicks(4);
		context.takeScreenshot(name);
	}
}
