package io.github.boxofdog.simpleblock.mixin.mining;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import io.github.boxofdog.simpleblock.feature.DeepslateGate;

/**
 * MINE-01: zero mining progress on natural deepslate and deepslate ores without an iron-tier or
 * better pickaxe. The server uses the same value to accept or reject a finished break, so a
 * modified client cannot get around it.
 */
@Mixin(BlockBehaviour.class)
abstract class BlockBehaviourMixin {
	@ModifyReturnValue(method = "getDestroyProgress", at = @At("RETURN"))
	private float simpleBlock$requireIronPickaxe(float progress, @Local(argsOnly = true) BlockState state,
		@Local(argsOnly = true) Player player) {
		return DeepslateGate.blocksMining(state, player) ? 0.0F : progress;
	}
}
