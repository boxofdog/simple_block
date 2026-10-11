package io.github.boxofdog.simpleblock.mixin.inventory;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;

/** INV-04: the 2x2 crafting grid is already carried; returning it to the inventory on close is free. */
@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin {
	@WrapMethod(method = "removed")
	private void simpleBlock$returnGridFreely(Player player, Operation<Void> original) {
		HeavyLoad.exempt(() -> original.call(player));
	}
}
