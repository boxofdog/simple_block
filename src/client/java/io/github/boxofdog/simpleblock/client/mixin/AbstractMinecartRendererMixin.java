package io.github.boxofdog.simpleblock.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.client.CargoMinecartRenderer;
import io.github.boxofdog.simpleblock.client.train.CobblestoneEngineRenderer;
import io.github.boxofdog.simpleblock.client.train.CouplingChains;

/**
 * Lets {@link CargoMinecartRenderer} draw a fitted cushion inside the cart (and, M06, the freight
 * cart's front gate), and (M05)
 * {@link CobblestoneEngineRenderer} draw the engine instead of the cart body. The hook sits just before
 * the cart body is drawn (the {@code scale(-1, -1, 1)} into model space), where the pose already
 * follows the rails, the slope and the hurt wobble; vanilla only offers a contents hook for a
 * display block. M05 also draws every minecart's coupling chains ({@link CouplingChains}).
 */
@Mixin(AbstractMinecartRenderer.class)
abstract class AbstractMinecartRendererMixin {
	@Inject(
			method = "submit(Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 1),
			cancellable = true
	)
	private void simpleBlock$submitCushion(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if ((Object) this instanceof CargoMinecartRenderer renderer) {
			renderer.submitCushion(state, poseStack, collector);
			renderer.submitGate(state, poseStack, collector);
		} else if ((Object) this instanceof CobblestoneEngineRenderer renderer) {
			renderer.submitEngine(state, poseStack, collector);
			// Skip the cart body, closing the pose vanilla opened.
			poseStack.popPose();
			ci.cancel();
		}
	}

	@Inject(
			method = "submit(Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD")
	)
	private void simpleBlock$submitChains(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		CouplingChains.submit(state, poseStack, collector);
	}

	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;F)V",
			at = @At("TAIL")
	)
	private void simpleBlock$extractChains(AbstractMinecart cart, MinecartRenderState state, float partialTicks, CallbackInfo ci) {
		CouplingChains.extract(cart, state, partialTicks);
	}
}
