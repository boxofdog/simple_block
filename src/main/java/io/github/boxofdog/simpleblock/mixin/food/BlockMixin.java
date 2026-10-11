package io.github.boxofdog.simpleblock.mixin.food;

import net.minecraft.world.level.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import io.github.boxofdog.simpleblock.feature.Exhaustion;

/** FOOD-01: half exhaustion for breaking a block (vanilla charges it in {@code Block.playerDestroy}). */
@Mixin(Block.class)
abstract class BlockMixin {
	@ModifyArg(method = "playerDestroy",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
	private float simpleBlock$halfMiningExhaustion(float exhaustion) {
		return Exhaustion.activity(exhaustion);
	}
}
