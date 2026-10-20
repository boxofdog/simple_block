package io.github.boxofdog.simpleblock.client.cable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CoalWinchMenu;

/**
 * M13 (BUG-013): the coal winch's engine box: vanilla's furnace screen with only its fuel slot and flame
 * (the input slot, arrow and output slot painted over in the screen's grey), and the inventory.
 */
public class CoalWinchScreen extends AbstractContainerScreen<CoalWinchMenu> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final int GREY = 0xFFC6C6C6;

	public CoalWinchScreen(CoalWinchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	public static void register() {
		MenuScreens.register(CableContent.COAL_WINCH_MENU, CoalWinchScreen::new);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = this.leftPos;
		int yo = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		// Only the fuel slot is the winch's: the furnace's input slot, arrow and output slot go.
		graphics.fill(xo + 54, yo + 15, xo + 74, yo + 35, GREY);
		graphics.fill(xo + 78, yo + 33, xo + 142, yo + 61, GREY);
		if (this.menu.isLit()) {
			int height = Mth.ceil(this.menu.litProgress() * 13.0F) + 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - height, xo + 56, yo + 36 + 14 - height, 14, height);
		}
	}
}
