package io.github.boxofdog.simpleblock.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import io.github.boxofdog.simpleblock.gametest.client.PlayedSounds;

/** Client tests: counts the lava particles (cinders) the client level is asked to show. */
@Mixin(ClientLevel.class)
abstract class ClientParticleCountMixin {
	@Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"))
	private void simpleBlockTest$countLava(ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd, CallbackInfo ci) {
		if (particle.getType() == ParticleTypes.LAVA) {
			PlayedSounds.LAVA_PARTICLES.incrementAndGet();
		}
	}
}
