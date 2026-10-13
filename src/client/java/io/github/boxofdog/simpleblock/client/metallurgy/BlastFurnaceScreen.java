package io.github.boxofdog.simpleblock.client.metallurgy;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceBlockEntity;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceMenu;
import io.github.boxofdog.simpleblock.metallurgy.MetallurgyContent;

/**
 * M08: the blast furnace's screen, on the vanilla furnace's background. Its input slot is the ore
 * reservoir, drawn here with the whole count (up to 516) and "count / 516" beside it; clicks on it go to
 * the server as menu buttons ({@link BlastFurnaceMenu}). The flame shows the fuel block burning, the
 * arrow the casting of the next block.
 */
public class BlastFurnaceScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<BlastFurnaceMenu> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
	private boolean reservoirClicked;

	public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
	}

	public static void register() {
		MenuScreens.register(MetallurgyContent.BLAST_FURNACE_MENU, BlastFurnaceScreen::new);
	}

	private boolean overReservoir(double x, double y) {
		return this.isHovering(BlastFurnaceMenu.RESERVOIR_X, BlastFurnaceMenu.RESERVOIR_Y, 16, 16, x, y);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = this.leftPos;
		int yo = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		if (this.menu.isLit()) {
			int height = Mth.ceil(this.menu.litProgress() * 13.0F) + 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - height, xo + 56, yo + 36 + 14 - height, 14, height);
		}
		int width = Mth.ceil(this.menu.castProgress() * 24.0F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, xo + 79, yo + 34, width, 16);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		super.extractLabels(graphics, xm, ym);
		int x = BlastFurnaceMenu.RESERVOIR_X;
		int y = BlastFurnaceMenu.RESERVOIR_Y;
		int count = this.menu.count();
		if (count > 0) {
			ItemStack shown = new ItemStack(this.menu.ore());
			graphics.item(shown, x, y);
			graphics.itemDecorations(this.font, shown, x, y, String.valueOf(count));
		}
		if (this.overReservoir(xm, ym)) {
			graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
		}
		graphics.text(this.font, count + " / " + BlastFurnaceBlockEntity.RESERVOIR_CAPACITY, x - 48, y + 4, -12566464, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.overReservoir(mouseX, mouseY) && this.menu.getCarried().isEmpty()) {
			int count = this.menu.count();
			Component what = count > 0 ? new ItemStack(this.menu.ore()).getItemName()
				: Component.translatableWithFallback("simple_block.blast_furnace.reservoir", "Raw iron, copper or gold");
			graphics.setTooltipForNextFrame(List.of(what.getVisualOrderText(),
				Component.literal(count + " / " + BlastFurnaceBlockEntity.RESERVOIR_CAPACITY).getVisualOrderText()), mouseX, mouseY);
			return;
		}
		super.extractTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.overReservoir(event.x(), event.y()) && (event.button() == 0 || event.button() == 1)) {
			boolean shift = event.hasShiftDown() || this.minecraft.hasShiftDown();
			int button = shift ? BlastFurnaceMenu.BUTTON_SHIFT : event.button() == 1 ? BlastFurnaceMenu.BUTTON_RIGHT : BlastFurnaceMenu.BUTTON_LEFT;
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
			this.reservoirClicked = true;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.reservoirClicked) {
			this.reservoirClicked = false;
			return true;
		}
		return super.mouseReleased(event);
	}
}
