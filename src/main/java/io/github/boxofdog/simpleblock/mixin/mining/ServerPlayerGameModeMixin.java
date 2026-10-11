package io.github.boxofdog.simpleblock.mixin.mining;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;

import io.github.boxofdog.simpleblock.feature.DeepslateGate;

/**
 * MINE-01: when a client reports a finished break that the server rejects as too early, vanilla
 * keeps that block as the player's one "delayed destroy" and keeps adding mining progress until it
 * breaks. A gated block never gains progress, so the slot would stay taken and the player's later
 * breaks that need it would silently fail. Drop the delayed destroy instead. (A client running this
 * mod never reports such a break; a vanilla client joining a server can.)
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
	@Shadow
	protected ServerLevel level;
	@Shadow
	@Final
	protected ServerPlayer player;
	@Shadow
	private boolean hasDelayedDestroy;
	@Shadow
	private BlockPos delayedDestroyPos;

	@Inject(method = "tick", at = @At("HEAD"))
	private void simpleBlock$dropGatedDelayedDestroy(CallbackInfo ci) {
		if (this.hasDelayedDestroy && DeepslateGate.blocksMining(this.level.getBlockState(this.delayedDestroyPos), this.player)) {
			this.hasDelayedDestroy = false;
		}
	}
}
