package io.github.boxofdog.simpleblock.client.mixin;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import io.github.boxofdog.simpleblock.client.mob.LedgeGrabPose;

/** M09: the skeleton render state carries the ledge-grab pose ({@link LedgeGrabPose}). */
@Mixin(SkeletonRenderState.class)
abstract class SkeletonRenderStateMixin implements LedgeGrabPose.Holder {
	@Unique
	private boolean simpleBlock$grabbing;

	@Override
	public boolean simpleBlock$grabbing() {
		return this.simpleBlock$grabbing;
	}

	@Override
	public void simpleBlock$setGrabbing(boolean grabbing) {
		this.simpleBlock$grabbing = grabbing;
	}
}
