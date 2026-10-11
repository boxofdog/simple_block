package io.github.boxofdog.simpleblock.mixin.chestcart;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.ChestMinecarts;

/** FRT-11: a chest minecart (a mineshaft's, or one summoned) never opens its inventory. */
@Mixin(MinecartChest.class)
abstract class MinecartChestMixin {
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void simple_block$neverOpen(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		ChestMinecarts.notifyDisabled(player);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
