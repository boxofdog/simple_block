package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;

/**
 * INV-04: one shared allowance of 64 ingot-equivalents for iron, gold and copper. The numbered
 * cases follow "Important QA cases" in docs/M02_INVENTORY_HEAVY.md.
 */
public class HeavyLoadGameTests {
	private static final int INGOT = HeavyLoad.UNITS_PER_INGOT;

	private static ServerPlayer survivor(GameTestHelper helper) {
		return TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
	}

	/** Offers a stack on the ground to the player; returns how many were picked up. */
	private static int offer(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
		ServerLevel level = helper.getLevel();
		int count = stack.getCount();
		ItemEntity drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
		drop.setNoPickUpDelay();
		level.addFreshEntity(drop);
		drop.playerTouch(player);
		int left = drop.isRemoved() ? 0 : drop.getItem().getCount();
		drop.discard();
		return count - left;
	}

	private static void clearDrops(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(5)).forEach(ItemEntity::discard);
	}

	/** The weights the design documents name, and every table entry resolves to a real item. */
	@GameTest
	public void weightTableMatchesTheDesign(GameTestHelper helper) {
		Object[][] expected = {
			{Items.IRON_INGOT, 1.0}, {Items.GOLD_INGOT, 1.0}, {Items.COPPER_INGOT, 1.0}, {Items.RAW_IRON, 1.0}, {Items.RAW_GOLD, 1.0},
			{Items.RAW_COPPER, 1.0}, {Items.IRON_NUGGET, 1 / 9.0}, {Items.GOLD_NUGGET, 1 / 9.0}, {Items.COPPER_NUGGET, 1 / 9.0},
			{Items.IRON_ORE, 1.0}, {Items.DEEPSLATE_IRON_ORE, 1.0}, {Items.GOLD_ORE, 1.0}, {Items.COPPER_ORE, 1.0}, {Items.NETHER_GOLD_ORE, 1.0},
			{Items.IRON_BLOCK, 9.0}, {Items.GOLD_BLOCK, 9.0}, {item("copper_block"), 9.0}, {item("oxidized_cut_copper"), 9 / 4.0}, {Items.RAW_IRON_BLOCK, 9.0},
			{Items.IRON_PICKAXE, 3.0}, {Items.BUCKET, 3.0}, {Items.WATER_BUCKET, 3.0}, {Items.RAIL, 3 / 8.0}, {Items.IRON_DOOR, 2.0},
			{Items.HOPPER, 5.0}, {Items.MINECART, 5.0}, {Items.ANVIL, 31.0}, {Items.IRON_CHESTPLATE, 8.0}, {Items.STONE, 0.0},
			{Items.DIAMOND, 0.0}, {Items.OAK_LOG, 0.0}};
		List<String> problems = new ArrayList<>();
		for (Object[] row : expected) {
			int units = HeavyLoad.unitWeight(new ItemStack((Item) row[0]));
			double ingots = (double) row[1];
			if (units != Math.round(ingots * INGOT)) {
				problems.add(row[0] + " weighs " + units + "/" + INGOT + ", expected " + ingots);
			}
		}
		Map<Item, Integer> table = HeavyLoad.table();
		SimpleBlockTestLog.info("HVY-04 weight table: {} items weigh something; {} design values checked; problems {}", table.size(),
			expected.length, problems);
		helper.assertTrue(table.size() > 250, "weight table should be loaded (" + table.size() + ")");
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** QA 1, 2: 64 iron ingots, or 32 iron + 32 gold, is full; no more iron, gold or copper is picked up. */
	@GameTest
	public void fullLoadRefusesAnyMoreMetal(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 64));
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "carried with 64 iron ingots");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_INGOT)), 0, "iron ingots picked up at 64");
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 32));
		inventory.setItem(19, new ItemStack(Items.GOLD_INGOT, 32));
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.COPPER_INGOT, 5)), 0, "copper picked up at 32 iron + 32 gold");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAW_GOLD, 1)), 0, "raw gold picked up when full");
		// Non-metal items are unaffected (HVY-09).
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.COBBLESTONE, 64)), 64, "cobblestone picked up when full");
		SimpleBlockTestLog.info("HVY-01/02: at 64 iron or 32 iron + 32 gold, iron/copper/raw gold pickups refused; 64 cobblestone still picked up");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** QA 3, 4, 5: whole items only; 63 + a block (9) is refused, 55 + a block fits exactly, nuggets count in ninths. */
	@GameTest
	public void exactIntegerAccounting(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 63));
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_BLOCK)), 0, "iron block picked up at 63");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.GOLD_NUGGET, 10)), 9, "nuggets picked up at 63 (room for 9 ninths)");
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "63 ingots + 9 nuggets = 64");
		inventory.clearContent();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 55));
		helper.assertValueEqual(offer(helper, player, new ItemStack(item("copper_block"))), 1, "copper block picked up at 55");
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "55 + 9 = exactly 64");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.COPPER_NUGGET)), 0, "one more nugget");
		// Rails are 3/8 each: 8 rails = 3 ingots exactly, 61 + 8 rails = 64.
		inventory.clearContent();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 61));
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAIL, 16)), 8, "rails picked up at 61 (3 ingots of room)");
		SimpleBlockTestLog.info("HVY-03/04: 63+block refused, 63+10 nuggets took 9, 55+block filled to exactly 64, 61+16 rails took 8; carried {}",
			HeavyLoad.describe(HeavyLoad.carried(player)));
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * QA 6 / HVY-05: no recipe in the game makes heavier items than its cheapest inputs, so no
	 * crafting chain can create carrying capacity. Two kinds can't be checked from their ingredient
	 * lists and are listed in the log instead: armor-trim recipes, which return the very piece they
	 * are given (weight unchanged), and the special recipes without fixed ingredients (map cloning
	 * and extending, repairing, banners, books, fireworks, shield decoration, decorated pots, dyeing),
	 * none of which turns lighter items into metal.
	 */
	@GameTest
	public void noRecipeCreatesCarryingCapacity(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ContextMap context = SlotDisplayContext.fromLevel(level);
		int scanned = 0;
		List<String> problems = new ArrayList<>();
		List<String> notCheckable = new ArrayList<>();
		for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			// Map cloning lists up to eight map slots but shows nine maps out: it turns one filled map plus n empty maps
			// into n + 1 filled maps, one per map in, so it is weight-neutral.
			if (holder.value() instanceof net.minecraft.world.item.crafting.SmithingTrimRecipe || holder.value().placementInfo().isImpossibleToPlace()
				|| holder.id().identifier().getPath().equals("map_cloning")) {
				notCheckable.add(holder.id().identifier().getPath());
				continue;
			}
			long input = 0;
			for (Ingredient ingredient : holder.value().placementInfo().ingredients()) {
				input += ingredient.items().mapToInt(item -> HeavyLoad.unitWeight(new ItemStack(item))).min().orElse(0);
			}
			long output = 0;
			for (RecipeDisplay display : holder.value().display()) {
				for (ItemStack result : display.result().resolveForStacks(context)) {
					output = Math.max(output, HeavyLoad.weight(result));
				}
			}
			if (output > input) {
				problems.add(holder.id().identifier() + ": in " + input + ", out " + output);
			}
			scanned++;
		}
		SimpleBlockTestLog.info("HVY-05 recipe audit: {} recipes checked, {} make more metal than their cheapest inputs {}; not checkable from ingredients: {}",
			scanned, problems.size(), problems, notCheckable);
		helper.assertTrue(scanned > 1000, "recipes scanned: " + scanned);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems.stream().limit(5).toList()));
		helper.succeed();
	}

	/**
	 * QA 6 in a real crafting table: at 64 carried, moving 9 ingots into the grid and taking the
	 * block works (64 again). In the 2x2 grid, an ingot -> 9 nuggets craft at the cap also works,
	 * because the ingredient leaves the carried grid as the result arrives.
	 */
	@GameTest
	public void craftingAtTheCapIsWeightNeutral(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		BlockPos table = helper.absolutePos(new BlockPos(2, 1, 1));
		level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 64));
		player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(level, table)),
			Component.literal("crafting")));
		CraftingMenu crafting = (CraftingMenu) player.containerMenu;
		int ingots = InventoryGameTests.menuIndex(crafting, inventory, 18);
		// Right-click-drag equivalent: put one ingot in each of the 9 grid slots.
		crafting.clicked(ingots, 0, ContainerInput.PICKUP, player);
		for (int grid = 1; grid <= 9; grid++) {
			crafting.clicked(grid, 1, ContainerInput.PICKUP, player);
		}
		crafting.clicked(ingots, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(crafting.getSlot(0).getItem().is(Items.IRON_BLOCK), "grid should show an iron block result");
		crafting.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.countItem(Items.IRON_BLOCK), 1, "iron block crafted at the cap");
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "carried after crafting the block");
		player.closeContainer();

		// 2x2 grid: one ingot in, nine nuggets out, while carrying exactly 64.
		inventory.clearContent();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 64));
		InventoryMenu menu = player.inventoryMenu;
		int stack = InventoryGameTests.menuIndex(menu, inventory, 18);
		menu.clicked(stack, 0, ContainerInput.PICKUP, player);
		menu.clicked(InventoryMenu.CRAFT_SLOT_START, 1, ContainerInput.PICKUP, player);
		menu.clicked(stack, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "carried with one ingot in the 2x2 grid");
		menu.clicked(InventoryMenu.RESULT_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.countItem(Items.IRON_NUGGET), 9, "nuggets crafted at the cap from the 2x2 grid");
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "carried after the 2x2 craft");
		SimpleBlockTestLog.info("HVY-05 crafting at the cap: 9 ingots -> iron block in a table, 1 ingot -> 9 nuggets in the 2x2 grid; load stays 64");
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** QA 7, 8: offhand and cursor count; worn armor does not, and can't be unequipped into overload. */
	@GameTest
	public void offhandCursorCountWornArmorDoesNot(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 60));
		player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLD_INGOT, 4));
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "inventory 60 + offhand 4");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_NUGGET)), 0, "nugget picked up with a full offhand load");
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		helper.assertValueEqual(HeavyLoad.carried(player), 60 * INGOT, "worn chestplate is exempt");

		// Shift-clicking the worn chestplate into the inventory would add 8: refused, it stays on.
		player.inventoryMenu.clicked(6, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "chestplate should stay worn");
		// Right-click equipping a leather chestplate would put the iron one in the hand: refused.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEATHER_CHESTPLATE));
		player.getItemInHand(InteractionHand.MAIN_HAND).use(player.level(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "equip swap should be refused");
		helper.assertTrue(player.getMainHandItem().is(Items.LEATHER_CHESTPLATE), "leather chestplate stays in hand");

		// The cursor counts too: 54 in the inventory + 10 on the cursor is full.
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.IRON_INGOT, 10));
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 54));
		ChestMenu menu = InventoryGameTests.openChest(player, chest);
		menu.clicked(0, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(menu.getCarried().getCount(), 10, "10 ingots on the cursor");
		chest.setItem(1, new ItemStack(Items.IRON_INGOT, 5));
		menu.clicked(1, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(chest.getItem(1).getCount(), 5, "shift-click refused while the cursor holds the rest of the allowance");
		menu.clicked(0, 0, ContainerInput.PICKUP, player);
		player.closeContainer();
		SimpleBlockTestLog.info("HVY-06: offhand and cursor counted; worn chestplate exempt, but shift-click unequip and equip-swap into overload refused");
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * QA 9: shift-click from a chest moves only what fits and leaves the rest; taking from a furnace
	 * output takes what fits; a crafting result that does not fit stays put with its ingredients.
	 */
	@GameTest
	public void transfersLeaveTheRemainderInTheSource(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 40));
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
		ChestMenu menu = InventoryGameTests.openChest(player, chest);
		menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(chest.getItem(0).getCount(), 40, "chest keeps what did not fit");
		helper.assertValueEqual(inventory.countItem(Items.IRON_INGOT), 64, "player took 24");
		// Number-key swap of another heavy stack into the hotbar would overload: the click is undone.
		chest.setItem(1, new ItemStack(Items.GOLD_BLOCK, 3));
		menu.clicked(1, 2, ContainerInput.SWAP, player);
		helper.assertValueEqual(chest.getItem(1).getCount(), 3, "swap undone, gold blocks still in the chest");
		player.closeContainer();

		BlockPos furnacePos = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
		FurnaceBlockEntity furnace = (FurnaceBlockEntity) level.getBlockEntity(furnacePos);
		furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 20));
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 60));
		player.openMenu(furnace);
		player.containerMenu.clicked(2, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(player.containerMenu.getCarried().getCount(), 4, "took 4 from the furnace output onto the cursor");
		helper.assertValueEqual(furnace.getItem(2).getCount(), 16, "furnace keeps 16");
		player.closeContainer();
		helper.assertValueEqual(inventory.countItem(Items.IRON_INGOT), 64, "cursor ingots returned to the inventory on close");

		BlockPos table = helper.absolutePos(new BlockPos(0, 1, 2));
		level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 60));
		player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(level, table)),
			Component.literal("crafting")));
		CraftingMenu crafting = (CraftingMenu) player.containerMenu;
		for (int grid = 1; grid <= 9; grid++) {
			crafting.getSlot(grid).set(new ItemStack(Items.GOLD_INGOT));
		}
		helper.assertTrue(crafting.getSlot(0).getItem().is(Items.GOLD_BLOCK), "gold block result");
		crafting.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		crafting.clicked(0, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(inventory.countItem(Items.GOLD_BLOCK), 0, "gold block taken at 60");
		helper.assertTrue(crafting.getCarried().isEmpty(), "nothing on the cursor");
		helper.assertTrue(crafting.getSlot(1).getItem().is(Items.GOLD_INGOT), "ingredients still in the grid");
		helper.assertTrue(crafting.getSlot(0).getItem().is(Items.GOLD_BLOCK), "result still offered");
		player.closeContainer();
		int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(5),
			entity -> entity.getItem().is(Items.GOLD_INGOT)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
		helper.assertValueEqual(inventory.countItem(Items.GOLD_INGOT) + dropped, 9, "grid ingots returned or dropped on close, none lost");
		SimpleBlockTestLog.info("HVY-07: chest shift-click moved 24 of 64; number-key swap undone; furnace output gave 4 of 20; crafting result "
			+ "refused at 60 with ingredients kept; closing the table returned {} ingots and dropped {}", inventory.countItem(Items.GOLD_INGOT), dropped);
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** Bundle contents count, creative ignores the cap, and leaving creative keeps the overload without deleting it. */
	@GameTest
	public void bundlesCreativeAndModeChanges(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(
			net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.IRON_INGOT, 10)))));
		inventory.setItem(18, bundle);
		helper.assertValueEqual(HeavyLoad.carried(player), 10 * INGOT, "bundle with 10 ingots");

		inventory.clearContent();
		player.setGameMode(GameType.CREATIVE);
		inventory.setItem(18, new ItemStack(Items.IRON_BLOCK, 64));
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.GOLD_INGOT, 5)), 5, "creative picks up past the cap");
		player.setGameMode(GameType.SURVIVAL);
		helper.assertValueEqual(inventory.countItem(Items.IRON_BLOCK), 64, "overload kept after switching to survival");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_NUGGET)), 0, "no new heavy intake while over the cap");
		inventory.setItem(18, ItemStack.EMPTY);
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_NUGGET)), 1, "intake resumes once under the cap");
		SimpleBlockTestLog.info("HVY-06/07: bundle contents counted; creative ignores the cap; survival keeps a creative overload and blocks intake until under 64");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** HVY-09: no speed penalty at the cap. */
	@GameTest
	public void noSpeedPenaltyAtTheCap(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		double empty = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		player.getInventory().setItem(18, new ItemStack(Items.IRON_BLOCK, 7));
		player.getInventory().setItem(19, new ItemStack(Items.IRON_INGOT, 1));
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "loaded to the cap");
		helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED), empty, "movement speed at the cap");
		SimpleBlockTestLog.info("HVY-09: movement speed {} empty and at 64/64", empty);
		TestPlayers.remove(player);
		helper.succeed();
	}

	static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path));
	}

	static String id(Holder<Item> item) {
		return BuiltInRegistries.ITEM.getKey(item.value()).toString();
	}
}
