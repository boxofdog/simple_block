package io.github.boxofdog.simpleblock.mixin.workwear;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Spelunker's Speed / Trail Blazer: climbing ladders, vines, scaffolding and ropes is
 * {@code workwear.climb_speed} times as fast, up (vanilla's 0.2 climbing lift, less gravity, scaled) and
 * down (vanilla's 0.15 slide limit scaled). Runs on the client for its own player too, as movement does.
 */
@Mixin(LivingEntity.class)
abstract class ClimbingMixin {
	/** Vanilla's lift while climbing, and the gravity it works against. */
	@Unique
	private static final double CLIMB_LIFT = 0.2;
	@Unique
	private static final double GRAVITY = 0.08;
	@Unique
	private static final double SLIDE = 0.15F;

	@Unique
	private double simpleBlock$fallBeforeClimb;

	@Inject(method = "handleRelativeFrictionAndCalculateMovement", at = @At("RETURN"), cancellable = true)
	private void simpleBlock$fasterClimbUp(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
		Vec3 movement = cir.getReturnValue();
		LivingEntity self = (LivingEntity) (Object) this;
		if (movement.y == CLIMB_LIFT && self.onClimbable()) {
			double multiplier = Workwear.climbMultiplier(self);
			if (multiplier != 1.0) {
				cir.setReturnValue(new Vec3(movement.x, GRAVITY + (CLIMB_LIFT - GRAVITY) * multiplier, movement.z));
			}
		}
	}

	@Inject(method = "handleOnClimbable", at = @At("HEAD"))
	private void simpleBlock$rememberFall(Vec3 delta, CallbackInfoReturnable<Vec3> cir) {
		this.simpleBlock$fallBeforeClimb = delta.y;
	}

	@Inject(method = "handleOnClimbable", at = @At("RETURN"), cancellable = true)
	private void simpleBlock$fasterClimbDown(Vec3 delta, CallbackInfoReturnable<Vec3> cir) {
		Vec3 result = cir.getReturnValue();
		LivingEntity self = (LivingEntity) (Object) this;
		if (result.y == -SLIDE && this.simpleBlock$fallBeforeClimb < -SLIDE && self.onClimbable()) {
			double multiplier = Workwear.climbMultiplier(self);
			if (multiplier != 1.0) {
				cir.setReturnValue(new Vec3(result.x, Math.max(this.simpleBlock$fallBeforeClimb, -SLIDE * multiplier), result.z));
			}
		}
	}
}
