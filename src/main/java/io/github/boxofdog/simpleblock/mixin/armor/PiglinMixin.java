package io.github.boxofdog.simpleblock.mixin.armor;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.feature.RetiredItems;

/** M10: piglins no longer spawn wearing golden armor (they keep their weapons). */
@Mixin(Piglin.class)
abstract class PiglinMixin {
	@Inject(method = "maybeWearArmor", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$noGoldenArmor(EquipmentSlot slot, ItemStack itemStack, RandomSource random, CallbackInfo ci) {
		if (RetiredItems.isRetired(itemStack)) {
			ci.cancel();
		}
	}
}
