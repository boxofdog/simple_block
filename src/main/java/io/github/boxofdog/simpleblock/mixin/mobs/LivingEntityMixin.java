package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.feature.SpiderJockeys;

/** M09: a spider being hauled round a ledge moves only by the haul ({@link SpiderJockeys#tick}), not by its own walking, climbing or gravity. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$hauled(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof Spider spider && SpiderJockeys.isHauling(spider)) {
			ci.cancel();
		}
	}
}
