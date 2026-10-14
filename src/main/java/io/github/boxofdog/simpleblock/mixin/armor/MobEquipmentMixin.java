package io.github.boxofdog.simpleblock.mixin.armor;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * M10: mobs that spawn wearing random armor never get golden (tier 2) or chainmail (tier 3) pieces;
 * those tiers become copper, the tier below. The other tiers are unchanged.
 */
@Mixin(Mob.class)
abstract class MobEquipmentMixin {
	@ModifyVariable(method = "getEquipmentForSlot", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static int simpleBlock$noRetiredArmorTier(int type) {
		return type == 2 || type == 3 ? 1 : type;
	}
}
