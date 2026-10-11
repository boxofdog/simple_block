package io.github.boxofdog.simpleblock.mixin.food;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import io.github.boxofdog.simpleblock.feature.Exhaustion;

/**
 * FOOD-01: half exhaustion for sprinting, swimming and moving in water ({@code checkMovementStatistics};
 * walking and sneaking charge 0 there already) and for jumps and sprint-jumps ({@code jumpFromGround}).
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
	@ModifyArg(method = {"checkMovementStatistics", "jumpFromGround"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
	private float simpleBlock$halfActivityExhaustion(float exhaustion) {
		return Exhaustion.activity(exhaustion);
	}
}
