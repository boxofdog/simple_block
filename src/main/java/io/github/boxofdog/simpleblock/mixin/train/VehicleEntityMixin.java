package io.github.boxofdog.simpleblock.mixin.train;

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
 * M05: a destroyed Cobblestone Engine (vanilla's furnace minecart) drops what it was made from, a
 * minecart and a furnace, never a furnace-minecart item. Only the furnace minecart drops that item.
 */
@Mixin(VehicleEntity.class)
abstract class VehicleEntityMixin {
	@Inject(method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$dropMinecartAndFurnace(ServerLevel level, Item dropItem, CallbackInfo ci) {
		if (dropItem != Items.FURNACE_MINECART) {
			return;
		}
		VehicleEntity self = (VehicleEntity) (Object) this;
		self.kill(level);
		if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
			self.spawnAtLocation(level, new ItemStack(Items.MINECART));
			self.spawnAtLocation(level, new ItemStack(Items.FURNACE));
		}
		ci.cancel();
	}
}
