package io.github.boxofdog.simpleblock.mixin.workwear;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Old Pro: a block broken with the offhand pickaxe ({@link Workwear#miningTool}, chosen for the block
 * as it was) wears that pickaxe and drops as mined with it (its enchantments); the main hand's item is
 * not used. Everything else about breaking is vanilla's.
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerPlayer player;
	@Shadow
	protected ServerLevel level;

	@Unique
	private ItemStack simpleBlock$tool = ItemStack.EMPTY;

	@Inject(method = "destroyBlock", at = @At("HEAD"))
	private void simpleBlock$chooseTool(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		this.simpleBlock$tool = Workwear.miningTool(this.player, this.level.getBlockState(pos));
	}

	@WrapOperation(method = "destroyBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack simpleBlock$mineWithTool(ServerPlayer player, Operation<ItemStack> original) {
		ItemStack main = original.call(player);
		return this.simpleBlock$tool.isEmpty() ? main : this.simpleBlock$tool;
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void simpleBlock$forgetTool(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		this.simpleBlock$tool = ItemStack.EMPTY;
	}
}
