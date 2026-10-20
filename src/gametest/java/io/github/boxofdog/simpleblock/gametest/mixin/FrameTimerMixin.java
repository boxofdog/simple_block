package io.github.boxofdog.simpleblock.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;

import io.github.boxofdog.simpleblock.gametest.client.FrameTimes;

/** Client tests: how long each frame's render takes ({@link FrameTimes}, for the M13 Trail Blazer benchmark). */
@Mixin(GameRenderer.class)
abstract class FrameTimerMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void simpleBlockTest$start(CallbackInfo ci) {
		FrameTimes.start();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void simpleBlockTest$end(CallbackInfo ci) {
		FrameTimes.end();
	}
}
