package io.github.boxofdog.simpleblock.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Where a container screen's panel sits; slot positions are relative to it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int simpleBlock$getLeftPos();

	@Accessor("topPos")
	int simpleBlock$getTopPos();
}
