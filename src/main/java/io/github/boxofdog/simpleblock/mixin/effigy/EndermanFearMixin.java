package io.github.boxofdog.simpleblock.mixin.effigy;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.boxofdog.simpleblock.effigy.Effigies;

/** M12: a frightened enderman takes no target either, so it does not turn "creepy" or gain its attack speed. */
@Mixin(Enderman.class)
abstract class EndermanFearMixin {
	@ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
	private @Nullable LivingEntity simpleBlock$noTargetWhileAfraid(@Nullable LivingEntity target) {
		return Effigies.afraid((Enderman) (Object) this) ? null : target;
	}
}
