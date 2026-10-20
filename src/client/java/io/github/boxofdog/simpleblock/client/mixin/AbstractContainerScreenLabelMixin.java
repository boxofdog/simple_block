package io.github.boxofdog.simpleblock.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.boxofdog.simpleblock.client.InventoryScreenAddons;

/** BUG-010: where the load bar takes the player inventory label's row, the label is not drawn under it. */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenLabelMixin {
	@WrapWithCondition(method = "extractLabels", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
		ordinal = 1))
	private boolean simpleBlock$inventoryLabel(final GuiGraphicsExtractor graphics, final Font font, final Component text, final int x, final int y,
		final int color, final boolean shadow) {
		return !InventoryScreenAddons.labelRowBar((AbstractContainerScreen<?>) (Object) this);
	}
}
