package io.github.boxofdog.simpleblock.mixin.pushing;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.feature.CartPushing;

/**
 * {@link CartPushing}: a moving minecart running into a player calls the player's {@code push(Entity)},
 * which pushes both; the cart's half is skipped for a cart a player may not shove.
 */
@Mixin(Entity.class)
abstract class EntityMixin {
	@WrapWithCondition(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
	private boolean simpleBlock$noShoving(Entity pushed, double x, double y, double z, Entity other) {
		Entity pusher = pushed == (Object) this ? other : (Entity) (Object) this;
		return !(pushed instanceof AbstractMinecart cart) || CartPushing.moves(cart, pusher);
	}
}
