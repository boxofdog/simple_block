package io.github.boxofdog.simpleblock.mixin.cable;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.cable.CenterColumnRecipes;

/** M06: the crafter's preview agrees with what it crafts ({@link CenterColumnRecipes}). */
@Mixin(CrafterMenu.class)
abstract class CrafterMenuMixin {
	@Shadow
	@Final
	private CraftingContainer container;

	@ModifyExpressionValue(method = "refreshRecipeResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;"))
	private Optional<RecipeHolder<CraftingRecipe>> simpleBlock$middleColumnOnly(Optional<RecipeHolder<CraftingRecipe>> found) {
		return CenterColumnRecipes.filter(found, this.container);
	}
}
