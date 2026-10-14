package io.github.boxofdog.simpleblock.client.mixin;

import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.client.mob.LedgeGrabPose;
import io.github.boxofdog.simpleblock.feature.SpiderJockeys;

/** M09: copies the skeleton's synced ledge-grab flag into its render state ({@link LedgeGrabPose}). */
@Mixin(AbstractSkeletonRenderer.class)
abstract class AbstractSkeletonRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/skeleton/AbstractSkeleton;Lnet/minecraft/client/renderer/entity/state/SkeletonRenderState;F)V",
		at = @At("TAIL"))
	private void simpleBlock$grabPose(AbstractSkeleton entity, SkeletonRenderState state, float partialTicks, CallbackInfo ci) {
		((LedgeGrabPose.Holder) state).simpleBlock$setGrabbing(entity instanceof SpiderJockeys.Grabbing grabbing && grabbing.simpleBlock$grabbing());
	}
}
