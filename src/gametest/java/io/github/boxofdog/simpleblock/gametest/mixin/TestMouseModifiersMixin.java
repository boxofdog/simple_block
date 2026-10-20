package io.github.boxofdog.simpleblock.gametest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Test harness only: Fabric's simulated mouse buttons carry no modifier keys, where a real SDL click
 * carries the held Shift. With Shift held through {@code TestInput}, the click says so, as a real one would.
 */
@Mixin(MouseHandler.class)
abstract class TestMouseModifiersMixin {
	@ModifyVariable(method = "onButton", at = @At("HEAD"), argsOnly = true)
	private MouseButtonInfo simpleBlock$heldShift(final MouseButtonInfo info) {
		if (info.modifiers() == 0 && Minecraft.getInstance().hasShiftDown()) {
			return new MouseButtonInfo(info.button(), 1);
		}
		return info;
	}
}
