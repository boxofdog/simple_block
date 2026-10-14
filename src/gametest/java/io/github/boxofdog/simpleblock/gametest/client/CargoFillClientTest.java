package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.CargoMinecartRenderer;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.train.CargoLooks;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M11 in a real client ({@code m11-*} screenshots), then over a dedicated server: every form of load at
 * its fullest and a second material of each half full, one material through the empty and four
 * banded fills, the renderer's chosen look and band for each cart checked against its synced cargo,
 * carts tipping into hoppers through an open side gate and an open end gate, loads on an east-west and
 * a sloping rail, and a cart loaded by holding the use key, its fill rising, with no text shown.
 */
public class CargoFillClientTest implements FabricClientGameTest {
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
			run(context, server, g, "m11");
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			run(context, server, g, "m11-server");
		}
	}

	private static int yard(TestServerContext server) {
		// Generate the yard's chunk first: on a fresh dedicated server it may not exist yet (the height would read the world's bottom).
		int g = server.computeOnServer(s -> {
			s.overworld().getChunk(X >> 4, Z >> 4);
			return s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
		});
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("fill " + (X - 14) + " " + (g - 1) + " " + (Z - 6) + " " + (X + 14) + " " + (g - 1) + " " + (Z + 16) + " minecraft:stone");
		server.runCommand("fill " + (X - 14) + " " + g + " " + (Z - 6) + " " + (X + 14) + " " + (g + 8) + " " + (Z + 16) + " minecraft:air");
		gameMode(server, net.minecraft.world.level.GameType.CREATIVE);
		return g;
	}

	/** Without the chat line the command would print. */
	private static void gameMode(TestServerContext server, net.minecraft.world.level.GameType mode) {
		server.runOnServer(s -> s.overworld().players().forEach(player -> player.setGameMode(mode)));
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
	}

	/** A plain rail at {@code pos} (shape {@code shape}) with a cart on it holding {@code count} of {@code item}. */
	private static void cart(TestServerContext server, BlockPos pos, String shape, Item item, int count) {
		server.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " minecraft:rail[shape=" + shape + "]");
		server.runOnServer(s -> {
			Minecart cart = EntityTypes.MINECART.create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			cart.setPos(Vec3.atBottomCenterOf(pos).add(0.0, 0.0625, 0.0));
			MinecartCargo.cart(cart).simpleBlock$setCargo(item, count);
			s.overworld().addFreshEntity(cart);
		});
	}

	/** On the client: each cart near {@code pos}: its synced cargo, and the look and band its renderer chose. */
	private static List<String> rendered(ClientGameTestContext context, BlockPos from, BlockPos to) {
		return context.computeOnClient(client -> {
			List<String> out = new ArrayList<>();
			List<Minecart> carts = new ArrayList<>(client.level.getEntitiesOfClass(Minecart.class, AABB.encapsulatingFullBlocks(from, to).inflate(0.5)));
			carts.sort(java.util.Comparator.comparingDouble((Minecart c) -> c.getZ()).thenComparingDouble(c -> c.getX()));
			for (Minecart cart : carts) {
				EntityRenderer<? super Minecart, ?> renderer = client.getEntityRenderDispatcher().getRenderer(cart);
				CargoMinecartRenderer.State state = (CargoMinecartRenderer.State) ((CargoMinecartRenderer) renderer).createRenderState();
				((CargoMinecartRenderer) renderer).extractRenderState(cart, state, 1.0F);
				MinecartCargo.Cart cargo = MinecartCargo.cart(cart);
				int band = CargoLooks.band(cargo.simpleBlock$count());
				boolean agrees = state.cargoBand == band && (band == 0 || state.cargoLook == CargoLooks.look(cargo.simpleBlock$cargo()));
				out.add(BuiltInRegistries.ITEM.getKey(cargo.simpleBlock$cargo()).getPath() + " " + cargo.simpleBlock$count() + " -> "
					+ (state.cargoBand == 0 ? "none" : state.cargoLook.getSerializedName() + "/" + state.cargoLook.form().name().toLowerCase() + " band "
						+ state.cargoBand) + (agrees ? "" : " MISMATCH"));
			}
			return out;
		});
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, String name, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch);
		context.waitTicks(8);
		context.takeScreenshot(name);
	}

	private static void run(ClientGameTestContext context, TestServerContext server, int g, String shots) {
		// Row 1 (z = Z): each form at its fullest. Row 2 (Z + 2): a second material of each, half full.
		String[][] rows = {{"iron_ingot", "diamond", "gold_block", "iron_ore", "sand", "stick"},
			{"copper_ingot", "emerald", "diamond_block", "raw_gold", "gravel", "cobbled_deepslate"}};
		int[] counts = {256, 100};
		for (int r = 0; r < 2; r++) {
			for (int i = 0; i < rows[r].length; i++) {
				cart(server, new BlockPos(X - 7 + 3 * i, g, Z + 2 * r), "east_west", item(rows[r][i]), counts[r]);
			}
		}
		// Row 3 (Z + 4): raw iron empty and in each band.
		int[] bands = {0, 1, 100, 160, 256};
		for (int i = 0; i < bands.length; i++) {
			cart(server, new BlockPos(X - 7 + 3 * i, g, Z + 4), "east_west", Items.RAW_IRON, bands[i]);
		}
		context.waitTicks(10);
		String onServer = server.computeOnServer(s -> {
			List<String> out = new ArrayList<>();
			for (Minecart cart : s.overworld().getEntitiesOfClass(Minecart.class, new AABB(X - 20, g - 3, Z - 10, X + 20, g + 5, Z + 20))) {
				out.add(cart.blockPosition().toShortString() + " " + MinecartCargo.cart(cart).simpleBlock$count());
			}
			return out.toString();
		});
		SimpleBlockTestLog.info("CLIENT M11 SERVER CARTS ({}): {}", shots, onServer);
		List<String> forms = rendered(context, new BlockPos(X - 8, g, Z), new BlockPos(X + 10, g, Z + 2));
		List<String> banded = rendered(context, new BlockPos(X - 8, g, Z + 4), new BlockPos(X + 10, g, Z + 4));
		shot(context, server, shots + "-forms", X + 1.5, g + 5.5, Z - 4.5, 0, 45);
		shot(context, server, shots + "-forms-close-1", X - 3.5, g + 2.2, Z - 2.0, 0, 40);
		shot(context, server, shots + "-forms-close-2", X + 5.5, g + 2.2, Z - 2.0, 0, 40);
		shot(context, server, shots + "-bands", X - 0.5, g + 2.6, Z + 7.0, 180, 35);
		SimpleBlockTestLog.info("CLIENT M11 FORMS ({}): {}", shots, forms);
		SimpleBlockTestLog.info("CLIENT M11 BANDS ({}): {}", shots, banded);
		assertTrue(forms.size() == 12 && banded.size() == 5 && forms.stream().noneMatch(f -> f.contains("MISMATCH"))
			&& banded.stream().noneMatch(f -> f.contains("MISMATCH")), "the renderer's look and band follow the synced cargo: " + forms + " " + banded);
		assertTrue(forms.get(5).contains("covered") && banded.get(0).endsWith("none") && banded.get(4).contains("band 4"), "fallback and bands");

		// Tipping into hoppers: a side gate (hopper east of a north-facing rail) and an end gate (hopper north).
		int sz = Z + 8;
		for (int k = 0; k < 2; k++) {
			int x = X - 4 + 8 * k;
			server.runCommand("setblock " + x + " " + g + " " + sz + " simple_block:loading_track[facing=north,shape=north_south]");
			BlockPos hopper = k == 0 ? new BlockPos(x + 1, g - 1, sz) : new BlockPos(x, g - 1, sz - 1);
			server.runCommand("setblock " + hopper.getX() + " " + hopper.getY() + " " + hopper.getZ() + " minecraft:hopper[facing=down]");
			BlockPos rail = new BlockPos(x, g, sz);
			server.runOnServer(s -> {
				Minecart cart = EntityTypes.MINECART.create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				cart.setPos(Vec3.atBottomCenterOf(rail).add(0.0, 0.0625, 0.0));
				MinecartCargo.cart(cart).simpleBlock$setCargo(Items.RAW_COPPER, 220);
				s.overworld().addFreshEntity(cart);
			});
		}
		context.waitTicks(30);
		String[] gates = context.computeOnClient(client -> {
			String[] out = new String[2];
			for (int k = 0; k < 2; k++) {
				BlockPos rail = new BlockPos(X - 4 + 8 * k, g, sz);
				List<Minecart> carts = client.level.getEntitiesOfClass(Minecart.class, new AABB(rail).inflate(0.5));
				out[k] = carts.isEmpty() ? "none" : LoadingTracks.freight(carts.getFirst()).simpleBlock$swingSide() + " "
					+ String.format("%.2f", LoadingTracks.freight(carts.getFirst()).simpleBlock$gateSwing(1.0F)) + " cargo "
					+ MinecartCargo.cart(carts.getFirst()).simpleBlock$count();
			}
			return out;
		});
		shot(context, server, shots + "-gates-open", X + 0.5, g + 2.5, sz + 5.0, 180, 25);
		shot(context, server, shots + "-side-gate-close", X - 1.5, g + 1.8, sz + 1.5, 120, 30);
		SimpleBlockTestLog.info("CLIENT M11 GATES ({}): side hopper: {}; end hopper: {}", shots, gates[0], gates[1]);
		assertTrue(gates[0].startsWith("east 1.00") && gates[1].startsWith("north 1.00"), "gates open on the client: " + gates[0] + " / " + gates[1]);

		// Orientations: north-south, and on a curve (turned 45 degrees).
		int oz = Z + 12;
		cart(server, new BlockPos(X - 4, g, oz), "north_south", item("netherite_ingot"), 200);
		cart(server, new BlockPos(X, g, oz), "south_east", item("cobblestone"), 256);
		cart(server, new BlockPos(X + 4, g, oz), "north_south", item("lapis_lazuli"), 150);
		context.waitTicks(10);
		shot(context, server, shots + "-orientations", X + 0.5, g + 2.6, oz - 3.5, 0, 35);

		// Loaded by holding the use key: the fill rises; no text.
		gameMode(server, net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos hand = new BlockPos(X + 8, g, oz);
		cart(server, hand, "north_south", Items.AIR, 0);
		server.runCommand("tp @a " + (hand.getX() + 0.5) + " " + g + " " + (hand.getZ() - 2.0) + " 0 30");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:raw_iron 64");
		context.waitTicks(10);
		lookAtPoint(context, Vec3.atBottomCenterOf(hand).add(0.0, 0.4, 0.0));
		CableClientTest.clearOverlay(context);
		context.getInput().holdKey(options -> options.keyUse);
		List<Integer> fill = new ArrayList<>();
		for (int t = 0; t < 6; t++) {
			context.waitTicks(10);
			fill.add(context.computeOnClient(client -> {
				List<Minecart> carts = client.level.getEntitiesOfClass(Minecart.class, new AABB(hand).inflate(0.5));
				return carts.isEmpty() ? -1 : MinecartCargo.cart(carts.getFirst()).simpleBlock$count();
			}));
		}
		context.getInput().releaseKey(options -> options.keyUse);
		String overlay = CableClientTest.overlayMessage(context);
		context.takeScreenshot(shots + "-hand-loading");
		gameMode(server, net.minecraft.world.level.GameType.CREATIVE);
		SimpleBlockTestLog.info("CLIENT M11 HAND ({}): held use, client count every 10 ticks {}; overlay '{}'", shots, fill, overlay);
		boolean rising = fill.getFirst() > 0 && fill.getLast() > fill.getFirst() && fill.getLast() <= 64;
		assertTrue(rising && overlay.isEmpty(), "gradual hand loading seen by the client, and no text: " + fill + " '" + overlay + "'");
		server.runCommand("kill @e[type=minecraft:minecart]");
		context.waitTicks(5);
	}
}
