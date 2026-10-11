package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * M03 FRT-10..12: ender chests and chest minecarts are out of ordinary play; donkeys, mules and
 * llamas still carry chests. Test ids refer to {@code docs/M03_TEST_PLAN.md}.
 */
public class StorageBypassGameTests {
	private static final Set<Item> REMOVED = Set.of(Items.ENDER_CHEST, Items.CHEST_MINECART);

	/** T19, T21: no recipe makes an ender chest or a chest minecart; the plain minecart still crafts. */
	@GameTest
	public void noRecipeMakesEnderChestsOrChestMinecarts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		RecipeManager recipes = server.getRecipeManager();
		for (String id : List.of("ender_chest", "chest_minecart")) {
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(id))).isEmpty(), "recipe " + id + " still exists");
		}
		for (String advancement : List.of("recipes/decorations/ender_chest", "recipes/transportation/chest_minecart")) {
			helper.assertTrue(server.getAdvancements().get(Identifier.withDefaultNamespace(advancement)) == null, "advancement " + advancement + " still exists");
		}

		ItemStack obsidian = new ItemStack(Items.OBSIDIAN);
		CraftingInput enderChest = CraftingInput.of(3, 3, List.of(obsidian, obsidian.copy(), obsidian.copy(), obsidian.copy(),
			new ItemStack(Items.ENDER_EYE), obsidian.copy(), obsidian.copy(), obsidian.copy(), obsidian.copy()));
		helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, enderChest, level).isEmpty(), "obsidian around an eye of ender still crafts something");
		CraftingInput chestCart = CraftingInput.of(2, 1, List.of(new ItemStack(Items.CHEST), new ItemStack(Items.MINECART)));
		helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, chestCart, level).isEmpty(), "chest + minecart still crafts something");

		ItemStack iron = new ItemStack(Items.IRON_INGOT);
		CraftingInput minecart = CraftingInput.of(3, 2, List.of(iron, ItemStack.EMPTY, iron.copy(), iron.copy(), iron.copy(), iron.copy()));
		helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, minecart, level)
			.map(holder -> holder.value().assemble(minecart).is(Items.MINECART)).orElse(false), "the plain minecart should still craft");

		ContextMap context = SlotDisplayContext.fromLevel(level);
		int scanned = 0;
		for (RecipeHolder<?> holder : recipes.getRecipes()) {
			Recipe<?> recipe = holder.value();
			for (RecipeDisplay display : recipe.display()) {
				for (ItemStack result : display.result().resolveForStacks(context)) {
					helper.assertFalse(REMOVED.contains(result.getItem()), "recipe " + holder.id().identifier() + " produces " + result);
				}
			}
			scanned++;
		}
		SimpleBlockTestLog.info("FRT-10/11 recipes: ender chest and chest minecart recipes and advancements absent, both shapes craft nothing, "
			+ "{} recipes scanned, none yield either; the minecart still crafts", scanned);
		helper.succeed();
	}

	/** T19: no loot table gives either item; a silk-touched ender chest (End city) gives obsidian. */
	@GameTest
	public void noLootGivesEnderChestsOrChestMinecarts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
		int tables = 0;
		List<String> found = new ArrayList<>();
		for (Holder.Reference<LootTable> table : server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElements().toList()) {
			String json = LootTable.DIRECT_CODEC.encodeStart(ops, table.value()).getOrThrow().toString();
			if (json.contains("\"minecraft:ender_chest\"") || json.contains("\"minecraft:chest_minecart\"")) {
				found.add(table.key().identifier().toString());
			}
			tables++;
		}
		helper.assertTrue(found.isEmpty(), "loot tables still giving an ender chest or chest minecart: " + found);

		Holder<Enchantment> silkTouch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.enchant(silkTouch, 1);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 2, 0));
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, Blocks.ENDER_CHEST);
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), level, helper.absolutePos(pos), level.getBlockEntity(helper.absolutePos(pos)),
			player, pickaxe);
		helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.OBSIDIAN) && drops.getFirst().getCount() == 8,
			"a silk-touched ender chest should drop 8 obsidian, dropped " + drops);
		SimpleBlockTestLog.info("FRT-10/11 loot: {} loot tables scanned, none give an ender chest or chest minecart; silk-touched ender chest drops {}",
			tables, drops);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** T19, T21: neither item is offered in any creative tab; the minecart still is. */
	@GameTest
	public void creativeTabsLeaveThemOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
		boolean minecart = false;
		for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
			for (ItemStack stack : tab.getDisplayItems()) {
				helper.assertFalse(REMOVED.contains(stack.getItem()), stack + " is still in a creative tab");
				minecart |= stack.is(Items.MINECART);
			}
			for (ItemStack stack : tab.getSearchTabDisplayItems()) {
				helper.assertFalse(REMOVED.contains(stack.getItem()), stack + " is still in creative search");
			}
		}
		helper.assertTrue(minecart, "the minecart should still be in a creative tab");
		helper.succeed();
	}

	/** T20: ender chests can't be placed, and an existing one (End city, commands) never opens. */
	@GameTest
	public void enderChestsCannotBePlacedOrOpened(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (GameType gameType : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
			ServerPlayer player = TestPlayers.create(helper, gameType, new BlockPos(0, 2, 0));
			helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
			helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_CHEST));
			BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false));
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
			helper.assertValueEqual(player.getMainHandItem().getCount(), 1, gameType.getName() + " ender chest count after use");
			TestPlayers.remove(player);
		}

		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 2, 0));
		player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
		BlockPos chest = new BlockPos(4, 2, 4);
		helper.setBlock(chest, Blocks.ENDER_CHEST);
		BlockPos abs = helper.absolutePos(chest);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
		helper.assertTrue(player.containerMenu == player.inventoryMenu, "the ender chest opened a menu: " + player.containerMenu);
		SimpleBlockTestLog.info("FRT-10: survival and creative placement refused; an existing ender chest stayed shut (menu: {})", player.containerMenu);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** T21, T23: chest minecarts can't be placed by hand or dispenser; plain minecarts still can. */
	@GameTest(maxTicks = 40)
	public void chestMinecartsCannotBePlacedButMinecartsCan(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 2, 0));
		BlockPos rail = new BlockPos(2, 2, 1);
		helper.setBlock(rail.below(), Blocks.STONE);
		helper.setBlock(rail, Blocks.RAIL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST_MINECART));
		BlockPos abs = helper.absolutePos(rail);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "chest minecart items after use on a rail");

		// Two dispensers facing rails: one with a chest minecart, one with a plain minecart.
		BlockPos chestDispenser = new BlockPos(1, 2, 4);
		BlockPos cartDispenser = new BlockPos(1, 2, 6);
		for (BlockPos dispenserPos : List.of(chestDispenser, cartDispenser)) {
			helper.setBlock(dispenserPos.east().below(), Blocks.STONE);
			helper.setBlock(dispenserPos.east(), Blocks.RAIL);
			helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
		}
		DispenserBlockEntity chestOne = (DispenserBlockEntity) level.getBlockEntity(helper.absolutePos(chestDispenser));
		DispenserBlockEntity cartOne = (DispenserBlockEntity) level.getBlockEntity(helper.absolutePos(cartDispenser));
		chestOne.setItem(0, new ItemStack(Items.CHEST_MINECART));
		cartOne.setItem(0, new ItemStack(Items.MINECART));
		helper.setBlock(chestDispenser.west(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(cartDispenser.west(), Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(10, () -> {
			AABB area = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(8, 8, 8);
			helper.assertValueEqual(level.getEntitiesOfClass(MinecartChest.class, area).size(), 0, "chest minecarts in the test area");
			helper.assertTrue(chestOne.getItem(0).is(Items.CHEST_MINECART), "the chest minecart should still be in its dispenser");
			helper.assertValueEqual(level.getEntitiesOfClass(Minecart.class, area).size(), 1, "plain minecarts dispensed");
			helper.assertTrue(cartOne.getItem(0).isEmpty(), "the plain minecart should have left its dispenser");
			SimpleBlockTestLog.info("FRT-11: chest minecart refused by hand and by dispenser (kept in both); plain minecart dispensed onto its rail");
			level.getEntitiesOfClass(AbstractMinecart.class, area).forEach(Entity::discard);
			TestPlayers.remove(player);
			helper.succeed();
		});
	}

	/**
	 * T22: a chest minecart that exists anyway (mineshafts generate them) is sealed loot: it doesn't
	 * open, hoppers and droppers can neither fill nor empty it, and breaking it spills its contents
	 * with a minecart and a chest, not a chest-minecart item.
	 */
	@GameTest(maxTicks = 80)
	public void existingChestMinecartsAreSealedLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Hopper above (pointing down into the cart) and hopper below (pulling from it).
		BlockPos rail = new BlockPos(2, 2, 2);
		helper.setBlock(rail.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(rail, Blocks.RAIL);
		helper.setBlock(rail.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		HopperBlockEntity above = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(rail.above()));
		HopperBlockEntity below = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(rail.below()));
		above.setItem(0, new ItemStack(Items.DIRT, 10));
		MinecartChest cart = helper.spawn(EntityTypes.CHEST_MINECART, Vec3.atBottomCenterOf(rail).add(0, 0.0625, 0));
		cart.setItem(0, new ItemStack(Items.RAW_IRON, 64));

		// A mineshaft-style cart with its loot table, elsewhere.
		BlockPos lootRail = new BlockPos(5, 2, 5);
		helper.setBlock(lootRail.below(), Blocks.STONE);
		helper.setBlock(lootRail, Blocks.RAIL);
		MinecartChest loot = EntityTypes.CHEST_MINECART.create(level, EntitySpawnReason.CHUNK_GENERATION);
		Vec3 lootAt = helper.absoluteVec(Vec3.atBottomCenterOf(lootRail).add(0, 0.0625, 0));
		loot.setInitialPos(lootAt.x, lootAt.y, lootAt.z);
		loot.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT, 42L);
		level.addFreshEntity(loot);

		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 2, 0));
		player.interactOn(cart, InteractionHand.MAIN_HAND, cart.position());
		helper.assertTrue(player.containerMenu == player.inventoryMenu, "the chest minecart opened: " + player.containerMenu);

		helper.runAfterDelay(60, () -> {
			helper.assertValueEqual(above.getItem(0).getCount(), 10, "dirt left in the hopper above (none should go in)");
			helper.assertTrue(below.isEmpty(), "the hopper below took something out of the cart");
			helper.assertTrue(cart.getItem(0).is(Items.RAW_IRON) && cart.getItem(0).getCount() == 64, "the cart's contents changed: " + cart.getItem(0));

			Vec3 at = cart.position();
			cart.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(2.0));
			List<String> dropped = drops.stream().map(item -> item.getItem().toString()).toList();
			helper.assertTrue(drops.stream().noneMatch(item -> item.getItem().is(Items.CHEST_MINECART)), "a chest minecart item dropped: " + dropped);
			helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.MINECART)), "no minecart dropped: " + dropped);
			helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.CHEST)), "no chest dropped: " + dropped);
			helper.assertValueEqual(drops.stream().filter(item -> item.getItem().is(Items.RAW_IRON)).mapToInt(item -> item.getItem().getCount()).sum(), 64,
				"raw iron spilled");
			drops.forEach(Entity::discard);

			loot.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
			List<ItemEntity> lootDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(lootAt, lootAt).inflate(2.0));
			List<String> lootDropped = lootDrops.stream().map(item -> item.getItem().toString()).toList();
			helper.assertTrue(lootDrops.stream().noneMatch(item -> item.getItem().is(Items.CHEST_MINECART)), "a chest minecart item dropped: " + lootDropped);
			helper.assertTrue(lootDrops.size() > 2, "the mineshaft loot should spill: " + lootDropped);
			SimpleBlockTestLog.info("FRT-11: chest minecart stayed shut; hoppers above/below moved nothing in 60 ticks; broken: {}; mineshaft loot cart broken: {}",
				dropped, lootDropped);
			lootDrops.forEach(Entity::discard);
			TestPlayers.remove(player);
			helper.succeed();
		});
	}

	/** T24: donkeys, mules and llamas still take a chest, open it, hold cargo and carry a rider. */
	@GameTest
	public void packAnimalsStillCarryChests(GameTestHelper helper) {
		List<String> log = new ArrayList<>();
		List<EntityType<? extends AbstractChestedHorse>> types = List.of(EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.LLAMA);
		for (int i = 0; i < types.size(); i++) {
			// Spawned as a spawn egg would: that rolls a llama's strength, which sets its chest size.
			AbstractChestedHorse animal = types.get(i).spawn(helper.getLevel(), helper.absolutePos(new BlockPos(1 + i * 3, 2, 3)), EntitySpawnReason.SPAWN_ITEM_USE);
			animal.setNoAi(true);
			animal.setTamed(true);
			ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1 + i * 3, 2, 1));
			player.getInventory().clearContent();
			// Equip the chest the way a player does: use a chest on the tamed animal.
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
			player.interactOn(animal, InteractionHand.MAIN_HAND, animal.position());
			helper.assertTrue(animal.hasChest() && player.getMainHandItem().isEmpty(), types.get(i) + " did not take the chest");
			player.getInventory().setItem(20, new ItemStack(Items.RAW_IRON, 32));
			animal.openCustomInventoryScreen(player);
			helper.assertTrue(player.containerMenu instanceof AbstractMountInventoryMenu, types.get(i) + " inventory did not open: " + player.containerMenu);
			AbstractMountInventoryMenu menu = (AbstractMountInventoryMenu) player.containerMenu;
			int from = -1;
			for (Slot slot : menu.slots) {
				if (slot.container == player.getInventory() && slot.getContainerSlot() == 20) {
					from = slot.index;
				}
			}
			menu.quickMoveStack(player, from);
			int inAnimal = 0;
			for (Slot slot : menu.slots) {
				if (slot.container != player.getInventory()) {
					inAnimal += slot.getItem().is(Items.RAW_IRON) ? slot.getItem().getCount() : 0;
				}
			}
			helper.assertValueEqual(inAnimal, 32, types.get(i) + " raw iron moved into its chest");
			player.closeContainer();
			helper.assertTrue(player.startRiding(animal), "a player should be able to ride the " + types.get(i));
			log.add(types.get(i).toShortString() + ": chest equipped by hand, opened, 32 raw iron shift-clicked in, ridden");
			player.stopRiding();
			TestPlayers.remove(player);
			animal.discard();
		}
		SimpleBlockTestLog.info("FRT-12: {}", String.join("; ", log));
		helper.succeed();
	}
}
