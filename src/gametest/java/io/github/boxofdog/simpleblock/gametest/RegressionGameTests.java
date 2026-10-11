package io.github.boxofdog.simpleblock.gametest;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** REG-02: neighbouring vanilla content keeps working; disabled content stays registered. */
public class RegressionGameTests {
	@GameTest
	public void unrelatedRecipesStillCraft(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack plank = new ItemStack(Items.OAK_PLANKS);
		ItemStack iron = new ItemStack(Items.IRON_INGOT);
		ItemStack wool = new ItemStack(Items.WOOL.pick(DyeColor.WHITE));

		assertCrafts(helper, level, CraftingInput.of(3, 3, List.of(plank, plank, plank, plank, ItemStack.EMPTY, plank, plank, plank, plank)), Items.CHEST);
		assertCrafts(helper, level, CraftingInput.of(2, 2, Collections.nCopies(4, plank)), Items.CRAFTING_TABLE);
		assertCrafts(helper, level, CraftingInput.of(3, 2, List.of(wool, wool, wool, plank, plank, plank)), Items.BED.pick(DyeColor.WHITE));
		assertCrafts(helper, level, CraftingInput.of(3, 2, List.of(iron, ItemStack.EMPTY, iron, iron, iron, iron)), Items.MINECART);
		assertCrafts(helper, level, CraftingInput.of(3, 3, List.of(iron, ItemStack.EMPTY, iron, iron, new ItemStack(Items.CHEST), iron, ItemStack.EMPTY, iron, ItemStack.EMPTY)), Items.HOPPER);
		helper.succeed();
	}

	@GameTest
	public void disabledContentStaysRegistered(GameTestHelper helper) {
		for (Item item : List.of(Items.ELYTRA, Items.SHULKER_BOX, Items.SHULKER_SHELL, Items.PHANTOM_MEMBRANE, Items.PHANTOM_SPAWN_EGG)) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(item)), item + " should stay registered");
		}

		ShulkerBoxGameTests.allBoxItems().forEach(item -> helper.assertTrue(BuiltInRegistries.ITEM.getKey(item) != null, "box registered"));
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(EntityTypes.PHANTOM)), "phantom entity type registered");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(EntityTypes.SHULKER)), "shulker entity type registered");
		helper.succeed();
	}

	private static void assertCrafts(GameTestHelper helper, ServerLevel level, CraftingInput input, Item expected) {
		var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		helper.assertTrue(recipe.isPresent(), "no recipe for " + expected);
		ItemStack result = recipe.get().value().assemble(input);
		helper.assertTrue(result.is(expected), "expected " + expected + " but crafted " + result);
	}
}
