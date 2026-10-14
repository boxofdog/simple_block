package io.github.boxofdog.simpleblock.mixin.workwear;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Trail Blazer: a skeleton (or stray, bogged, parched) shooting at someone with the full spelunking
 * outfit aims worse: {@code workwear.trail_blazer_aim_penalty} added to its arrows' spread (vanilla's is
 * 14 - 4 x difficulty, so 2 on Hard).
 */
@Mixin(AbstractSkeleton.class)
abstract class SkeletonAimMixin {
	@WrapOperation(method = "performRangedAttack",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/skeleton/AbstractSkeleton;rangedAttackUncertainty(Lnet/minecraft/world/level/Level;)F"))
	private float simpleBlock$worseAimAtTrailBlazers(AbstractSkeleton skeleton, Level level, Operation<Float> original,
		@Local(argsOnly = true) LivingEntity target) {
		float spread = original.call(skeleton, level);
		return Workwear.bonus(target) == Workwear.Bonus.TRAIL_BLAZER ? spread + (float) Tuning.getDouble("workwear.trail_blazer_aim_penalty") : spread;
	}
}
