package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.feature.Entrancement;

/**
 * MOB-01: the wander destination, whichever subclass chose it (land, water-avoiding, ...), may be
 * replaced by one towards lit ground. Called only when the mob decides to wander.
 */
@Mixin(RandomStrollGoal.class)
abstract class RandomStrollGoalMixin {
	@Shadow
	@Final
	protected PathfinderMob mob;

	@ModifyExpressionValue(method = "canUse", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/ai/goal/RandomStrollGoal;getPosition()Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 simpleBlock$drawnToLight(Vec3 wanted) {
		return Entrancement.redirect(this.mob, wanted);
	}
}
