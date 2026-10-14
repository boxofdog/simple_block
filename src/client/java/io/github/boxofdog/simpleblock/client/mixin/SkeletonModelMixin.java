package io.github.boxofdog.simpleblock.client.mixin;

import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.client.mob.LedgeGrabPose;

/** M09: a skeleton grabbing a ledge holds both arms straight up, reaching for the lip ({@link LedgeGrabPose}). */
@Mixin(SkeletonModel.class)
abstract class SkeletonModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/SkeletonRenderState;)V", at = @At("TAIL"))
	private void simpleBlock$grabPose(SkeletonRenderState state, CallbackInfo ci) {
		if (((LedgeGrabPose.Holder) state).simpleBlock$grabbing()) {
			SkeletonModel<?> model = (SkeletonModel<?>) (Object) this;
			model.rightArm.xRot = (float) -Math.PI;
			model.rightArm.yRot = 0.0F;
			model.rightArm.zRot = 0.15F;
			model.leftArm.xRot = (float) -Math.PI;
			model.leftArm.yRot = 0.0F;
			model.leftArm.zRot = -0.15F;
		}
	}
}
