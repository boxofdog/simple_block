package io.github.boxofdog.simpleblock.mixin.workwear;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Old Pro: how fast a block breaks and whether it drops are judged with the item it is mined with
 * ({@link Workwear#miningTool}): an offhand pickaxe when the main hand's item is not right for the block.
 * Client and server judge the same way.
 */
@Mixin(Player.class)
abstract class PlayerMiningMixin {
	@WrapOperation(method = {"getDestroySpeed", "hasCorrectToolForDrops"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack simpleBlock$oldProTool(Inventory inventory, Operation<ItemStack> original, @Local(argsOnly = true) BlockState state) {
		original.call(inventory);
		return Workwear.miningTool((Player) (Object) this, state);
	}
}
