package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.feature.Entrancement;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/** M09 breach: after any explosion, light-drawn mobs around re-check what they can reach ({@link Entrancement#reconsider}). */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;
	@Shadow
	@Final
	private Vec3 center;

	@Inject(method = "explode", at = @At("TAIL"))
	private void simpleBlock$breach(CallbackInfoReturnable<Integer> cir) {
		Entrancement.reconsider(this.level, this.center, Tuning.getInt("mob.breach.radius"));
	}
}
