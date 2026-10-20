package io.github.boxofdog.simpleblock.client.train;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.train.CargoInspectMenu;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M13 (BUG-014): the freight cart's cargo window: a small grey panel with the cart's name and one
 * greyed-out slot holding the cargo and its count (empty when the cart is). Nothing in it can be clicked
 * or moved, and the player's inventory is not shown.
 */
public class CargoInspectScreen extends AbstractContainerScreen<CargoInspectMenu> {
	private static final int WIDTH = 96;
	private static final int HEIGHT = 56;
	public static final int SLOT_X = WIDTH / 2 - 9;
	public static final int SLOT_Y = 24;

	public CargoInspectScreen(CargoInspectMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	public static void register() {
		MenuScreens.register(TrainContent.CARGO_INSPECT_MENU, CargoInspectScreen::new);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x0 = this.leftPos;
		int y0 = this.topPos;
		graphics.fill(x0, y0, x0 + WIDTH, y0 + HEIGHT, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x0 + WIDTH - 1, y0 + HEIGHT - 1, 0xFFFFFFFF);
		graphics.fill(x0 + 3, y0 + 3, x0 + WIDTH - 1, y0 + HEIGHT - 1, 0xFF555555);
		graphics.fill(x0 + 3, y0 + 3, x0 + WIDTH - 3, y0 + HEIGHT - 3, 0xFFC6C6C6);
		// The slot, sunken like vanilla's.
		int sx = x0 + SLOT_X;
		int sy = y0 + SLOT_Y;
		graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
		graphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
		graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.centeredText(this.font, this.title, WIDTH / 2, 8, -12566464);
		int count = this.menu.count();
		if (count > 0) {
			ItemStack shown = new ItemStack(this.menu.cargo());
			graphics.item(shown, SLOT_X + 1, SLOT_Y + 1);
			graphics.itemDecorations(this.font, shown, SLOT_X + 1, SLOT_Y + 1, String.valueOf(count));
		}
		// Greyed out: a disabled slot.
		graphics.fill(SLOT_X + 1, SLOT_Y + 1, SLOT_X + 17, SLOT_Y + 17, 0x808B8B8B);
	}
}
