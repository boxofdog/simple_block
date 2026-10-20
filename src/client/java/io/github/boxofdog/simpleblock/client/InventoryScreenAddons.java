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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;

import io.github.boxofdog.simpleblock.client.mixin.AbstractContainerScreenAccessor;
import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.network.SortInventoryPayload;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * Client display for INV-01, INV-02 and INV-04. Every container screen that shows the player's
 * inventory gets the unusable top storage row covered over; that strip shows the heavy-load bar
 * and, in the survival inventory, the Sort button. Display only: the server decides what is
 * allowed.
 *
 * <p>The load bar has no text. Its length is the 64-ingot allowance, and it fills with one colour
 * per material (iron, copper, gold, diamond, netherite, in that order), so it shows both how full
 * the player is and what the weight is made of. Any material that is carried gets at least one
 * pixel, so even a nugget shows. The frame turns red at the cap. Carried equipment can take the
 * load past 64 (M08): the bar then rescales to the whole load and a white tick marks where 64 is.
 * Hovering over the bar shows the numbers: the total out of 64 and what each material contributes,
 * carried equipment included, and at the cap that only commodities are refused.
 *
 * <p>M10: with the Overalls on, the row is a backpack (usable slots), so the bar and the Sort button sit on
 * a strip just under the panel instead; the bar's allowance is the player's own (more with Mining Boots).
 * The armor slots the commodity-overload lock holds are greyed and outlined in red, and the legs slot
 * the Overalls take is greyed: display only, the server enforces both.
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
	/** The Sort button has the bar's profile: same height and level, framed the same way, right-aligned in the row. */
	private static final int BUTTON_WIDTH = 15;
	private static final int BUTTON_HEIGHT = BAR_HEIGHT + 2;
	private static final int ICON = 0xFFD8D8D8;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int CAP_TICK = 0xFFFFFFFF;

	private InventoryScreenAddons() {
	}

	private static final int LOCKED_SHADE = 0x99606060;
	private static final int LOCKED_OUTLINE = 0xFFE02020;
	private static final int RESERVED_SHADE = 0x77808080;

	/**
	 * Panel-relative bounds of the strip the bar and Sort button sit on: the unusable row, or (Overalls
	 * on, the row a backpack) a strip just under the panel; {x0, y0, x1, y1}, or null if the screen shows
	 * no player inventory.
	 */
	static int[] disabledRow(AbstractContainerScreen<?> screen) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int y = Integer.MIN_VALUE;
		for (Slot slot : screen.getMenu().slots) {
			if (InventorySlots.isRow(slot)) {
				minX = Math.min(minX, slot.x);
				maxX = Math.max(maxX, slot.x);
				y = slot.y;
			}
		}
		if (y == Integer.MIN_VALUE) {
			return null;
		}
		if (Workwear.backpack(Minecraft.getInstance().player)) {
			int below = ((AbstractContainerScreenAccessor) screen).simpleBlock$getImageHeight() + 1;
			return new int[] {minX - 1, below, maxX + 17, below + 18};
		}
		return new int[] {minX - 1, y - 1, maxX + 17, y + 17};
	}

	/** M13: quick-moves sent by Shift + right-button glides (the first, vanilla, click excluded); read by the client test. */
	public static int glideMoves;

	/** M13: the chest menu the server last said can be sorted ({@link io.github.boxofdog.simpleblock.network.SortableChestPayload}). */
	private static int sortableChest = Integer.MIN_VALUE;

	/** Panel-relative position of the chest Sort button: right-aligned on the title's row, clear of every slot. */
	public static int[] chestSortButton(AbstractContainerScreen<?> screen) {
		int width = ((AbstractContainerScreenAccessor) screen).simpleBlock$getImageWidth();
		return new int[] {width - 8 - BUTTON_WIDTH, 5, BUTTON_WIDTH, BUTTON_HEIGHT};
	}

	public static void register() {
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
			io.github.boxofdog.simpleblock.network.SortableChestPayload.TYPE, (payload, context) -> sortableChest = payload.containerId());
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			// M13: a chest's screen gets its own Sort button, for the chest's slots only.
			if (screen instanceof AbstractContainerScreen<?> chest && chest.getMenu() instanceof net.minecraft.world.inventory.ChestMenu menu
				&& menu.containerId == sortableChest) {
				SortButton sort = new SortButton(button -> ClientPlayNetworking.send(new io.github.boxofdog.simpleblock.network.SortChestPayload(menu.containerId)));
				sort.setTooltip(Tooltip.create(Component.translatable("simple_block.chest.sort.tooltip")));
				AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) chest;
				int[] at = chestSortButton(chest);
				sort.setX(panel.simpleBlock$getLeftPos() + at[0]);
				sort.setY(panel.simpleBlock$getTopPos() + at[1]);
				Screens.getWidgets(screen).add(sort);
			}
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container) || disabledRow(container) == null) {
				return;
			}
			ScreenEvents.afterBackground(screen).register(InventoryScreenAddons::paintRow);
			// Tooltips are drawn when the screen's deferred elements are flushed, which happens before
			// afterExtract; set it here so it is shown in this frame.
			ScreenEvents.afterBackground(screen).register(InventoryScreenAddons::loadTooltip);
			ScreenEvents.afterExtract(screen).register(InventoryScreenAddons::paintArmorSlots);
			if (screen instanceof InventoryScreen inventoryScreen) {
				SortButton sort = new SortButton();
				sort.setTooltip(Tooltip.create(Component.translatable("simple_block.inventory.sort.tooltip")));
				placeSortButton(inventoryScreen, sort);
				Screens.getWidgets(screen).add(sort);
				// The recipe book moves the panel; follow it.
				ScreenEvents.afterTick(screen).register(s -> placeSortButton(inventoryScreen, sort));
			}
		});
	}

	private static void placeSortButton(InventoryScreen screen, SortButton button) {
		int[] row = disabledRow(screen);
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) screen;
		button.setX(panel.simpleBlock$getLeftPos() + row[2] - BAR_X - BUTTON_WIDTH);
		button.setY(panel.simpleBlock$getTopPos() + row[1] + BAR_Y);
	}

	/**
	 * INV-02: a Sort button with the load bar's profile (8 pixels high, on the bar's level, dark
	 * with a frame that lights up under the mouse) and a small sorted-bars icon instead of text;
	 * the tooltip says what it does.
	 */
	private static final class SortButton extends Button {
		SortButton() {
			this(button -> ClientPlayNetworking.send(new SortInventoryPayload()));
		}

		SortButton(Button.OnPress press) {
			super(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("simple_block.inventory.sort"), press, DEFAULT_NARRATION);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			int x = this.getX();
			int y = this.getY();
			boolean lit = this.isHoveredOrFocused();
			graphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, TRACK);
			graphics.outline(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, lit ? HIGHLIGHT : FRAME);
			// Three bars of rising height (2, 4, 6 pixels), bottom-aligned: "sorted".
			int colour = lit ? HIGHLIGHT : ICON;
			int bottom = y + BUTTON_HEIGHT - 1;
			int left = x + (BUTTON_WIDTH - 8) / 2;
			for (int i = 0; i < 3; i++) {
				graphics.fill(left + i * 3, bottom - 2 * (i + 1), left + i * 3 + 2, bottom, colour);
			}
		}
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
		if (Workwear.backpack(player)) {
			graphics.outline(x + row[0] - 1, y + row[1] - 1, row[2] - row[0] + 2, row[3] - row[1] + 2, FRAME);
		}
		long[] units = HeavyLoad.carriedByMaterial(player);
		long load = Arrays.stream(units).sum();
		int[] bar = barBounds(container);
		graphics.outline(bar[0], bar[1], bar[2] - bar[0], bar[3] - bar[1], load >= HeavyLoad.capacity(player) ? FRAME_FULL : FRAME);
		int left = bar[0] + 1;
		int top = bar[1] + 1;
		graphics.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, TRACK);
		int[] widths = segmentWidths(units, HeavyLoad.capacity(player));
		for (int i = 0; i < ORDER.length; i++) {
			int width = widths[ORDER[i].ordinal()];
			if (width > 0) {
				graphics.fill(left, top, left + width, top + BAR_HEIGHT, COLOURS[i]);
				left += width;
			}
		}
		int tick = capTick(load);
		if (tick >= 0) {
			graphics.fill(bar[0] + 1 + tick, top - 1, bar[0] + 2 + tick, top + BAR_HEIGHT + 1, CAP_TICK);
		}
	}

	/** The worn slot an armor slot of the player's inventory shows (inventory entries 36-39: feet, legs, chest, head). */
	private static EquipmentSlot armorSlot(Slot slot) {
		return switch (slot.getContainerSlot()) {
			case 36 -> EquipmentSlot.FEET;
			case 37 -> EquipmentSlot.LEGS;
			case 38 -> EquipmentSlot.CHEST;
			case 39 -> EquipmentSlot.HEAD;
			default -> null;
		};
	}

	/** M10: locked armor slots greyed with a red outline; the legs slot the Overalls take greyed. */
	private static void paintArmorSlots(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !(screen instanceof AbstractContainerScreen<?> container)) {
			return;
		}
		java.util.Set<EquipmentSlot> locked = Workwear.lockedSlots(player);
		boolean reserved = Workwear.legsReserved(player);
		if (locked.isEmpty() && !reserved) {
			return;
		}
		AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) container;
		for (Slot slot : container.getMenu().slots) {
			if (!(slot instanceof ArmorSlot) || !(slot.container instanceof Inventory inventory) || inventory.player != player || !slot.isActive()) {
				continue;
			}
			EquipmentSlot worn = armorSlot(slot);
			int x = panel.simpleBlock$getLeftPos() + slot.x;
			int y = panel.simpleBlock$getTopPos() + slot.y;
			if (worn != null && locked.contains(worn)) {
				graphics.fill(x, y, x + 16, y + 16, LOCKED_SHADE);
				graphics.outline(x - 1, y - 1, 18, 18, LOCKED_OUTLINE);
			} else if (worn == EquipmentSlot.LEGS && reserved) {
				graphics.fill(x, y, x + 16, y + 16, RESERVED_SHADE);
			}
		}
	}

	/** Pixel offset of the 64-ingot mark inside the bar when the load is over the cap, else -1. */
	static int capTick(long load) {
		if (load <= HeavyLoad.CAPACITY) {
			return -1;
		}
		return (int) Math.min(BAR_WIDTH - 1, Math.round((double) BAR_WIDTH * HeavyLoad.CAPACITY / load));
	}

	/**
	 * Pixel width of each material's segment (indexed by ordinal): its share of the 64-ingot
	 * allowance, at least one pixel if any is carried, and never more than the bar in total (over
	 * the cap the bar rescales to the whole load).
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
		List<Component> lines = tooltipLines(HeavyLoad.carriedByMaterial(player), HeavyLoad.capacity(player));
		shownTooltip = lines;
		graphics.setComponentTooltipForNextFrame(Screens.getFont(screen), lines, mouseX, mouseY);
	}

	/**
	 * The load bar's tooltip, from the same per-material units the bar is drawn from: the total out
	 * of the allowance (rounded up, so it never shows room that is not there; red at the cap), then
	 * each carried material in bar order with a swatch of its colour (rounded to the nearest 0.01).
	 */
	static List<Component> tooltipLines(long[] units) {
		return tooltipLines(units, HeavyLoad.CAPACITY);
	}

	static List<Component> tooltipLines(long[] units, int capacity) {
		long total = Arrays.stream(units).sum();
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("simple_block.inventory.load", ingots(total, RoundingMode.CEILING), capacity / HeavyLoad.UNITS_PER_INGOT)
			.withColor(total >= capacity ? 0xFFFF5555 : 0xFFFFFFFF));
		for (int i = 0; i < ORDER.length; i++) {
			long amount = units[ORDER[i].ordinal()];
			if (amount > 0) {
				lines.add(Component.literal("\u25A0 ").withColor(COLOURS[i]).append(Component.translatable("simple_block.inventory.load.material",
					Component.translatable("simple_block.inventory.load." + ORDER[i].key()), ingots(amount, RoundingMode.HALF_UP))
					.withColor(0xFFAAAAAA)));
			}
		}
		if (total >= capacity) {
			lines.add(Component.translatable("simple_block.inventory.load.full").withColor(0xFFAAAAAA));
		}
		return lines;
	}

	/** Units of 1/216 ingot as ingots with up to two decimals, e.g. "6.44". */
	static String ingots(long units, RoundingMode rounding) {
		return BigDecimal.valueOf(units).divide(BigDecimal.valueOf(HeavyLoad.UNITS_PER_INGOT), 2, rounding).stripTrailingZeros().toPlainString();
	}

	static int[] segmentWidths(long[] units) {
		return segmentWidths(units, HeavyLoad.CAPACITY);
	}

	static int[] segmentWidths(long[] units, int capacity) {
		double scale = (double) BAR_WIDTH / Math.max(capacity, Arrays.stream(units).sum());
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
