package io.github.boxofdog.simpleblock.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.client.CargoMinecartRenderer;

/**
 * Lets {@link CargoMinecartRenderer} draw a fitted cushion inside the cart. The hook sits just before
 * the cart body is drawn (the {@code scale(-1, -1, 1)} into model space), where the pose already
 * follows the rails, the slope and the hurt wobble; vanilla only offers a contents hook for a
 * display block.
 */
@Mixin(AbstractMinecartRenderer.class)
abstract class AbstractMinecartRendererMixin {
	@Inject(
			method = "submit(Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 1)
	)
	private void simpleBlock$submitCushion(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if ((Object) this instanceof CargoMinecartRenderer renderer) {
			renderer.submitCushion(state, poseStack, collector);
		}
	}
}
