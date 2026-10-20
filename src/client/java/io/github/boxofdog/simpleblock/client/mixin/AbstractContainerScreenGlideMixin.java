package io.github.boxofdog.simpleblock.client.mixin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.client.InventoryScreenAddons;

/**
 * M13: Shift + right-button glide. Vanilla's Shift + right-click quick-moves the slot under the cursor
 * (with an empty cursor); holding the button and gliding over more slots then does the same for each
 * occupied slot the cursor enters, once per slot per gesture. Each move is the ordinary QUICK_MOVE click,
 * so the server's own quick-move (hotbar-first priority, commodity weight, Overalls row, read-only and
 * output slots) decides what happens. Releasing the button or Shift, or holding an item, ends the gesture.
 */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenGlideMixin<T extends AbstractContainerMenu> {
	/** GUI pixels between samples along a fast drag, so a quick sweep cannot skip a 16-pixel slot. */
	@Unique
	private static final double SIMPLE_BLOCK$STEP = 4.0;

	@Shadow
	@Final
	protected T menu;

	@Shadow
	protected boolean isQuickCrafting;

	@Shadow
	private @Nullable Slot getHoveredSlot(final double x, final double y) {
		throw new AssertionError();
	}

	@Shadow
	protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

	/** Slots already handled in the current gesture; null when no gesture is running. */
	@Unique
	private @Nullable Set<Slot> simpleBlock$glided;

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void simpleBlock$startGlide(final MouseButtonEvent event, final boolean doubleClick, final CallbackInfoReturnable<Boolean> cir) {
		this.simpleBlock$glided = null;
		Slot slot = this.getHoveredSlot(event.x(), event.y());
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && event.hasShiftDown() && slot != null && this.menu.getCarried().isEmpty() && !this.isQuickCrafting) {
			// Vanilla quick-moves this first slot itself.
			this.simpleBlock$glided = Collections.newSetFromMap(new IdentityHashMap<>());
			this.simpleBlock$glided.add(slot);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$glide(final MouseButtonEvent event, final double dx, final double dy, final CallbackInfoReturnable<Boolean> cir) {
		Set<Slot> glided = this.simpleBlock$glided;
		if (glided == null || event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
			return;
		}
		// A drag event repeats the modifiers of the original press, so Shift is read live.
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.hasShiftDown() || !this.menu.getCarried().isEmpty()) {
			this.simpleBlock$glided = null;
			return;
		}
		int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / SIMPLE_BLOCK$STEP));
		for (int i = steps - 1; i >= 0; i--) {
			Slot slot = this.getHoveredSlot(event.x() - dx * i / steps, event.y() - dy * i / steps);
			if (slot != null && glided.add(slot) && slot.hasItem() && minecraft.player != null && slot.mayPickup(minecraft.player)) {
				this.slotClicked(slot, slot.index, 1, ContainerInput.QUICK_MOVE);
				InventoryScreenAddons.glideMoves++;
			}
		}
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"))
	private void simpleBlock$endGlide(final MouseButtonEvent event, final CallbackInfoReturnable<Boolean> cir) {
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			this.simpleBlock$glided = null;
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void simpleBlock$closeGlide(final CallbackInfo ci) {
		this.simpleBlock$glided = null;
	}
}
