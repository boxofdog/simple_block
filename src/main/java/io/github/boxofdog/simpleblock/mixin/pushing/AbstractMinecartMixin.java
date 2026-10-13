package io.github.boxofdog.simpleblock.mixin.pushing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.feature.CartPushing;

/**
 * {@link CartPushing}: a player walking into a minecart calls this (from {@code LivingEntity.pushEntities});
 * for a cart a player may not shove, nothing is pushed, neither the cart nor the player.
 */
@Mixin(AbstractMinecart.class)
abstract class AbstractMinecartMixin {
	@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$noShoving(Entity entity, CallbackInfo ci) {
		if (!CartPushing.moves((AbstractMinecart) (Object) this, entity)) {
			ci.cancel();
		}
	}
}
