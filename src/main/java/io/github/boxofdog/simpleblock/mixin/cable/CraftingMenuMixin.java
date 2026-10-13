package io.github.boxofdog.simpleblock.mixin.cable;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.cable.CenterColumnRecipes;

/** M06: the cable recipes only in the middle column ({@link CenterColumnRecipes}), for the crafting table and the inventory grid. */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
	@ModifyExpressionValue(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;"))
	private static Optional<RecipeHolder<CraftingRecipe>> simpleBlock$middleColumnOnly(Optional<RecipeHolder<CraftingRecipe>> found,
		@Local(argsOnly = true) CraftingContainer container) {
		return CenterColumnRecipes.filter(found, container);
	}
}
