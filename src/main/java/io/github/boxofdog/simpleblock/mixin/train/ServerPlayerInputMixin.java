package io.github.boxofdog.simpleblock.mixin.train;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;

import io.github.boxofdog.simpleblock.train.EngineSelector;

/**
 * M15: a press of Space is seen as the client's input arrives (a quick tap's press and release can both
 * arrive before the next server tick), so the engine selector steps once for every press ({@link EngineSelector#jumpPressed}).
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerInputMixin {
	@Inject(method = "setLastClientInput", at = @At("HEAD"))
	private void simpleBlock$spacePressed(Input input, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (input.jump() && !self.getLastClientInput().jump()) {
			EngineSelector.jumpPressed(self);
		}
	}
}
