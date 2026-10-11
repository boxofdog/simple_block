package io.github.boxofdog.simpleblock.mixin.light;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import io.github.boxofdog.simpleblock.feature.LightLevels;

/** LIGHT-01: torches emit 12. The light engine reads emission through this method on both sides. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
	@Shadow
	public abstract Block getBlock();

	@ModifyReturnValue(method = "getLightEmission", at = @At("RETURN"))
	private int simpleBlock$torchLight(int emission) {
		return emission > 0 && LightLevels.isTorch(this.getBlock()) ? LightLevels.TORCH : emission;
	}
}
