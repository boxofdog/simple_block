package io.github.boxofdog.simpleblock.mixin.chestcart;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.ChestMinecarts;

/**
 * FRT-11: a chest minecart item never becomes a cart. {@code createMinecart} is where both the
 * item (on both sides, so client and server agree) and the dispenser behaviour make one; with no
 * cart, the item fails and the dispenser keeps its item. Mineshafts create theirs directly.
 */
@Mixin(AbstractMinecart.class)
abstract class AbstractMinecartMixin {
	@Inject(method = "createMinecart", at = @At("HEAD"), cancellable = true)
	private static void simple_block$noChestMinecarts(
			Level level, double x, double y, double z, EntityType<?> type, EntitySpawnReason reason, ItemStack itemStack, @Nullable Player player,
			CallbackInfoReturnable<AbstractMinecart> cir
	) {
		if (type == EntityTypes.CHEST_MINECART) {
			ChestMinecarts.notifyDisabled(player);
			cir.setReturnValue(null);
		}
	}
}
