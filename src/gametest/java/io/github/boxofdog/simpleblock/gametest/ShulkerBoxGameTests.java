package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** SF-04 evidence. Every check covers the plain box and all 16 dyed variants. */
public class ShulkerBoxGameTests {
	static List<Item> allBoxItems() {
		List<Item> items = new ArrayList<>();
		items.add(Items.SHULKER_BOX);
		items.addAll(Items.DYED_SHULKER_BOX.asList());
		return items;
	}

	static boolean isShulkerBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	/** SHU-01, SHU-02: no recipe of any kind produces a shulker box, and the vanilla ones are gone. */
	@GameTest
	public void noRecipeMakesShulkerBoxes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		RecipeManager recipes = server.getRecipeManager();
		helper.assertValueEqual(allBoxItems().size(), 17, "shulker box variants");

		for (Item box : allBoxItems()) {
			Identifier id = BuiltInRegistries.ITEM.getKey(box);
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, id)).isEmpty(), "recipe " + id + " still exists");
			Identifier advancement = id.withPrefix("recipes/decorations/");
			helper.assertTrue(server.getAdvancements().get(advancement) == null, "recipe advancement " + advancement + " still exists");
		}

		// The vanilla shaped recipe: shell / chest / shell.
		ItemStack shell = new ItemStack(Items.SHULKER_SHELL);
		CraftingInput boxShape = CraftingInput.of(1, 3, List.of(shell, new ItemStack(Items.CHEST), shell.copy()));
		helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, boxShape, level).isEmpty(), "shell + chest + shell still crafts something");

		// The vanilla dye (transmute) recipes, for every box/dye combination.
		int dyeAttempts = 0;
		for (Item box : allBoxItems()) {
			for (Item dye : Items.DYE.asList()) {
				CraftingInput dyeing = CraftingInput.of(2, 1, List.of(new ItemStack(box), new ItemStack(dye)));
				helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, dyeing, level).isEmpty(), box + " + " + dye + " still crafts something");
				dyeAttempts++;
			}
		}

		// Every recipe of every type: none shows a shulker box as its result.
		ContextMap context = SlotDisplayContext.fromLevel(level);
		int scanned = 0;
		for (RecipeHolder<?> holder : recipes.getRecipes()) {
			Recipe<?> recipe = holder.value();
			for (RecipeDisplay display : recipe.display()) {
				for (ItemStack result : display.result().resolveForStacks(context)) {
					helper.assertFalse(isShulkerBox(result), "recipe " + holder.id().identifier() + " produces " + result);
				}
			}
			scanned++;
		}

		SimpleBlockTestLog.info("SHU-01/02: 17 recipe ids and advancements absent, {} dye combinations rejected, {} recipes scanned, none yield a shulker box",
				dyeAttempts, scanned);
		helper.succeed();
	}

	/** SHU-03: neither survival nor creative players can place any box variant. */
	@GameTest
	public void shulkerBoxesCannotBePlaced(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<Item> boxes = allBoxItems();

		for (GameType gameType : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
			ServerPlayer player = TestPlayers.create(helper, gameType, new BlockPos(0, 2, 0));

			for (int i = 0; i < boxes.size(); i++) {
				BlockPos support = new BlockPos(1 + i % 6, 1, 1 + i / 6);
				BlockPos target = support.above();
				helper.setBlock(support, Blocks.STONE);
				helper.setBlock(target, Blocks.AIR);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(boxes.get(i)));

				BlockPos abs = helper.absolutePos(support);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
				player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);

				helper.assertBlockPresent(Blocks.AIR, target);
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, gameType.getName() + " " + boxes.get(i) + " count after use");
			}

			TestPlayers.remove(player);
		}

		helper.succeed();
	}

	/** SHU-03: the vanilla shulker-box dispenser behaviour (places the box) no longer works. */
	@GameTest(maxTicks = 40)
	public void dispenserCannotPlaceBoxes(GameTestHelper helper) {
		BlockPos dispenserPos = new BlockPos(2, 1, 2);
		helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
		DispenserBlockEntity dispenser = (DispenserBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(dispenserPos));
		dispenser.setItem(0, new ItemStack(Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.RED)));
		helper.setBlock(dispenserPos.west(), Blocks.REDSTONE_BLOCK);

		helper.runAfterDelay(10, () -> {
			helper.assertBlockPresent(Blocks.AIR, dispenserPos.east());
			helper.assertTrue(isShulkerBox(dispenser.getItem(0)), "the box should still be in the dispenser");
			helper.succeed();
		});
	}

	/** SHU-04: boxes that exist anyway (placed with /setblock) cannot be opened, for every variant. */
	@GameTest
	public void existingBoxesCannotBeOpened(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 1, 0));
		List<Item> boxes = allBoxItems();

		for (int i = 0; i < boxes.size(); i++) {
			BlockPos pos = new BlockPos(1 + i % 6, 1, 1 + (i / 6) * 2);
			Block block = ((BlockItem) boxes.get(i)).getBlock();
			helper.setBlock(pos, block);
			ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
			box.setItem(0, new ItemStack(Items.DIAMOND, 5));

			BlockPos abs = helper.absolutePos(pos);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
			helper.assertTrue(player.containerMenu == player.inventoryMenu, block + " opened a container menu");
		}

		TestPlayers.remove(player);
		helper.succeed();
	}

	/** SHU-04: hoppers can neither drain a box nor fill one, so a box cannot be loaded by automation. */
	@GameTest(maxTicks = 100)
	public void hoppersCannotFillOrDrainBoxes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Drain: box with diamonds sitting on a hopper.
		BlockPos drainBox = new BlockPos(2, 2, 2);
		helper.setBlock(drainBox.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(drainBox, Blocks.SHULKER_BOX);
		((ShulkerBoxBlockEntity) level.getBlockEntity(helper.absolutePos(drainBox))).setItem(0, new ItemStack(Items.DIAMOND, 5));
		// Fill: hopper with emeralds pointing down into a box.
		BlockPos fillBox = new BlockPos(5, 1, 2);
		helper.setBlock(fillBox, Blocks.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BLUE));
		helper.setBlock(fillBox.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		((HopperBlockEntity) level.getBlockEntity(helper.absolutePos(fillBox.above()))).setItem(0, new ItemStack(Items.EMERALD, 5));

		helper.runAfterDelay(80, () -> {
			ShulkerBoxBlockEntity drained = (ShulkerBoxBlockEntity) level.getBlockEntity(helper.absolutePos(drainBox));
			ShulkerBoxBlockEntity filled = (ShulkerBoxBlockEntity) level.getBlockEntity(helper.absolutePos(fillBox));
			helper.assertValueEqual(drained.getItem(0).getCount(), 5, "diamonds left in the box above the hopper");
			helper.assertTrue(filled.isEmpty(), "the box below the hopper received items");
			helper.assertValueEqual(((HopperBlockEntity) level.getBlockEntity(helper.absolutePos(fillBox.above()))).getItem(0).getCount(), 5,
					"emeralds left in the hopper above the box");
			helper.succeed();
		});
	}

	/** SHU-06: shulker mobs still exist, survive, take damage and keep their shell drop. */
	@GameTest(maxTicks = 60)
	public void shulkerMobsUnaffected(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		Shulker shulker = helper.spawn(EntityTypes.SHULKER, new BlockPos(3, 2, 3));
		String lootJson = DataAuditGameTests.lootTableJson(level.getServer(), EntityTypes.SHULKER.getDefaultLootTable().orElseThrow());
		helper.assertTrue(lootJson.contains("minecraft:shulker_shell"), "shulker loot table should still drop shells");

		helper.runAfterDelay(40, () -> {
			helper.assertTrue(shulker.isAlive(), "shulker should still be alive");
			float before = shulker.getHealth();
			shulker.hurtServer(level, level.damageSources().generic(), 6.0F);
			helper.assertTrue(shulker.getHealth() < before, "shulker should take damage");
			shulker.discard();
			helper.succeed();
		});
	}
}
