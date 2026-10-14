package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.InventoryScreenAddons;
import io.github.boxofdog.simpleblock.client.metallurgy.BlastFurnaceScreen;
import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceBlock;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceBlockEntity;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceMoldBlock;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceStructure;
import io.github.boxofdog.simpleblock.metallurgy.MetallurgyContent;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M08 in a real client ({@code m08-*} screenshots in {@code build/run/clientGameTest/screenshots}):
 * <ul>
 *   <li>the furnace item placed with a real use-key press builds the whole 3x3x4 furnace, seen by the
 *       client; front and hopper views, lit, the mold filling as a block is cast, the cast block, the next fill over it;</li>
 *   <li>a real use-key press on the furnace opens its screen, showing the reservoir's count;</li>
 *   <li>the survival inventory's load bar over the cap (64 ingots plus a carried anvil): the client's
 *       load agrees with the server's, the bar's tooltip says only commodities are refused.</li>
 * </ul>
 * The furnace placement, lighting and screen are repeated with a dedicated server ({@code m08-server-*}).
 * The yard, at ground level {@code g}: stone x 0..20, z 0..16; the furnace's front middle bottom block
 * at (10, g, 8), facing north towards the player, its channel and mold at z 7 and 6.
 */
public class BlastFurnaceClientTest implements FabricClientGameTest {
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
			BlockPos front = new BlockPos(10, g, 8);
			BlockPos controller = BlastFurnaceStructure.controllerFor(front, net.minecraft.core.Direction.NORTH);
			furnace(context, server, front, controller, "m08-furnace");
			screen(context, server, controller, "m08-furnace");
			loadBar(context, server);
		}
		// The same over a real network connection: a dedicated server, the client a remote player.
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			BlockPos front = new BlockPos(10, g, 8);
			BlockPos controller = BlastFurnaceStructure.controllerFor(front, net.minecraft.core.Direction.NORTH);
			furnace(context, server, front, controller, "m08-server-furnace");
			screen(context, server, controller, "m08-server-furnace");
		}
	}

	/** The yard, cleared and floored, with the player in front of where the furnace goes; returns ground level. */
	private static int yard(TestServerContext server) {
		int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("fill 0 " + (g - 1) + " 0 20 " + (g - 1) + " 16 minecraft:stone");
		server.runCommand("fill 0 " + g + " 0 20 " + (g + 8) + " 16 minecraft:air");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a 10.5 " + g + " 3.5 0 20");
		return g;
	}

	private static ServerPlayer playerOf(ServerLevel level) {
		return level.players().getFirst();
	}

	private static void furnace(ClientGameTestContext context, TestServerContext server, BlockPos front, BlockPos controller, String shots) {
		server.runCommand("item replace entity @a weapon.mainhand with simple_block:blast_furnace");
		context.waitTicks(3);
		// The block used becomes the mold, two in front of the furnace.
		lookAtPoint(context, new Vec3(front.getX() + 0.5, front.getY() - 0.02, front.getZ() - 1.5));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		int serverBlocks = server.computeOnServer(s -> count(s.overworld(), controller));
		int clientBlocks = context.computeOnClient(client -> count(client.level, controller));
		BlockState clientController = context.computeOnClient(client -> client.level.getBlockState(controller));
		server.runCommand("tp @a 10.5 " + front.getY() + " -2.5 0 10");
		context.waitTicks(10);
		context.takeScreenshot(shots + "-front");
		server.runCommand("tp @a 15.5 " + (front.getY() + 7) + " 2.5 35 35");
		context.waitTicks(10);
		context.takeScreenshot(shots + "-hopper");
		// Lit and casting: a short cast (160 ticks) for the pictures.
		Tuning.set("blast_furnace.cast_ticks", 160);
		server.runOnServer(s -> {
			BlastFurnaceBlockEntity furnace = (BlastFurnaceBlockEntity) s.overworld().getBlockEntity(controller);
			furnace.setOre(Items.RAW_IRON, 100);
			furnace.items().setItem(BlastFurnaceBlockEntity.FUEL_SLOT, new net.minecraft.world.item.ItemStack(Items.COAL_BLOCK, 4));
		});
		server.runCommand("tp @a 10.5 " + front.getY() + " -2.5 0 10");
		context.waitTicks(20);
		boolean lit = context.computeOnClient(client -> client.level.getBlockState(controller).getValue(BlastFurnaceBlock.LIT));
		context.takeScreenshot(shots + "-lit");
		// The output close up: the iron front's tap, the channel and the mold filling as the block is cast.
		BlockPos moldPos = BlastFurnaceStructure.mold(controller, net.minecraft.core.Direction.NORTH);
		server.runCommand("tp @a " + (front.getX() - 2.5) + " " + front.getY() + " " + (front.getZ() - 5.5) + " -37 25");
		context.waitTicks(60);
		boolean pouring = context.computeOnClient(client -> client.level.getBlockState(controller).getValue(BlastFurnaceBlock.POURING));
		int midFill = context.computeOnClient(client -> client.level.getBlockState(moldPos).getValue(BlastFurnaceMoldBlock.FILL));
		context.takeScreenshot(shots + "-filling");
		// The cast done: the solid block shows in the mold for a moment.
		int[] solidFill = {-1};
		String[] solidShows = {""};
		for (int i = 0; i < 120 && solidFill[0] != 0; i++) {
			context.waitTick();
			context.runOnClient(client -> {
				solidFill[0] = client.level.getBlockState(moldPos).getValue(BlastFurnaceMoldBlock.FILL);
				solidShows[0] = client.level.getBlockState(moldPos).getValue(BlastFurnaceMoldBlock.CONTENT).getSerializedName();
			});
		}
		context.takeScreenshot(shots + "-cast-block");
		// The next cast fills over it, the finished block above the molten metal.
		context.waitTicks(Tuning.getInt("blast_furnace.cast_hold_ticks") + 50);
		int nextFill = context.computeOnClient(client -> client.level.getBlockState(moldPos).getValue(BlastFurnaceMoldBlock.FILL));
		context.takeScreenshot(shots + "-next-fill");
		// No more ore: the mold shows the cast block once the cast in progress is set aside.
		server.runOnServer(s -> ((BlastFurnaceBlockEntity) s.overworld().getBlockEntity(controller)).setOre(Items.AIR, 0));
		Tuning.reset("blast_furnace.cast_ticks");
		context.waitTicks(5);
		String moldShows = context.computeOnClient(client -> client.level.getBlockState(moldPos).getValue(BlastFurnaceMoldBlock.CONTENT).getSerializedName());
		context.takeScreenshot(shots + "-casting");
		server.runCommand("tp @a " + (front.getX() + 4.5) + " " + (front.getY() + 1) + " " + (front.getZ() - 6.5) + " 28 12");
		context.waitTicks(5);
		context.takeScreenshot(shots + "-three-quarter");
		int cast = server.computeOnServer(s -> ((BlastFurnaceBlockEntity) s.overworld().getBlockEntity(controller)).mold().getCount());
		SimpleBlockTestLog.info("CLIENT M08 FURNACE ({}): use-key placement made {} structure blocks on the server, {} seen by the client, controller {}; "
			+ "lit {}, pouring {}, mold filled {}/8 part way through the first cast, then the cast block ({}, fill {}), the next fill {}/8 over it; "
			+ "{} blocks cast at cast_ticks 160; the mold then shows {}", shots, serverBlocks, clientBlocks, clientController, lit, pouring, midFill,
			solidShows[0], solidFill[0], nextFill, cast, moldShows);
		assertTrue(serverBlocks == 38 && clientBlocks == 38, "the whole furnace should be placed: server " + serverBlocks + ", client " + clientBlocks);
		assertTrue(clientController.is(MetallurgyContent.BLAST_FURNACE) && clientController.getValue(BlastFurnaceBlock.FACING)
			== net.minecraft.core.Direction.NORTH, "controller facing the player: " + clientController);
		assertTrue(moldShows.equals("iron"), "the mold should show the cast iron block, shows " + moldShows);
		assertTrue(lit && pouring && cast >= 1, "the furnace should light, pour and cast: lit " + lit + ", pouring " + pouring + ", cast " + cast);
		assertTrue(midFill >= 2 && midFill <= 6, "part way through a cast the client should see the mold part filled: " + midFill);
		assertTrue(solidFill[0] == 0 && solidShows[0].equals("iron"), "the finished cast should show as a solid iron block: " + solidShows[0] + " fill " + solidFill[0]);
		assertTrue(nextFill > 0, "the next cast should fill over it: " + nextFill);
	}

	private static int count(net.minecraft.world.level.Level level, BlockPos controller) {
		int n = 0;
		for (BlockPos pos : BlastFurnaceStructure.blocks(controller, net.minecraft.core.Direction.NORTH)) {
			BlockState state = level.getBlockState(pos);
			if (state.is(MetallurgyContent.BLAST_FURNACE) || state.is(MetallurgyContent.BLAST_FURNACE_PART)
				|| state.is(MetallurgyContent.BLAST_FURNACE_CHANNEL) || state.is(MetallurgyContent.BLAST_FURNACE_MOLD)) {
				n++;
			}
		}
		return n;
	}

	private static void screen(ClientGameTestContext context, TestServerContext server, BlockPos controller, String shots) {
		server.runOnServer(s -> ((BlastFurnaceBlockEntity) s.overworld().getBlockEntity(controller)).setOre(Items.RAW_IRON, 300));
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
		server.runCommand("tp @a 10.5 " + (controller.getY() - 1) + " " + (controller.getZ() - 4.5) + " 0 0");
		context.waitTicks(3);
		lookAtPoint(context, new Vec3(controller.getX() + 0.5, controller.getY() + 0.5, controller.getZ() - 1.0));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(BlastFurnaceScreen.class);
		context.waitTicks(5);
		int shown = context.computeOnClient(client -> ((BlastFurnaceScreen) client.gui.screen()).getMenu().count());
		context.takeScreenshot(shots + "-screen");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		SimpleBlockTestLog.info("CLIENT M08 SCREEN ({}): a use-key press opened the furnace screen showing {} / {} raw iron", shots, shown,
			BlastFurnaceBlockEntity.RESERVOIR_CAPACITY);
		assertTrue(shown == 300, "the screen should show the reservoir's 300, saw " + shown);
	}

	private static void loadBar(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("clear @a");
		server.runCommand("item replace entity @a container.18 with minecraft:iron_ingot 64");
		server.runCommand("item replace entity @a container.19 with minecraft:anvil 1");
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(3);
		int clientLoad = context.computeOnClient(client -> HeavyLoad.carried(client.player));
		int serverLoad = server.computeOnServer(s -> HeavyLoad.carried(playerOf(s.overworld())));
		int[] bar = context.computeOnClient(client -> InventoryScreenAddons.barBounds((AbstractContainerScreen<?>) client.gui.screen()));
		double scale = context.computeOnClient(client -> (double) client.getWindow().getGuiScale());
		context.getInput().setCursorPos((bar[0] + bar[2]) / 2.0 * scale, (bar[1] + bar[3]) / 2.0 * scale);
		context.waitTicks(3);
		List<String> tooltip = context.computeOnClient(client -> {
			List<Component> lines = InventoryScreenAddons.shownTooltip();
			return lines == null ? List.of("(no tooltip)") : lines.stream().map(Component::getString).toList();
		});
		context.takeScreenshot("m08-load-bar-over-cap");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		SimpleBlockTestLog.info("CLIENT M08 LOAD BAR: 64 iron ingots + an anvil: client {} / server {}; tooltip {}", HeavyLoad.describe(clientLoad),
			HeavyLoad.describe(serverLoad), tooltip);
		assertTrue(clientLoad == serverLoad && serverLoad == 95 * HeavyLoad.UNITS_PER_INGOT, "load 95 on both sides: client " + clientLoad);
		assertTrue(tooltip.getFirst().equals("Heavy load: 95 / 64") && tooltip.getLast().startsWith("No room for more ore, ingots or metal blocks"),
			"tooltip: " + tooltip);
		server.runCommand("clear @a");
	}
}
