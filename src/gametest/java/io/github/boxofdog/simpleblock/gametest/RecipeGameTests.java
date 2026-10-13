package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableContent;

/**
 * M06 cable recipes in a real crafting table grid (the menu the server runs for a player): three
 * iron nuggets down the middle column make two cables, three cables down the middle column make a
 * coil, and a coil goes back into three cables. The same items in another column, the old
 * nuggets-and-string recipe and the old eight-cable ring make nothing. Test ids refer to the matrix in
 * {@code docs/MILESTONE_06_REPORT.md}.
 */
public class RecipeGameTests {
	private static final Item N = Items.IRON_NUGGET;
	private static final Item C = CableContent.CABLE;
	private static final Item A = Items.AIR;

	/** What the crafting table's result slot shows for {@code grid} (row by row). */
	private static ItemStack craft(GameTestHelper helper, ServerPlayer player, Item... grid) {
		BlockPos table = new BlockPos(1, 1, 1);
		helper.setBlock(table, Blocks.CRAFTING_TABLE);
		CraftingMenu menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(table)));
		for (int i = 0; i < 9; i++) {
			menu.getSlot(1 + i).set(grid[i] == A ? ItemStack.EMPTY : new ItemStack(grid[i]));
		}
		ItemStack result = menu.getSlot(0).getItem().copy();
		for (int i = 0; i < 9; i++) {
			menu.getSlot(1 + i).set(ItemStack.EMPTY);
		}
		return result;
	}

	private static void assertMakes(GameTestHelper helper, ItemStack result, Item item, int count, String what, List<String> log) {
		helper.assertTrue(result.is(item) && result.getCount() == count, what + " should make " + count + " " + item + ", made " + result);
		log.add(what + ": " + result);
	}

	private static void assertNothing(GameTestHelper helper, ItemStack result, String what, List<String> log) {
		helper.assertTrue(result.isEmpty(), what + " should make nothing, made " + result);
		log.add(what + ": nothing");
	}

	/** REC-01..04 (M06). */
	@GameTest
	public void cableRecipesUseTheMiddleColumn(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		List<String> log = new ArrayList<>();
		assertMakes(helper, craft(helper, player, A, N, A, A, N, A, A, N, A), C, 2, "3 nuggets, middle column", log);
		assertNothing(helper, craft(helper, player, N, A, A, N, A, A, N, A, A), "3 nuggets, left column", log);
		assertNothing(helper, craft(helper, player, A, A, N, A, A, N, A, A, N), "3 nuggets, right column", log);
		assertNothing(helper, craft(helper, player, N, N, N, Items.STRING, A, A, A, A, A), "old recipe: 3 nuggets + string", log);
		assertNothing(helper, craft(helper, player, A, N, Items.STRING, A, N, A, A, N, A), "middle column plus string", log);

		assertMakes(helper, craft(helper, player, A, C, A, A, C, A, A, C, A), CableContent.CABLE_COIL, 1, "3 cables, middle column", log);
		assertNothing(helper, craft(helper, player, C, A, A, C, A, A, C, A, A), "3 cables, left column", log);
		assertNothing(helper, craft(helper, player, C, C, C, A, A, A, A, A, A), "3 cables, top row", log);
		assertNothing(helper, craft(helper, player, C, C, C, C, A, C, C, C, C), "old recipe: 8 cables in a ring", log);

		assertMakes(helper, craft(helper, player, A, A, A, A, CableContent.CABLE_COIL, A, A, A, A), C, 3, "a coil taken apart", log);
		SimpleBlockTestLog.info("REC-01..04: {}", log);
		TestPlayers.remove(player);
		helper.succeed();
	}
}
