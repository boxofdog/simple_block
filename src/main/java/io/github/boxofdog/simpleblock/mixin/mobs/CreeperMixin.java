package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;

import io.github.boxofdog.simpleblock.feature.CreeperEvaporation;

/** MOB-03: creepers dissolve in direct sunlight (see {@link CreeperEvaporation}). */
@Mixin(Creeper.class)
abstract class CreeperMixin implements CreeperEvaporation.Evaporating {
	@Unique
	private int simpleBlock$evaporation = -1;

	@Override
	public int simpleBlock$evaporationTicks() {
		return this.simpleBlock$evaporation;
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$evaporateInSunlight(CallbackInfo ci) {
		Creeper self = (Creeper) (Object) this;
		if (!(self.level() instanceof ServerLevel level) || !self.isAlive()) {
			return;
		}
		if (this.simpleBlock$evaporation < 0) {
			if (!CreeperEvaporation.inDirectSunlight(self, level)) {
				return;
			}
			this.simpleBlock$evaporation = 0;
			CreeperEvaporation.start(self, level);
		}
		// While it smokes the creeper is frozen: no fuse, no movement, no attack.
		ci.cancel();
		this.simpleBlock$evaporation++;
		if (this.simpleBlock$evaporation >= CreeperEvaporation.duration()) {
			CreeperEvaporation.finish(self, level);
		} else {
			CreeperEvaporation.smoke(self, level);
		}
	}
}
