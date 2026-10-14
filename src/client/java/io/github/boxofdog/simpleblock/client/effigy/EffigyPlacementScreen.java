package io.github.boxofdog.simpleblock.client.effigy;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.effigy.EffigyContent;
import io.github.boxofdog.simpleblock.effigy.EffigyPlacementMenu;

/**
 * M12: the confirmation screen before an effigy is placed ({@link EffigyPlacementMenu}): the effigy,
 * the owner's warning and the Place and Cancel buttons. Either button works once (both go grey after a
 * click); the server checks Place again and closes the screen.
 */
public class EffigyPlacementScreen extends AbstractContainerScreen<EffigyPlacementMenu> {
	public static final Component WARNING = Component.translatable("simple_block.effigy.confirm");
	private static final int WIDTH = 220;
	private static final int HEIGHT = 120;
	private static final int BUTTON_WIDTH = 80;
	private Button place;
	private Button cancel;

	public EffigyPlacementScreen(EffigyPlacementMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	public static void register() {
		MenuScreens.register(EffigyContent.PLACEMENT_MENU, EffigyPlacementScreen::new);
	}

	@Override
	protected void init() {
		super.init();
		int y = this.topPos + HEIGHT - 28;
		this.place = this.addRenderableWidget(Button.builder(Component.translatable("simple_block.effigy.place"), button -> this.press(EffigyPlacementMenu.BUTTON_PLACE))
			.bounds(this.leftPos + WIDTH / 2 - BUTTON_WIDTH - 6, y, BUTTON_WIDTH, 20).build());
		this.cancel = this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.press(EffigyPlacementMenu.BUTTON_CANCEL))
			.bounds(this.leftPos + WIDTH / 2 + 6, y, BUTTON_WIDTH, 20).build());
	}

	private void press(int button) {
		this.place.active = false;
		this.cancel.active = false;
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x0 = this.leftPos;
		int y0 = this.topPos;
		// A plain vanilla-grey panel with the bevelled edge of the container screens.
		graphics.fill(x0, y0, x0 + WIDTH, y0 + HEIGHT, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x0 + WIDTH - 1, y0 + HEIGHT - 1, 0xFFFFFFFF);
		graphics.fill(x0 + 3, y0 + 3, x0 + WIDTH - 1, y0 + HEIGHT - 1, 0xFF555555);
		graphics.fill(x0 + 3, y0 + 3, x0 + WIDTH - 3, y0 + HEIGHT - 3, 0xFFC6C6C6);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.centeredText(this.font, this.title, WIDTH / 2, 8, -12566464);
		ItemStack shown = new ItemStack(EffigyContent.effigyItem(this.menu.type()));
		graphics.item(shown, WIDTH / 2 - 8, 22);
		List<FormattedCharSequence> lines = this.font.split(WARNING, WIDTH - 24);
		int y = 46;
		for (FormattedCharSequence line : lines) {
			graphics.centeredText(this.font, line, WIDTH / 2, y, -12566464);
			y += this.font.lineHeight + 1;
		}
	}
}
