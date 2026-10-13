package io.github.boxofdog.simpleblock.mixin.metallurgy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M08: an ordinary furnace smelts raw metal and metal ores ({@code #simple_block:raw_ore_smelting})
 * {@code furnace.raw_ore_cook_multiplier} times slower, so the blast furnace is the way to smelt in bulk.
 * Smokers, other recipes and other inputs are unchanged.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockEntityMixin {
	private static final TagKey<Item> SIMPLE_BLOCK$RAW_ORE = TagKey.create(Registries.ITEM,
		Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "raw_ore_smelting"));

	@ModifyReturnValue(method = "getTotalCookTime(Lnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I",
		at = @At("RETURN"))
	private static int simpleBlock$slowRawOre(int cookTime, RecipeHolder<? extends AbstractCookingRecipe> recipe, AbstractFurnaceBlockEntity entity) {
		if (entity instanceof FurnaceBlockEntity && entity.getItem(0).is(SIMPLE_BLOCK$RAW_ORE)) {
			return (int) Math.ceil(cookTime * Tuning.getDouble("furnace.raw_ore_cook_multiplier"));
		}
		return cookTime;
	}
}
