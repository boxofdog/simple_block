package io.github.boxofdog.simpleblock.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import io.github.boxofdog.simpleblock.client.mixin.AbstractContainerScreenAccessor;
import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.network.SortInventoryPayload;

/**
 * Client display for INV-01, INV-02 and INV-04. Every container screen that shows the player's
 * inventory gets the unusable top storage row covered over; that strip shows the heavy-load
 * indicator ("Heavy 37.4 / 64", red at the cap) and, in the survival inventory, the Sort button.
 * Display only: the server decides what is allowed.
 */
public final class InventoryScreenAddons {
	private static final int PANEL = 0xFFC6C6C6;
	private static final int TEXT = 0xFF404040;
	private static final int TEXT_FULL = 0xFFAA0000;
	private static final int BUTTON_WIDTH = 34;
	private static final int BUTTON_HEIGHT = 14;

	private InventoryScreenAddons() {
	}

	/** Panel-relative bounds of the disabled row: {x0, y0, x1, y1}, or null if the screen has none. */
	static int[] disabledRow(AbstractContainerScreen<?> screen) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int y = Integer.MIN_VALUE;
		for (Slot slot : screen.getMenu().slots) {
			if (InventorySlots.isDisabled(slot)) {
				minX = Math.min(minX, slot.x);
				maxX = Math.max(maxX, slot.x);
				y = slot.y;
			}
		}
		return y == Integer.MIN_VALUE ? null : new int[] {minX - 1, y - 1, maxX + 17, y + 17};
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container) || disabledRow(container) == null) {
				return;
			}
			ScreenEvents.afterBackground(screen).register(InventoryScreenAddons::paintRow);
			if (screen instanceof InventoryScreen inventoryScreen) {
				Button sort = Button.builder(Component.translatable("simple_block.inventory.sort"),
						button -> ClientPlayNetworking.send(new SortInventoryPayload()))
					.size(BUTTON_WIDTH, BUTTON_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("simple_block.inventory.sort.tooltip")))
					.build();
				placeSortButton(inventoryScreen, sort);
				Screens.getWidgets(screen).add(sort);
				// The recipe book moves the panel; follow it.
				ScreenEvents.afterTick(screen).register(s -> placeSortButton(inventoryScreen, sort));
			}
		});
	}

	private static void placeSortButton(InventoryScreen screen, Button button) {
		int[] row = disabledRow(screen);
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
		button.setX(panel.simpleBlock$getLeftPos() + row[2] - BUTTON_WIDTH - 1);
		button.setY(panel.simpleBlock$getTopPos() + row[1] + 2);
	}

	private static void paintRow(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
		int[] row = disabledRow(container);
		Player player = Minecraft.getInstance().player;
		if (row == null || player == null) {
			return;
		}
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) container;
		int x = panel.simpleBlock$getLeftPos();
		int y = panel.simpleBlock$getTopPos();
		graphics.fill(x + row[0], y + row[1], x + row[2], y + row[3], PANEL);
		int load = HeavyLoad.carried(player);
		Font font = Screens.getFont(screen);
		graphics.text(font, Component.translatable("simple_block.inventory.heavy", HeavyLoad.describe(load)),
			x + row[0] + 2, y + row[1] + 5, load >= HeavyLoad.CAPACITY ? TEXT_FULL : TEXT, false);
	}
}
