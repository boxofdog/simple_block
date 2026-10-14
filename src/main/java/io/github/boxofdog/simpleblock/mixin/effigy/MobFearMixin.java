package io.github.boxofdog.simpleblock.mixin.effigy;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.boxofdog.simpleblock.effigy.Effigies;

/**
 * M12: a mob frightened by an effigy takes no target while it flees ({@link Effigies}): every target
 * goal, retaliation and anger goes through {@code setTarget}, which then clears the target instead.
 * (The enderman's own {@code setTarget} is covered by {@code EndermanFearMixin}.)
 */
@Mixin(Mob.class)
abstract class MobFearMixin implements Effigies.Fearful {
	@Unique
	private boolean simpleBlock$afraid;

	@Override
	public boolean simpleBlock$afraid() {
		return this.simpleBlock$afraid;
	}

	@Override
	public void simpleBlock$setAfraid(boolean afraid) {
		this.simpleBlock$afraid = afraid;
	}

	@ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
	private @Nullable LivingEntity simpleBlock$noTargetWhileAfraid(@Nullable LivingEntity target) {
		return this.simpleBlock$afraid ? null : target;
	}
}
