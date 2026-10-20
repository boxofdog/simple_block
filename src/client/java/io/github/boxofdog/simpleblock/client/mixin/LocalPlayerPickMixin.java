package io.github.boxofdog.simpleblock.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.cable.CableDrumBlock;

/**
 * M13 (BUG-008): the crosshair also finds a cable drum's crank or coal-winch lever where it sticks out
 * of the drum's cell (the lever's knob is above it), which the game's look test alone misses.
 */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerPickMixin {
	@ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
	private static HitResult simpleBlock$drumHandles(HitResult hit, @Local(argsOnly = true) Entity camera, @Local(argsOnly = true, ordinal = 0) double blockRange,
		@Local(argsOnly = true, ordinal = 1) double entityRange, @Local(argsOnly = true) float partialTicks) {
		Vec3 from = camera.getEyePosition(partialTicks);
		double reach = Math.max(blockRange, entityRange);
		Vec3 to = from.add(camera.getViewVector(partialTicks).scale(reach));
		BlockHitResult handle = CableDrumBlock.pickOverhang(camera.level(), from, to);
		if (handle != null && (hit.getType() == HitResult.Type.MISS || handle.getLocation().distanceToSqr(from) < hit.getLocation().distanceToSqr(from) - 1.0E-6)) {
			return handle;
		}
		return hit;
	}
}
