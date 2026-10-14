package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;

import io.github.boxofdog.simpleblock.feature.Entrancement;
import io.github.boxofdog.simpleblock.feature.LightRoutes;

/** M09: a light-drawn mob's path may lead through closed wooden doors only while it takes such a route ({@link LightRoutes}). */
@Mixin(Mob.class)
abstract class MobMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"))
	private void simpleBlock$lightDoorRoute(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		// The zombie family sets this flag itself (pursuit or light route): ZombieMixin.
		if (!(self instanceof Zombie) && Entrancement.participates(self)) {
			LightRoutes.tick(self);
		}
	}
}
