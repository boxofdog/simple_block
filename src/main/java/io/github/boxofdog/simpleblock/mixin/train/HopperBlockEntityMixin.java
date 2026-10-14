package io.github.boxofdog.simpleblock.mixin.train;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import io.github.boxofdog.simpleblock.train.FreightHoppers;

/**
 * M11: a hopper pointing down into a freight cart docked at a Loading Rail pushes into it like into any
 * container below it ({@link FreightHoppers#intake}); everything else about the hopper is vanilla.
 */
@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
	@Inject(method = "getAttachedContainer", at = @At("RETURN"), cancellable = true)
	private static void simpleBlock$feedDockedCart(Level level, BlockPos pos, HopperBlockEntity self, CallbackInfoReturnable<Container> cir) {
		if (cir.getReturnValue() == null && level instanceof ServerLevel serverLevel) {
			Container cart = FreightHoppers.intake(serverLevel, pos, level.getBlockState(pos));
			if (cart != null) {
				cir.setReturnValue(cart);
			}
		}
	}
}
