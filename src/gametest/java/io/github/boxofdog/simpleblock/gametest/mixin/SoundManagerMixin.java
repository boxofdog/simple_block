package io.github.boxofdog.simpleblock.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

import io.github.boxofdog.simpleblock.gametest.client.PlayedSounds;

/**
 * Client tests: every sound the client is asked to play, by name, whether or not there is an audio
 * device to play it on (with none, as under xvfb, the sound engine drops sounds before its listeners).
 */
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
	@Inject(method = "play", at = @At("HEAD"))
	private void simpleBlockTest$record(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		PlayedSounds.PLAYED.add(instance.getIdentifier());
	}
}
