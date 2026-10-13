package io.github.boxofdog.simpleblock.client.skip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.skip.SkipCargo;
import io.github.boxofdog.simpleblock.skip.SkipContent;
import io.github.boxofdog.simpleblock.skip.SkipMenu;

/**
 * M07: the ore skip's screen, on the hopper's background with its one cargo slot in the middle. The slot
 * shows the skip's item and its whole count (up to 512) and, under it, "count / 512". It is drawn here
 * rather than being a vanilla slot ({@link SkipMenu}); clicks on it go to the server as menu buttons.
 */
public class SkipScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<SkipMenu> {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
	/** The hopper background's fill colour, to cover its other four slots. */
	private static final int PANEL = 0xFFC6C6C6;
	private boolean cargoClicked;

	public SkipScreen(SkipMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 133);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	public static void register() {
		MenuScreens.register(SkipContent.SKIP_MENU, SkipScreen::new);
	}

	private boolean overCargo(double x, double y) {
		return this.isHovering(SkipMenu.CARGO_X, SkipMenu.CARGO_Y, 16, 16, x, y);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = (this.width - this.imageWidth) / 2;
		int yo = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		// One slot: cover the hopper's other four.
		graphics.fill(xo + 43, yo + 19, xo + SkipMenu.CARGO_X - 1, yo + 37, PANEL);
		graphics.fill(xo + SkipMenu.CARGO_X + 17, yo + 19, xo + 133, yo + 37, PANEL);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		super.extractLabels(graphics, xm, ym);
		int x = SkipMenu.CARGO_X;
		int y = SkipMenu.CARGO_Y;
		int count = this.menu.count();
		if (count > 0) {
			ItemStack shown = new ItemStack(this.menu.cargo());
			graphics.item(shown, x, y);
			graphics.itemDecorations(this.font, shown, x, y, String.valueOf(count));
		}
		if (this.overCargo(xm, ym)) {
			graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
		}
		String capacity = count + " / " + SkipCargo.CAPACITY;
		graphics.text(this.font, capacity, x + 22, y + 4, -12566464, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.overCargo(mouseX, mouseY) && this.menu.getCarried().isEmpty()) {
			List<FormattedCharSequence> lines = new ArrayList<>();
			int count = this.menu.count();
			Component what = count > 0 ? new ItemStack(this.menu.cargo()).getItemName()
				: Component.translatableWithFallback("simple_block.skip.empty", "Empty");
			lines.add(what.getVisualOrderText());
			lines.add(Component.literal(count + " / " + SkipCargo.CAPACITY).getVisualOrderText());
			graphics.setTooltipForNextFrame(lines, mouseX, mouseY);
			return;
		}
		super.extractTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.overCargo(event.x(), event.y()) && (event.button() == 0 || event.button() == 1)) {
			boolean shift = event.hasShiftDown() || this.minecraft.hasShiftDown();
			int button = shift ? SkipMenu.BUTTON_SHIFT : event.button() == 1 ? SkipMenu.BUTTON_RIGHT : SkipMenu.BUTTON_LEFT;
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
			this.cargoClicked = true;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.cargoClicked) {
			this.cargoClicked = false;
			return true;
		}
		return super.mouseReleased(event);
	}
}
