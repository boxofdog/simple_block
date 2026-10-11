package io.github.boxofdog.simpleblock.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
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
 * inventory gets the unusable top storage row covered over; that strip shows the heavy-load bar
 * and, in the survival inventory, the Sort button. Display only: the server decides what is
 * allowed.
 *
 * <p>The load bar has no text. Its length is the 64-ingot allowance, and it fills with one colour
 * per material (iron, copper, gold, diamond, netherite, in that order), so it shows both how full
 * the player is and what the weight is made of. Any material that is carried gets at least one
 * pixel, so even a nugget shows. The frame turns red at the cap. Hovering over the bar shows the
 * numbers: the total out of 64 and what each material contributes, carried equipment included.
 */
public final class InventoryScreenAddons {
	private static final int PANEL = 0xFFC6C6C6;
	/** Bar geometry, relative to the covered row: inner fill area 112 x 6 pixels (1.75 pixels per ingot). */
	private static final int BAR_X = 3;
	private static final int BAR_Y = 5;
	private static final int BAR_WIDTH = 112;
	private static final int BAR_HEIGHT = 6;
	private static final int FRAME = 0xFF373737;
	private static final int FRAME_FULL = 0xFFAA0000;
	/** Dark, like the experience bar's background, so light iron and dark netherite both stand out. */
	private static final int TRACK = 0xFF1E1E1E;
	/** Fill colour per {@link HeavyLoad.Material}, drawn in this order. */
	private static final HeavyLoad.Material[] ORDER = {HeavyLoad.Material.IRON, HeavyLoad.Material.COPPER, HeavyLoad.Material.GOLD,
		HeavyLoad.Material.DIAMOND, HeavyLoad.Material.NETHERITE};
	private static final int[] COLOURS = {0xFFD8D8D8, 0xFFD9774F, 0xFFF5CB3A, 0xFF4AEDD9, 0xFF7A6872};
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
			// Tooltips are drawn when the screen's deferred elements are flushed, which happens before
			// afterExtract; set it here so it is shown in this frame.
			ScreenEvents.afterBackground(screen).register(InventoryScreenAddons::loadTooltip);
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
		long[] units = HeavyLoad.carriedByMaterial(player);
		long load = Arrays.stream(units).sum();
		int[] bar = barBounds(container);
		graphics.outline(bar[0], bar[1], bar[2] - bar[0], bar[3] - bar[1], load >= HeavyLoad.CAPACITY ? FRAME_FULL : FRAME);
		int left = bar[0] + 1;
		int top = bar[1] + 1;
		graphics.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, TRACK);
		int[] widths = segmentWidths(units);
		for (int i = 0; i < ORDER.length; i++) {
			int width = widths[ORDER[i].ordinal()];
			if (width > 0) {
				graphics.fill(left, top, left + width, top + BAR_HEIGHT, COLOURS[i]);
				left += width;
			}
		}
	}

	/**
	 * Pixel width of each material's segment (indexed by ordinal): its share of the 64-ingot
	 * allowance, at least one pixel if any is carried, and never more than the bar in total (a
	 * creative overload kept in survival just fills the bar).
	 */
	private static volatile List<Component> shownTooltip;

	/** The tooltip shown over the load bar in the last frame, or null if the mouse was not over it (for the client test). */
	public static List<Component> shownTooltip() {
		return shownTooltip;
	}

	/** Screen-space bounds of the load bar including its frame, {x0, y0, x1, y1}, or null if the screen has none. */
	public static int[] barBounds(AbstractContainerScreen<?> screen) {
		int[] row = disabledRow(screen);
		if (row == null) {
			return null;
		}
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
		int left = panel.simpleBlock$getLeftPos() + row[0] + BAR_X;
		int top = panel.simpleBlock$getTopPos() + row[1] + BAR_Y;
		return new int[] {left, top, left + BAR_WIDTH + 2, top + BAR_HEIGHT + 2};
	}

	private static void loadTooltip(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int[] bar = barBounds((AbstractContainerScreen<?>) screen);
		Player player = Minecraft.getInstance().player;
		if (bar == null || player == null || mouseX < bar[0] || mouseX >= bar[2] || mouseY < bar[1] || mouseY >= bar[3]) {
			shownTooltip = null;
			return;
		}
		List<Component> lines = tooltipLines(HeavyLoad.carriedByMaterial(player));
		shownTooltip = lines;
		graphics.setComponentTooltipForNextFrame(Screens.getFont(screen), lines, mouseX, mouseY);
	}

	/**
	 * The load bar's tooltip, from the same per-material units the bar is drawn from: the total out
	 * of the allowance (rounded up, so it never shows room that is not there; red at the cap), then
	 * each carried material in bar order with a swatch of its colour (rounded to the nearest 0.01).
	 */
	static List<Component> tooltipLines(long[] units) {
		long total = Arrays.stream(units).sum();
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("simple_block.inventory.load", ingots(total, RoundingMode.CEILING), HeavyLoad.CAPACITY_INGOTS)
			.withColor(total >= HeavyLoad.CAPACITY ? 0xFFFF5555 : 0xFFFFFFFF));
		for (int i = 0; i < ORDER.length; i++) {
			long amount = units[ORDER[i].ordinal()];
			if (amount > 0) {
				lines.add(Component.literal("\u25A0 ").withColor(COLOURS[i]).append(Component.translatable("simple_block.inventory.load.material",
					Component.translatable("simple_block.inventory.load." + ORDER[i].key()), ingots(amount, RoundingMode.HALF_UP))
					.withColor(0xFFAAAAAA)));
			}
		}
		return lines;
	}

	/** Units of 1/216 ingot as ingots with up to two decimals, e.g. "6.44". */
	static String ingots(long units, RoundingMode rounding) {
		return BigDecimal.valueOf(units).divide(BigDecimal.valueOf(HeavyLoad.UNITS_PER_INGOT), 2, rounding).stripTrailingZeros().toPlainString();
	}

	static int[] segmentWidths(long[] units) {
		double scale = (double) BAR_WIDTH / Math.max(HeavyLoad.CAPACITY, Arrays.stream(units).sum());
		int[] widths = new int[units.length];
		int total = 0;
		for (int i = 0; i < units.length; i++) {
			widths[i] = units[i] > 0 ? (int) Math.max(1, Math.round(units[i] * scale)) : 0;
			total += widths[i];
		}
		// Rounding and the one-pixel minimum can overshoot by a few pixels: take them off the widest.
		while (total > BAR_WIDTH) {
			int widest = 0;
			for (int i = 1; i < widths.length; i++) {
				widest = widths[i] > widths[widest] ? i : widest;
			}
			widths[widest]--;
			total--;
		}
		return widths;
	}
}
