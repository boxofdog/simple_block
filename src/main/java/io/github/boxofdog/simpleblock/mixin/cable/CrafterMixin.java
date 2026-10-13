package io.github.boxofdog.simpleblock.mixin.cable;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.cable.CenterColumnRecipes;

/** M06: the cable recipes only in the middle column ({@link CenterColumnRecipes}), for the crafter block. */
@Mixin(CrafterBlock.class)
abstract class CrafterMixin {
	@ModifyExpressionValue(method = "dispenseFrom", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;"))
	private Optional<RecipeHolder<CraftingRecipe>> simpleBlock$middleColumnOnly(Optional<RecipeHolder<CraftingRecipe>> found,
		@Local CrafterBlockEntity crafter) {
		return CenterColumnRecipes.filter(found, crafter);
	}
}
