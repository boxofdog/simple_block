package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;

/**
 * M08 heavy-load amendment: equipment keeps its weight but is always admitted; only commodities
 * (ore, raw metal, ingots, nuggets, diamonds, metal blocks) are refused once the whole load leaves
 * no room. Each test fails on the M02 rule, which refused any weighted item at the cap.
 */
public class CommodityCapGameTests {
	private static final int INGOT = HeavyLoad.UNITS_PER_INGOT;

	private static ServerPlayer survivor(GameTestHelper helper) {
		return TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
	}

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

	private static int droppedNear(GameTestHelper helper, ServerPlayer player) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(5)).size();
	}

	/** At exactly 64 ingots, rails, a bucket, a lantern, a minecart and a pickaxe are all picked up; commodities are not. */
	@GameTest
	public void equipmentFitsAtTheCapCommoditiesDoNot(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 64));
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "loaded to the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAIL, 16)), 16, "rails at the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.BUCKET)), 1, "bucket at the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.LANTERN)), 1, "lantern at the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.MINECART)), 1, "minecart at the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_PICKAXE)), 1, "pickaxe at the cap");
		int expected = 64 * INGOT + 16 * HeavyLoad.unitWeight(new ItemStack(Items.RAIL)) + HeavyLoad.unitWeight(new ItemStack(Items.BUCKET))
			+ HeavyLoad.unitWeight(new ItemStack(Items.LANTERN)) + HeavyLoad.unitWeight(new ItemStack(Items.MINECART))
			+ HeavyLoad.unitWeight(new ItemStack(Items.IRON_PICKAXE));
		helper.assertValueEqual(HeavyLoad.carried(player), expected, "equipment weight still counts, past 64");
		for (Item commodity : List.of(Items.IRON_INGOT, Items.RAW_IRON, Items.IRON_ORE, Items.IRON_BLOCK, Items.IRON_NUGGET, Items.DIAMOND,
			Items.DIAMOND_ORE, Items.DEEPSLATE_GOLD_ORE, Items.RAW_COPPER_BLOCK, Items.GOLD_NUGGET, HeavyLoadGameTests.item("copper_block"), Items.NETHERITE_INGOT)) {
			helper.assertValueEqual(offer(helper, player, new ItemStack(commodity)), 0, BuiltInRegistries.ITEM.getKey(commodity) + " over the cap");
		}
		helper.assertValueEqual(droppedNear(helper, player), 0, "nothing the player carried was dropped");
		helper.assertValueEqual(inventory.countItem(Items.IRON_INGOT), 64, "the 64 ingots are kept");
		SimpleBlockTestLog.info("M08-W1: at 64 ingots, 16 rails + bucket + lantern + minecart + iron pickaxe picked up (load {}); ingots, raw ore, "
			+ "ore blocks, metal blocks, nuggets, diamonds and netherite refused", HeavyLoad.describe(HeavyLoad.carried(player)));
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** Equipment uses up the shared allowance: 20 units of minecarts leave room for exactly 44 ingots, not a separate 64. */
	@GameTest
	public void equipmentLeavesLessRoomForCommodities(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.MINECART));
		inventory.setItem(19, new ItemStack(Items.MINECART));
		inventory.setItem(20, new ItemStack(Items.MINECART));
		inventory.setItem(21, new ItemStack(Items.MINECART));
		helper.assertValueEqual(HeavyLoad.carried(player), 20 * INGOT, "four minecarts weigh 20");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_INGOT, 64)), 44, "ingots taken with 20 of equipment carried");
		helper.assertValueEqual(HeavyLoad.carried(player), 64 * INGOT, "exactly at the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAW_IRON)), 0, "raw iron at the cap");
		SimpleBlockTestLog.info("M08-W2: 4 minecarts (20) + 64 offered ingots took 44; the allowance is shared, not a separate 64 for ore");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * Over the cap by equipment alone (3 anvils, 93): more equipment is still picked up, commodities
	 * are refused, nothing is dropped; dropping the anvils frees room again.
	 */
	@GameTest
	public void overTheCapByEquipmentAlone(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.ANVIL, 3));
		helper.assertValueEqual(HeavyLoad.carried(player), 93 * INGOT, "three anvils weigh 93");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAIL, 8)), 8, "rails over the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.WATER_BUCKET)), 1, "water bucket over the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_NUGGET)), 0, "a nugget over the cap");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAW_GOLD)), 0, "raw gold over the cap");
		helper.assertValueEqual(droppedNear(helper, player), 0, "nothing dropped while over the cap");
		helper.assertValueEqual(inventory.countItem(Items.ANVIL), 3, "anvils kept");
		inventory.setItem(18, ItemStack.EMPTY);
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.RAW_GOLD, 64)), 64 - 3 - 3, "room again after setting the anvils down");
		SimpleBlockTestLog.info("M08-W3: 93 of anvils: rails and a water bucket still picked up, nugget and raw gold refused, nothing dropped; "
			+ "without the anvils raw gold fits into the room the rails and bucket leave");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** Resource first, equipment second: 64 ingots then a pickaxe; both kept, load 67, no further ore. */
	@GameTest
	public void resourcesFirstThenEquipment(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_INGOT, 64)), 64, "64 ingots");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_PICKAXE)), 1, "then a pickaxe");
		helper.assertValueEqual(HeavyLoad.carried(player), 67 * INGOT, "64 + 3");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_ORE)), 0, "no more ore");
		helper.assertValueEqual(inventory.countItem(Items.IRON_INGOT) + inventory.countItem(Items.IRON_PICKAXE), 65, "both kept");
		SimpleBlockTestLog.info("M08-W4: 64 ingots then a pickaxe: both kept at 67/64, iron ore refused");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * Container paths at the cap: shift-click and cursor pickup of equipment from a chest succeed;
	 * shift-click of ingots moves none; a number-key swap that brings in gold blocks is undone, while
	 * one that brings in a minecart is not; a bundle of ingots stays put, a bundle of rails is taken;
	 * swapping a carried rail for a chest ingot is undone.
	 */
	@GameTest
	public void containerTransfersAtTheCap(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 64));
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.RAIL, 32));
		chest.setItem(1, new ItemStack(Items.IRON_INGOT, 10));
		chest.setItem(2, new ItemStack(Items.GOLD_BLOCK, 2));
		chest.setItem(3, new ItemStack(Items.MINECART));
		chest.setItem(4, new ItemStack(Items.BUCKET));
		ItemStack ingotBundle = new ItemStack(Items.BUNDLE);
		ingotBundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.IRON_INGOT, 4)))));
		chest.setItem(5, ingotBundle);
		ItemStack railBundle = new ItemStack(Items.BUNDLE);
		railBundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.RAIL, 8)))));
		chest.setItem(6, railBundle);
		ChestMenu menu = InventoryGameTests.openChest(player, chest);

		menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(chest.getItem(0).isEmpty(), "all 32 rails shift-clicked in at the cap");
		menu.clicked(1, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(chest.getItem(1).getCount(), 10, "ingots not shift-clicked in over the cap");
		menu.clicked(1, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(menu.getCarried().isEmpty(), "ingots not picked onto the cursor over the cap");
		menu.clicked(2, 3, ContainerInput.SWAP, player);
		helper.assertValueEqual(chest.getItem(2).getCount(), 2, "number-key swap of gold blocks undone");
		menu.clicked(3, 3, ContainerInput.SWAP, player);
		helper.assertTrue(chest.getItem(3).isEmpty(), "number-key swap of a minecart kept");
		menu.clicked(4, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(menu.getCarried().is(Items.BUCKET), "bucket picked onto the cursor over the cap");
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, 30), 0, ContainerInput.PICKUP, player);
		menu.clicked(5, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(chest.getItem(5).is(Items.BUNDLE), "a bundle of ingots stays in the chest");
		menu.clicked(6, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(chest.getItem(6).isEmpty(), "a bundle of rails is taken");
		// Swap a carried rail stack on the cursor for the chest's ingots: the click is undone.
		int rails = -1;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(Items.RAIL)) {
				rails = i;
			}
		}
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, rails), 0, ContainerInput.PICKUP, player);
		helper.assertTrue(menu.getCarried().is(Items.RAIL), "rails on the cursor");
		menu.clicked(1, 0, ContainerInput.PICKUP, player);
		helper.assertTrue(menu.getCarried().is(Items.RAIL), "swap for ingots undone, rails still on the cursor");
		helper.assertValueEqual(chest.getItem(1).getCount(), 10, "ingots still in the chest");
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, rails), 0, ContainerInput.PICKUP, player);
		player.closeContainer();
		helper.assertValueEqual(inventory.countItem(Items.IRON_INGOT), 64, "ingots unchanged");
		helper.assertValueEqual(inventory.countItem(Items.RAIL), 32, "32 rails taken");
		SimpleBlockTestLog.info("M08-W5: at 64 ingots, chest rails/minecart/bucket/rail bundle came in by shift-click, number key and cursor; "
			+ "ingots, gold blocks, an ingot bundle and a rail-for-ingot swap were refused or undone; load {}",
			HeavyLoad.describe(HeavyLoad.carried(player)));
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * Conversions conserve weight and open no bypass: over the cap (64 ingots + rails), an iron
	 * block in the 2x2 grid still crafts into 9 ingots; a crafting table makes rails from 6 ingots at
	 * the cap (equipment out), but a gold block made from 9 table ingots is refused with its
	 * ingredients kept.
	 */
	@GameTest
	public void conversionsAtTheCap(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 55));
		inventory.setItem(19, new ItemStack(Items.RAIL, 8));
		InventoryMenu own = player.inventoryMenu;
		own.getSlot(InventoryMenu.CRAFT_SLOT_START).set(new ItemStack(Items.IRON_BLOCK));
		helper.assertValueEqual(HeavyLoad.carried(player), 67 * INGOT, "55 + block in the grid + 8 rails");
		helper.assertTrue(own.getSlot(InventoryMenu.RESULT_SLOT).getItem().is(Items.IRON_INGOT), "block -> ingots offered");
		own.clicked(InventoryMenu.RESULT_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.countItem(Items.IRON_INGOT), 64, "block crafted into 9 ingots over the cap");
		helper.assertValueEqual(HeavyLoad.carried(player), 67 * INGOT, "weight conserved");

		BlockPos table = helper.absolutePos(new BlockPos(2, 1, 1));
		level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
		player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(level, table)),
			Component.literal("crafting")));
		CraftingMenu crafting = (CraftingMenu) player.containerMenu;
		for (int grid : new int[] {1, 3, 4, 6, 7, 9}) {
			crafting.getSlot(grid).set(new ItemStack(Items.IRON_INGOT));
		}
		crafting.getSlot(5).set(new ItemStack(Items.STICK));
		helper.assertTrue(crafting.getSlot(0).getItem().is(Items.RAIL), "rail result");
		crafting.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.countItem(Items.RAIL), 8 + 16, "16 rails crafted over the cap");
		for (int grid = 1; grid <= 9; grid++) {
			crafting.getSlot(grid).set(new ItemStack(Items.GOLD_INGOT));
		}
		helper.assertTrue(crafting.getSlot(0).getItem().is(Items.GOLD_BLOCK), "gold block result");
		crafting.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		crafting.clicked(0, 0, ContainerInput.PICKUP, player);
		helper.assertValueEqual(inventory.countItem(Items.GOLD_BLOCK), 0, "gold block refused over the cap");
		helper.assertTrue(crafting.getCarried().isEmpty(), "nothing on the cursor");
		helper.assertTrue(crafting.getSlot(1).getItem().is(Items.GOLD_INGOT), "gold ingredients kept in the grid");
		for (int grid = 1; grid <= 9; grid++) {
			crafting.getSlot(grid).set(ItemStack.EMPTY);
		}
		player.closeContainer();
		SimpleBlockTestLog.info("M08-W6: over the cap, iron block -> 9 ingots in the 2x2 grid conserved 67; table rails crafted; gold block "
			+ "from table ingots refused with ingredients kept");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** Offhand counts and swapping carried items between hand and offhand stays free over the cap. */
	@GameTest
	public void offhandOverTheCap(GameTestHelper helper) {
		ServerPlayer player = survivor(helper);
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 64));
		player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.LANTERN, 4));
		helper.assertTrue(HeavyLoad.carried(player) > 64 * INGOT, "offhand lanterns count");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.LANTERN, 4)), 4, "lanterns fill the offhand stack over the cap");
		helper.assertValueEqual(player.getOffhandItem().getCount(), 8, "eight lanterns in the offhand");
		player.inventoryMenu.clicked(InventoryGameTests.menuIndex(player.inventoryMenu, inventory, 18), 40, ContainerInput.SWAP, player);
		helper.assertTrue(player.getOffhandItem().is(Items.IRON_INGOT), "ingots swapped into the offhand");
		helper.assertValueEqual(inventory.countItem(Items.LANTERN), 8, "lanterns swapped into the inventory");
		helper.assertValueEqual(offer(helper, player, new ItemStack(Items.IRON_INGOT)), 0, "no more ingots");
		SimpleBlockTestLog.info("M08-W7: offhand lanterns counted and topped up over the cap; F-swap of carried stacks free; ingots refused");
		clearDrops(helper, player);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * Commodity audit: every tagged commodity has a weight; nuggets, diamonds, raw ore, ore blocks
	 * and metal blocks are commodities; tools, armor, rails, buckets, lanterns, minecarts and this
	 * mod's freight equipment are not.
	 */
	@GameTest
	public void commodityClassificationAudit(GameTestHelper helper) {
		Map<Item, Integer> table = HeavyLoad.table();
		List<String> unweighted = new ArrayList<>();
		List<String> commodities = new ArrayList<>();
		for (Item item : table.keySet()) {
			if (HeavyLoad.isCommodity(new ItemStack(item))) {
				commodities.add(BuiltInRegistries.ITEM.getKey(item).getPath());
			}
		}
		BuiltInRegistries.ITEM.getTagOrEmpty(HeavyLoad.COMMODITIES).forEach(holder -> {
			if (!table.containsKey(holder.value())) {
				unweighted.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString());
			}
		});
		helper.assertTrue(unweighted.isEmpty(), "commodities without a weight: " + unweighted);
		for (Item item : List.of(Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.COPPER_NUGGET, Items.DIAMOND, Items.RAW_IRON, Items.RAW_GOLD_BLOCK,
			Items.IRON_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.NETHER_GOLD_ORE, Items.IRON_BLOCK, HeavyLoadGameTests.item("waxed_oxidized_copper"), HeavyLoadGameTests.item("cut_copper_slab"),
			Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, Items.DIAMOND_BLOCK)) {
			helper.assertTrue(HeavyLoad.isCommodity(new ItemStack(item)), BuiltInRegistries.ITEM.getKey(item) + " should be a commodity");
		}
		List<String> equipment = new ArrayList<>();
		for (Item item : table.keySet()) {
			String path = BuiltInRegistries.ITEM.getKey(item).getPath();
			boolean gear = path.matches(".*_(pickaxe|axe|shovel|hoe|sword|spear|helmet|chestplate|leggings|boots|horse_armor|nautilus_armor)")
				|| path.endsWith("rail") || path.endsWith("bucket") || path.endsWith("lantern") || path.endsWith("minecart")
				|| BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("simple_block");
			if (gear && HeavyLoad.isCommodity(new ItemStack(item))) {
				equipment.add(path);
			}
		}
		helper.assertTrue(equipment.isEmpty(), "equipment wrongly restricted: " + equipment);
		SimpleBlockTestLog.info("M08-W8 commodity audit: {} of {} weighted items are cap-restricted commodities: {}", commodities.size(), table.size(),
			commodities.stream().sorted().toList());
		helper.succeed();
	}
}
