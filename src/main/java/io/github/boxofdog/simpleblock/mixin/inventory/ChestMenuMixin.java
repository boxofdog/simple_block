package io.github.boxofdog.simpleblock.mixin.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.feature.ChestSort;

/** M13: a chest's menu, made on the server, tells the player's client its screen may show Sort ({@link ChestSort}). */
@Mixin(ChestMenu.class)
abstract class ChestMenuMixin {
	@Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
		at = @At("TAIL"))
	private void simpleBlock$announceSortable(MenuType<?> type, int containerId, Inventory inventory, Container container, int rows, CallbackInfo ci) {
		if (inventory.player instanceof ServerPlayer player && ChestSort.isChest(container)) {
			ChestSort.announce(player, containerId);
		}
	}
}
