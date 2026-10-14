package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.spider.Spider;

import io.github.boxofdog.simpleblock.feature.SpiderJockeys;

/** M09: the jockey's ledge grab, after the spider's own movement each server tick ({@link SpiderJockeys#tick}). */
@Mixin(Spider.class)
abstract class SpiderMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void simpleBlock$ledgeGrab(CallbackInfo ci) {
		Spider self = (Spider) (Object) this;
		if (self.level() instanceof ServerLevel) {
			SpiderJockeys.tick(self);
		}
	}
}
