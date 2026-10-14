package io.github.boxofdog.simpleblock.mixin.effigy;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.effigy.CompressionRecipe;

/**
 * M12: taking a compressed monster block from a crafting grid's result slot takes the whole stack
 * from every cell, not one item ({@link CompressionRecipe}). Every way of taking the result (click,
 * shift-click and its repeats, drag, the recipe book's craft) goes through {@code onTake}.
 */
@Mixin(ResultSlot.class)
abstract class ResultSlotMixin extends Slot {
	private ResultSlotMixin() {
		super(null, 0, 0, 0);
	}

	@WrapOperation(method = "onTake", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack simpleBlock$takeWholeStacks(CraftingContainer grid, int slot, int count, Operation<ItemStack> original) {
		if (this.container instanceof ResultContainer result && CompressionRecipe.takesWholeStacks(result.getRecipeUsed())) {
			count = Math.max(count, grid.getItem(slot).getCount());
		}
		return original.call(grid, slot, count);
	}
}
