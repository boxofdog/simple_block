package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.boxofdog.simpleblock.client.InventoryScreenAddons;
import io.github.boxofdog.simpleblock.client.mixin.AbstractContainerScreenAccessor;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * M13 inventory UI in a real client ({@code m13-*} screenshots), in singleplayer and over a dedicated server:
 * the Shift + right-button glide across a chest's slots with real mouse input. One glide quick-moves each
 * occupied slot it crosses, once (gliding back changes nothing); gaps are harmless; the heavy-load cap and
 * the Overalls row still decide where things may go; letting go of Shift stops the glide; a plain right-drag
 * and a one-off Shift + right-click still do what vanilla does.
 */
public class InventoryUiClientTest implements FabricClientGameTest {
	private static final int X = 110;
	private static final int Z = 20;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			BlockPos chest = yard(server);
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);
			glide(context, server, chest, "m13");
			loadBar(context, server, chest, "m13");
			sortButton(context, server, chest, "m13");
		}
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			BlockPos chest = yard(server);
			connection.waitForChunksRender();
			context.waitTicks(10);
			glide(context, server, chest, "m13-server");
		}
	}

	private static BlockPos yard(TestServerContext server) {
		int g = server.computeOnServer(s -> {
			s.overworld().getChunk(X >> 4, Z >> 4);
			return s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
		});
		server.runCommand("gamerule minecraft:advance_time false");
		server.runCommand("gamerule minecraft:spawn_monsters false");
		server.runCommand("time set noon");
		server.runCommand("fill " + (X - 4) + " " + (g - 1) + " " + (Z - 4) + " " + (X + 4) + " " + (g - 1) + " " + (Z + 4) + " minecraft:stone");
		server.runCommand("fill " + (X - 4) + " " + g + " " + (Z - 4) + " " + (X + 4) + " " + (g + 4) + " " + (Z + 4) + " minecraft:air");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a " + (X + 0.5) + " " + g + " " + (Z + 0.5) + " 180 30");
		BlockPos chest = new BlockPos(X, g, Z - 2);
		server.runCommand("setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " minecraft:chest[facing=south]");
		return chest;
	}

	private static void glide(ClientGameTestContext context, TestServerContext server, BlockPos chest, String shots) {
		List<String> log = new ArrayList<>();
		String c = chest.getX() + " " + chest.getY() + " " + chest.getZ();

		// 1. One glide over chest slots 0-4 (slot 3 empty), then back again: four stacks move, each once.
		server.runCommand("clear @a");
		server.runCommand("data merge block " + c + " {Items:[]}");
		server.runCommand("item replace block " + c + " container.0 with minecraft:cobblestone 20");
		server.runCommand("item replace block " + c + " container.1 with minecraft:iron_pickaxe");
		server.runCommand("item replace block " + c + " container.2 with minecraft:dirt 5");
		server.runCommand("item replace block " + c + " container.4 with minecraft:cobblestone 10");
		open(context, chest);
		InventoryScreenAddons.glideMoves = 0;
		context.getInput().holdShift();
		sweep(context, new int[] {0, 1, 2, 3, 4, 3, 2, 1, 0}, () -> context.takeScreenshot(shots + "-glide-midway"), 4);
		context.getInput().releaseShift();
		context.waitTicks(5);
		String chestAfter = contents(server, chest, 0, 5);
		String hotbar = playerContents(server, 0, 4);
		int moves = InventoryScreenAddons.glideMoves;
		log.add("glide: chest " + chestAfter + " | hotbar " + hotbar + " | glide moves " + moves);
		assertTrue(chestAfter.equals("-,-,-,-,-"), "the glide should empty chest slots 0-4: " + chestAfter);
		assertTrue(hotbar.equals("30 cobblestone,1 iron_pickaxe,5 dirt,-"), "hotbar-first, merged: " + hotbar);
		assertTrue(moves == 3, "slots 1, 2 and 4 by the glide (slot 0 by vanilla's own click), none twice: " + moves);
		close(context);

		// 2. The heavy-load cap: two 64-ingot stacks; the first fills the load, the second is refused and stays.
		server.runCommand("clear @a");
		server.runCommand("item replace block " + c + " container.0 with minecraft:iron_ingot 64");
		server.runCommand("item replace block " + c + " container.1 with minecraft:iron_ingot 64");
		open(context, chest);
		InventoryScreenAddons.glideMoves = 0;
		context.getInput().holdShift();
		// Back and forth: the refused stack is still there each time, but is tried once.
		sweep(context, new int[] {0, 1, 0, 1, 0}, null, -1);
		context.getInput().releaseShift();
		context.waitTicks(5);
		String capped = contents(server, chest, 0, 2);
		int tries = InventoryScreenAddons.glideMoves;
		log.add("cap: chest " + capped + ", refused slot tried " + tries + "x");
		assertTrue(capped.equals("-,64 iron_ingot"), "the second stack is over the heavy-load cap: " + capped);
		assertTrue(tries == 1, "a slot acts once per glide, even when its stack stays: " + tries);
		close(context);

		// 3. The Overalls row: a full inventory without the Overalls refuses; with them, the stack lands in row 9-17.
		server.runCommand("clear @a");
		server.runCommand("data merge block " + c + " {Items:[]}");
		server.runCommand("item replace block " + c + " container.0 with minecraft:cobblestone 7");
		for (int i = 0; i < 36; i++) {
			if (i < 9 || i >= 18) {
				server.runCommand("item replace entity @a container." + i + " with minecraft:stick 64");
			}
		}
		open(context, chest);
		context.getInput().holdShift();
		sweep(context, new int[] {0, 1}, null, -1);
		context.getInput().releaseShift();
		context.waitTicks(5);
		String full = contents(server, chest, 0, 1);
		close(context);
		server.runCommand("item replace entity @a armor.chest with simple_block:overalls");
		context.waitTicks(3);
		open(context, chest);
		context.getInput().holdShift();
		sweep(context, new int[] {1, 0}, null, -1);
		context.getInput().releaseShift();
		context.waitTicks(5);
		String row = playerContents(server, 9, 18);
		String left = contents(server, chest, 0, 1);
		log.add("overalls: without " + full + ", with: chest " + left + ", row 9-17 " + row);
		assertTrue(full.equals("7 cobblestone"), "no room without the Overalls row: " + full);
		assertTrue(left.equals("-") && row.contains("7 cobblestone"), "the Overalls row takes it: chest " + left + ", row " + row);
		close(context);

		// 4. Letting go of Shift mid-glide stops it; a plain right-drag moves nothing; one Shift + right-click moves one stack.
		server.runCommand("clear @a");
		server.runCommand("data merge block " + c + " {Items:[]}");
		for (int i = 0; i < 4; i++) {
			server.runCommand("item replace block " + c + " container." + i + " with minecraft:dirt " + (i + 1));
		}
		open(context, chest);
		context.getInput().holdShift();
		// Shift goes up once the glide reaches slot 1; the button stays down over slot 2.
		sweep(context, new int[] {0, 1, 2}, () -> context.getInput().releaseShift(), 1);
		context.waitTicks(5);
		String stopped = contents(server, chest, 0, 4);
		// A plain right-drag from the (now empty) slot 0 over 2 and 3, with an empty cursor.
		sweep(context, new int[] {0, 2, 3}, null, -1);
		context.waitTicks(5);
		String plain = contents(server, chest, 0, 4);
		context.getInput().holdShift();
		click(context, 2);
		context.getInput().releaseShift();
		context.waitTicks(5);
		String single = contents(server, chest, 0, 4);
		ItemStack carried = context.computeOnClient(client -> client.player.containerMenu.getCarried().copy());
		log.add("Shift up mid-glide " + stopped + "; plain right-drag " + plain + "; one Shift + right-click " + single + ", cursor " + describe(carried));
		assertTrue(stopped.equals("-,-,3 dirt,4 dirt"), "Shift released over slot 1: slot 2 stays: " + stopped);
		assertTrue(plain.equals(stopped), "a plain right-drag with an empty cursor moves nothing: " + plain);
		assertTrue(single.equals("-,-,-,4 dirt") && carried.isEmpty(), "one Shift + right-click moves one stack: " + single);
		close(context);
		server.runCommand("clear @a");
		server.runCommand("data merge block " + c + " {Items:[]}");

		SimpleBlockTestLog.info("CLIENT M13 GLIDE ({}): {}", shots, log);
	}

	/**
	 * BUG-010: the load bar stays on screen and clear of every slot, with and without the Overalls' backpack
	 * row, in the inventory, a chest and a double chest, at GUI scales 2 and 3 (at 1280 x 720, scale 3 leaves
	 * a double chest no room under its panel, so the bar takes the inventory label's row).
	 */
	private static void loadBar(ClientGameTestContext context, TestServerContext server, BlockPos chest, String shots) {
		BlockPos double0 = chest.east(2);
		server.runCommand("setblock " + double0.getX() + " " + double0.getY() + " " + double0.getZ() + " minecraft:chest[facing=south,type=right]");
		server.runCommand("setblock " + (double0.getX() + 1) + " " + double0.getY() + " " + double0.getZ() + " minecraft:chest[facing=south,type=left]");
		List<String> log = new ArrayList<>();
		int scaleBefore = context.computeOnClient(client -> client.options.guiScale().get());
		for (boolean overalls : new boolean[] {false, true}) {
			server.runCommand("clear @a");
			if (overalls) {
				server.runCommand("item replace entity @a armor.chest with simple_block:overalls");
			}
			server.runCommand("item replace entity @a container.20 with minecraft:iron_ingot 40");
			server.runCommand("item replace entity @a container.21 with minecraft:copper_ingot 30");
			context.waitTicks(5);
			for (int scale : new int[] {2, 3}) {
				context.runOnClient(client -> {
					client.options.guiScale().set(scale);
					client.resizeGui();
				});
				context.waitTicks(2);
				String tag = (overalls ? "overalls" : "plain") + "-scale" + scale;
				context.getInput().pressKey(options -> options.keyInventory);
				context.waitForScreen(net.minecraft.client.gui.screens.inventory.InventoryScreen.class);
				context.waitTicks(3);
				log.add(barCheck(context, "inventory " + tag));
				close(context);
				open(context, chest);
				log.add(barCheck(context, "chest " + tag));
				close(context);
				open(context, double0);
				int slots = context.computeOnClient(client -> ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().slots.size());
				assertTrue(slots == 54 + 36, "a double chest: " + slots);
				log.add(barCheck(context, "double chest " + tag));
				if (overalls) {
					context.takeScreenshot(shots + "-double-chest-" + tag);
				}
				close(context);
			}
		}
		context.runOnClient(client -> {
			client.options.guiScale().set(scaleBefore);
			client.resizeGui();
		});
		server.runCommand("clear @a");
		SimpleBlockTestLog.info("CLIENT M13 BUG-010 LOAD BAR ({}): {}", shots, log);
	}

	/** The chest Sort button, clicked with the real mouse: the chest is sorted, the player's inventory is not. */
	private static void sortButton(ClientGameTestContext context, TestServerContext server, BlockPos chest, String shots) {
		String c = chest.getX() + " " + chest.getY() + " " + chest.getZ();
		server.runCommand("clear @a");
		server.runCommand("data merge block " + c + " {Items:[]}");
		server.runCommand("item replace block " + c + " container.20 with minecraft:cobblestone 40");
		server.runCommand("item replace block " + c + " container.7 with minecraft:dirt 3");
		server.runCommand("item replace block " + c + " container.11 with minecraft:cobblestone 40");
		server.runCommand("item replace entity @a container.22 with minecraft:dirt 5");
		server.runCommand("item replace entity @a container.19 with minecraft:cobblestone 5");
		open(context, chest);
		double[] at = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
			int[] button = InventoryScreenAddons.chestSortButton(screen);
			for (var slot : screen.getMenu().slots) {
				if (slot.isActive() && button[0] < slot.x + 17 && slot.x - 1 < button[0] + button[2] && button[1] < slot.y + 17 && slot.y - 1 < button[1] + button[3]) {
					throw new AssertionError("the Sort button overlaps slot " + slot.index);
				}
			}
			double scale = client.getWindow().getGuiScale();
			return new double[] {(panel.simpleBlock$getLeftPos() + button[0] + button[2] / 2.0) * scale,
				(panel.simpleBlock$getTopPos() + button[1] + button[3] / 2.0) * scale};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(3);
		context.takeScreenshot(shots + "-chest-sort-hover");
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
		String sorted = contents(server, chest, 0, 4);
		String mine = playerContents(server, 19, 23);
		close(context);
		SimpleBlockTestLog.info("CLIENT M13 CHEST SORT ({}): chest {} | player 19-22 {}", shots, sorted, mine);
		assertTrue(sorted.equals("3 dirt,64 cobblestone,16 cobblestone,-"), "the chest sorted: " + sorted);
		assertTrue(mine.equals("5 cobblestone,-,-,5 dirt"), "the player's inventory untouched: " + mine);
		server.runCommand("clear @a");
		server.runCommand("data merge block " + c + " {Items:[]}");
	}

	/** Asserts the load bar is on screen and clear of every active slot (and, in the label row, of the label); describes where it is. */
	private static String barCheck(ClientGameTestContext context, String what) {
		String[] result = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			int[] bar = InventoryScreenAddons.barBounds(screen);
			AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
			int left = panel.simpleBlock$getLeftPos();
			int top = panel.simpleBlock$getTopPos();
			boolean onScreen = bar[0] >= 0 && bar[1] >= 0 && bar[2] <= screen.width && bar[3] <= screen.height;
			String overlap = "";
			for (var slot : screen.getMenu().slots) {
				int x0 = left + slot.x - 1;
				int y0 = top + slot.y - 1;
				if (slot.isActive() && bar[0] < x0 + 18 && x0 < bar[2] && bar[1] < y0 + 18 && y0 < bar[3]) {
					overlap += slot.index + " ";
				}
			}
			String place = InventoryScreenAddons.labelRowBar(screen) ? "label row"
				: bar[1] >= top + panel.simpleBlock$getImageHeight() ? "under the panel" : "in the row";
			return new String[] {String.valueOf(onScreen), overlap, place + " y " + (bar[1] - top) + "-" + (bar[3] - top) + " of panel "
				+ panel.simpleBlock$getImageHeight() + ", screen " + screen.height};
		});
		assertTrue(result[0].equals("true"), what + ": the load bar must be on screen: " + result[2]);
		assertTrue(result[1].isEmpty(), what + ": the load bar overlaps slots " + result[1] + ": " + result[2]);
		return what + ": " + result[2];
	}

	private static void open(ClientGameTestContext context, BlockPos chest) {
		lookAtPoint(context, Vec3.atCenterOf(chest));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(ContainerScreen.class);
		context.waitTicks(3);
	}

	private static void close(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(3);
	}

	/** Window position of the centre of the open screen's menu slot. */
	private static double[] at(ClientGameTestContext context, int slot) {
		return context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
			var s = screen.getMenu().slots.get(slot);
			AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
			double scale = client.getWindow().getGuiScale();
			return new double[] {(panel.simpleBlock$getLeftPos() + s.x + 8) * scale, (panel.simpleBlock$getTopPos() + s.y + 8) * scale};
		});
	}

	/** Right button down on the first slot, glides through the rest in small cursor steps, then up. */
	private static void sweep(ClientGameTestContext context, int[] slots, Runnable midway, int midwayAfter) {
		double[] p = at(context, slots[0]);
		context.getInput().setCursorPos(p[0], p[1]);
		context.waitTicks(1);
		context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(2);
		for (int i = 1; i < slots.length; i++) {
			double[] q = at(context, slots[i]);
			for (int step = 1; step <= 3; step++) {
				context.getInput().setCursorPos(p[0] + (q[0] - p[0]) * step / 3, p[1] + (q[1] - p[1]) * step / 3);
				context.waitTicks(1);
			}
			p = q;
			if (i == midwayAfter && midway != null) {
				midway.run();
			}
		}
		context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(2);
	}

	private static void click(ClientGameTestContext context, int slot) {
		double[] p = at(context, slot);
		context.getInput().setCursorPos(p[0], p[1]);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(2);
	}

	private static String contents(TestServerContext server, BlockPos pos, int from, int to) {
		return server.computeOnServer(s -> {
			Container container = (Container) s.overworld().getBlockEntity(pos);
			List<String> out = new ArrayList<>();
			for (int i = from; i < to; i++) {
				out.add(describe(container.getItem(i)));
			}
			return String.join(",", out);
		});
	}

	private static String playerContents(TestServerContext server, int from, int to) {
		return server.computeOnServer(s -> {
			ServerPlayer player = s.overworld().players().getFirst();
			List<String> out = new ArrayList<>();
			for (int i = from; i < to; i++) {
				out.add(describe(player.getInventory().getItem(i)));
			}
			return String.join(",", out);
		});
	}

	private static String describe(ItemStack stack) {
		return stack.isEmpty() ? "-" : stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
	}
}
