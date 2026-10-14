package io.github.boxofdog.simpleblock.mixin.effigy;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.effigy.CompressionRecipe;

/**
 * M12: a crafter making a compressed monster block uses the whole stack in every cell
 * ({@link CompressionRecipe}). Vanilla then takes one from each cell; each is left at one first.
 */
@Mixin(CrafterBlock.class)
abstract class CrafterBlockMixin {
	@ModifyExpressionValue(method = "dispenseFrom", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/entity/CrafterBlockEntity;getItems()Lnet/minecraft/core/NonNullList;"))
	private NonNullList<ItemStack> simpleBlock$takeWholeStacks(NonNullList<ItemStack> items, @Local RecipeHolder<CraftingRecipe> recipe) {
		if (CompressionRecipe.takesWholeStacks(recipe)) {
			for (ItemStack stack : items) {
				if (stack.getCount() > 1) {
					stack.setCount(1);
				}
			}
		}
		return items;
	}
}
