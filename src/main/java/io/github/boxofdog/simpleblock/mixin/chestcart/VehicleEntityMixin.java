package io.github.boxofdog.simpleblock.mixin.chestcart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FRT-11: a destroyed chest minecart drops a minecart and a chest instead of a chest-minecart item
 * (its contents still spill, from {@code AbstractMinecartContainer}). Only chest minecarts drop
 * that item.
 */
@Mixin(VehicleEntity.class)
abstract class VehicleEntityMixin {
	@Inject(method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V", at = @At("HEAD"), cancellable = true)
	private void simple_block$dropMinecartAndChest(ServerLevel level, Item dropItem, CallbackInfo ci) {
		if (dropItem != Items.CHEST_MINECART) {
			return;
		}
		VehicleEntity self = (VehicleEntity) (Object) this;
		self.kill(level);
		if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
			self.spawnAtLocation(level, new ItemStack(Items.MINECART));
			self.spawnAtLocation(level, new ItemStack(Items.CHEST));
		}
		ci.cancel();
	}
}
