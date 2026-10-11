package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.feature.InventorySort;

/** INV-01 (27 usable slots), INV-02 (sort) and INV-03 (hotbar fills from the left). */
public class InventoryGameTests {
	static ChestMenu openChest(ServerPlayer player, SimpleContainer container) {
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, container), Component.literal("test chest")));
		return (ChestMenu) player.containerMenu;
	}

	static int menuIndex(AbstractContainerMenu menu, Inventory inventory, int inventorySlot) {
		for (Slot slot : menu.slots) {
			if (slot.container == inventory && slot.getContainerSlot() == inventorySlot) {
				return slot.index;
			}
		}
		throw new IllegalStateException("inventory slot " + inventorySlot + " not in menu");
	}

	static int usableInventorySlots(AbstractContainerMenu menu, ServerPlayer player) {
		int usable = 0;
		for (Slot slot : menu.slots) {
			if (slot.container == player.getInventory() && slot.getContainerSlot() < 36
				&& slot.isActive() && slot.mayPlace(new ItemStack(Items.DIRT)) && slot.mayPickup(player)) {
				usable++;
			}
		}
		return usable;
	}

	/** INV-01: the survival inventory, a chest and a furnace each offer exactly 18 + 9 usable inventory slots. */
	@GameTest
	public void menusOfferExactly27Slots(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Map<String, Integer> counts = new HashMap<>();
		counts.put("inventory", usableInventorySlots(player.inventoryMenu, player));
		counts.put("chest", usableInventorySlots(openChest(player, new SimpleContainer(27)), player));
		player.closeContainer();
		BlockPos furnacePos = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlockAndUpdate(furnacePos, Blocks.FURNACE.defaultBlockState());
		player.openMenu((FurnaceBlockEntity) level.getBlockEntity(furnacePos));
		counts.put("furnace", usableInventorySlots(player.containerMenu, player));
		player.closeContainer();
		SimpleBlockTestLog.info("INV-01 usable player slots per menu: {}", counts);
		counts.forEach((menu, count) -> helper.assertValueEqual(count, 27, menu + " usable inventory slots"));
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** INV-01: pickups fill the 27 slots and then stop; the disabled row never receives anything. */
	@GameTest
	public void pickupsNeverUseTheDisabledRow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Inventory inventory = player.getInventory();
		int picked = 0;
		for (int i = 0; i < 30; i++) {
			ItemEntity drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), new ItemStack(Items.WOODEN_SWORD));
			drop.setNoPickUpDelay();
			level.addFreshEntity(drop);
			drop.playerTouch(player);
			picked += drop.isRemoved() ? 1 : 0;
		}
		for (int slot = InventorySlots.FIRST_DISABLED; slot <= InventorySlots.LAST_DISABLED; slot++) {
			helper.assertTrue(inventory.getItem(slot).isEmpty(), "disabled slot " + slot + " received " + inventory.getItem(slot));
		}
		SimpleBlockTestLog.info("INV-01 pickups: 30 swords offered, {} picked up, disabled row empty", picked);
		helper.assertValueEqual(picked, 27, "swords picked up");
		level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(3)).forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * INV-01: items that get into the disabled row anyway (commands, creative edits) are moved to a
	 * usable slot on the next tick, or dropped at the player's feet when there is no room; never deleted.
	 */
	@GameTest
	public void itemsInTheDisabledRowAreMovedOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Inventory inventory = player.getInventory();
		inventory.setItem(12, new ItemStack(Items.DIAMOND, 5));
		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				helper.assertTrue(inventory.getItem(12).isEmpty(), "slot 12 should have been emptied");
				helper.assertValueEqual(inventory.countItem(Items.DIAMOND), 5, "diamonds kept in usable slots");
				// Now with every usable slot full: the item must be dropped, not deleted.
				for (int slot = 0; slot < 36; slot++) {
					if (!InventorySlots.isDisabled(slot)) {
						inventory.setItem(slot, new ItemStack(Items.STONE, 64));
					}
				}
				inventory.setItem(15, new ItemStack(Items.EMERALD, 7));
			})
			.thenIdle(1)
			.thenExecute(() -> {
				helper.assertTrue(inventory.getItem(15).isEmpty(), "slot 15 should have been emptied");
				int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4),
					entity -> entity.getItem().is(Items.EMERALD)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
				helper.assertValueEqual(dropped, 7, "emeralds dropped at the player's feet");
				SimpleBlockTestLog.info("INV-01 eviction: 5 diamonds moved to usable slots; with a full inventory 7 emeralds dropped, none lost");
				level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4)).forEach(ItemEntity::discard);
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}

	/** INV-01: clicking an item onto a disabled-row slot (or shift-clicking into it) does nothing. */
	@GameTest
	public void disabledRowRefusesClicks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		SimpleContainer chest = new SimpleContainer(27);
		ChestMenu menu = openChest(player, chest);
		Inventory inventory = player.getInventory();
		menu.setCarried(new ItemStack(Items.OAK_LOG, 10));
		menu.clicked(menuIndex(menu, inventory, 10), 0, ContainerInput.PICKUP, player);
		helper.assertTrue(inventory.getItem(10).isEmpty(), "disabled slot accepted the cursor stack");
		helper.assertValueEqual(menu.getCarried().getCount(), 10, "cursor stack kept");
		menu.setCarried(ItemStack.EMPTY);
		// Fill every usable slot, then shift-click from the chest: the move must stop, not spill into the row.
		for (int slot = 0; slot < 36; slot++) {
			if (!InventorySlots.isDisabled(slot)) {
				inventory.setItem(slot, new ItemStack(Items.STONE, 64));
			}
		}
		chest.setItem(0, new ItemStack(Items.OAK_LOG, 32));
		menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		for (int slot = InventorySlots.FIRST_DISABLED; slot <= InventorySlots.LAST_DISABLED; slot++) {
			helper.assertTrue(inventory.getItem(slot).isEmpty(), "shift-click filled disabled slot " + slot);
		}
		helper.assertValueEqual(chest.getItem(0).getCount(), 32, "logs left in the chest");
		SimpleBlockTestLog.info("INV-01 clicks: cursor placement and shift-click both refused by the disabled row");
		player.closeContainer();
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * INV-03: shift-clicking items out of a chest into an empty hotbar fills hotbar slots 1, 2, 3
	 * (vanilla: 9, 8, 7). Existing stacks still get topped up first.
	 */
	@GameTest
	public void shiftClickFillsHotbarFromTheLeft(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Inventory inventory = player.getInventory();
		// The storage rows are full, so the transfers go to the hotbar.
		for (int slot = 9; slot < 36; slot++) {
			if (!InventorySlots.isDisabled(slot)) {
				inventory.setItem(slot, new ItemStack(Items.STONE, 64));
			}
		}
		inventory.setItem(30, new ItemStack(Items.COBBLESTONE, 10));
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.IRON_SHOVEL));
		chest.setItem(1, new ItemStack(Items.WOODEN_AXE));
		chest.setItem(2, new ItemStack(Items.STONE_HOE));
		chest.setItem(3, new ItemStack(Items.COBBLESTONE, 20));
		ChestMenu menu = openChest(player, chest);
		for (int i = 0; i < 4; i++) {
			menu.clicked(i, 0, ContainerInput.QUICK_MOVE, player);
		}
		List<String> hotbar = new ArrayList<>();
		for (int slot = 0; slot < 9; slot++) {
			hotbar.add(inventory.getItem(slot).isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(inventory.getItem(slot).getItem()).getPath());
		}
		SimpleBlockTestLog.info("INV-03 hotbar after shift-clicking shovel, axe, hoe, cobblestone from a chest: {}; slot 30 cobblestone {}",
			hotbar, inventory.getItem(30).getCount());
		helper.assertTrue(inventory.getItem(0).is(Items.IRON_SHOVEL), "first transfer should land in hotbar slot 1");
		helper.assertTrue(inventory.getItem(1).is(Items.WOODEN_AXE), "second transfer should land in hotbar slot 2");
		helper.assertTrue(inventory.getItem(2).is(Items.STONE_HOE), "third transfer should land in hotbar slot 3");
		helper.assertValueEqual(inventory.getItem(30).getCount(), 30, "cobblestone merged into the existing stack");
		helper.assertTrue(inventory.getItem(3).isEmpty(), "merged cobblestone should not take a hotbar slot");
		player.closeContainer();
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * INV-02: Sort merges compatible stacks, orders by item registry id, fills the upper storage row
	 * left to right and then the bottom row, keeps different durability/names apart, loses nothing and
	 * leaves the hotbar and offhand alone.
	 */
	@GameTest
	public void sortMergesAndOrdersTopRowFirst(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Inventory inventory = player.getInventory();
		ItemStack wornPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		wornPickaxe.setDamageValue(100);
		ItemStack namedCobble = new ItemStack(Items.COBBLESTONE, 5);
		namedCobble.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
		inventory.setItem(18, new ItemStack(Items.COBBLESTONE, 40));
		inventory.setItem(20, new ItemStack(Items.DIRT, 5));
		inventory.setItem(22, wornPickaxe.copy());
		inventory.setItem(25, new ItemStack(Items.DIAMOND_PICKAXE));
		inventory.setItem(27, namedCobble.copy());
		inventory.setItem(31, new ItemStack(Items.COBBLESTONE, 40));
		inventory.setItem(35, new ItemStack(Items.APPLE, 3));
		inventory.setItem(0, new ItemStack(Items.TORCH, 16));
		inventory.setItem(4, new ItemStack(Items.COBBLESTONE, 7));
		player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
		Map<Item, Integer> before = countAll(inventory);

		InventorySort.sort(inventory);

		List<String> layout = new ArrayList<>();
		for (int slot : InventorySlots.SORT_ORDER) {
			ItemStack stack = inventory.getItem(slot);
			layout.add(slot + ":" + (stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "x" + stack.getCount()
				+ (stack.isDamaged() ? "(damaged)" : "") + (stack.has(DataComponents.CUSTOM_NAME) ? "(named)" : "")));
		}
		SimpleBlockTestLog.info("INV-02 sorted storage (top row first): {}", layout);
		helper.assertValueEqual(countAll(inventory), before, "item counts after sorting");
		helper.assertTrue(inventory.getItem(0).is(Items.TORCH) && inventory.getItem(0).getCount() == 16, "hotbar slot 1 untouched");
		helper.assertTrue(inventory.getItem(4).is(Items.COBBLESTONE) && inventory.getItem(4).getCount() == 7, "hotbar cobblestone not merged");
		helper.assertTrue(player.getOffhandItem().is(Items.SHIELD), "offhand untouched");

		// Expected: registry order; cobblestone 64 + 16 (merged 40 + 40), named cobblestone apart, two pickaxes apart.
		List<ItemStack> sorted = new ArrayList<>();
		for (int slot : InventorySlots.SORT_ORDER) {
			if (!inventory.getItem(slot).isEmpty()) {
				sorted.add(inventory.getItem(slot));
			}
		}
		helper.assertValueEqual(sorted.size(), 7, "stacks after merging");
		for (int i = 1; i < sorted.size(); i++) {
			helper.assertTrue(BuiltInRegistries.ITEM.getId(sorted.get(i - 1).getItem()) <= BuiltInRegistries.ITEM.getId(sorted.get(i).getItem()),
				"stacks out of registry order at " + i + ": " + layout);
		}
		int firstEmpty = -1;
		for (int i = 0; i < InventorySlots.SORT_ORDER.length; i++) {
			if (inventory.getItem(InventorySlots.SORT_ORDER[i]).isEmpty()) {
				firstEmpty = i;
				break;
			}
		}
		helper.assertValueEqual(firstEmpty, 7, "stacks packed from top-left; first empty position");
		helper.assertTrue(!inventory.getItem(18).isEmpty() && inventory.getItem(27).isEmpty(), "upper row filled before the bottom row");
		long pickaxes = sorted.stream().filter(stack -> stack.is(Items.DIAMOND_PICKAXE)).count();
		helper.assertValueEqual((int) pickaxes, 2, "pickaxes with different durability stay separate");
		long namedStacks = sorted.stream().filter(stack -> stack.has(DataComponents.CUSTOM_NAME)).count();
		helper.assertValueEqual((int) namedStacks, 1, "the named cobblestone stays separate");
		List<Integer> plainCobble = sorted.stream().filter(stack -> stack.is(Items.COBBLESTONE) && !stack.has(DataComponents.CUSTOM_NAME))
			.map(ItemStack::getCount).sorted(Comparator.reverseOrder()).toList();
		helper.assertValueEqual(plainCobble, List.of(64, 16), "plain cobblestone merged to the stack limit");
		TestPlayers.remove(player);
		helper.succeed();
	}

	static Map<Item, Integer> countAll(Inventory inventory) {
		Map<Item, Integer> counts = new HashMap<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		return counts;
	}
}
