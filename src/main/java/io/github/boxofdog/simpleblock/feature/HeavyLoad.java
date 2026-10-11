package io.github.boxofdog.simpleblock.feature;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * INV-04: one shared carrying allowance of 64 ingot-equivalents for iron, gold, copper, diamond
 * and netherite. A diamond weighs like an ingot; netherite weighs like diamond (an ingot 1,
 * netherite gear what its diamond version weighs).
 *
 * <p>Weights come from {@code simple_block/heavy_weights.json} (ingot-equivalents per single item
 * and {@link Material}, fractions allowed), generated from the game's recipes by
 * {@code tools/heavy-weights} and reviewed. All accounting is in whole units of 1/216 ingot: rails (3/8) and hanging signs
 * (11/27) need eighths and twenty-sevenths, so 216 is the smallest unit that keeps every weight an
 * exact integer. The capacity is 64 ingots = 13,824 units (the same 576 ninths the design names).
 *
 * <p>What counts as carried: the 36 inventory slots (including any items in the disabled row),
 * the offhand, the item on the cursor, the player's own 2x2 crafting grid and the contents of
 * bundles in any of those. Worn armor does not count. Creative and spectator players are never
 * limited, and their excess is kept, not deleted, when they switch to survival: they just cannot
 * take in more heavy items until they are back under the cap.
 *
 * <p>Enforcement (all on the server) is in {@code mixin/inventory}: {@code Inventory.add} and
 * {@code placeItemBackInInventory} (pickups, commands, returning items when a screen closes),
 * shift-click transfers, cursor pickups, armor swaps, armor stands, and a safety net around every
 * container click.
 */
public final class HeavyLoad {
	public static final int UNITS_PER_INGOT = 216;
	public static final int CAPACITY_INGOTS = 64;
	public static final int CAPACITY = CAPACITY_INGOTS * UNITS_PER_INGOT;

	/** What carried weight is made of; the client's load bar shows one colour per material. */
	public enum Material {
		IRON("iron"), GOLD("gold"), COPPER("copper"), DIAMOND("diamond"), NETHERITE("netherite");

		private final String key;

		Material(String key) {
			this.key = key;
		}

		public String key() {
			return this.key;
		}
	}

	private static final Material[] MATERIALS = Material.values();
	private static final Map<Item, Integer> WEIGHTS = new IdentityHashMap<>();
	/** Per item, units of each material (indexed by {@link Material#ordinal()}); they add up to {@link #WEIGHTS}. */
	private static final Map<Item, int[]> COMPOSITION = new IdentityHashMap<>();
	private static final ThreadLocal<Integer> EXEMPT = ThreadLocal.withInitial(() -> 0);
	private static boolean loaded;

	private HeavyLoad() {
	}

	/** Loads the weight table; fails loudly on a malformed entry rather than guessing. */
	public static synchronized void load() {
		if (loaded) {
			return;
		}
		String path = "/" + SimpleBlock.MOD_ID + "/heavy_weights.json";
		try (InputStream in = HeavyLoad.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + path);
			}
			JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			int capacity = json.get("capacity_ingots").getAsInt();
			if (capacity != CAPACITY_INGOTS) {
				throw new IllegalStateException("heavy_weights.json capacity " + capacity + " != locked " + CAPACITY_INGOTS);
			}
			JsonObject weights = json.getAsJsonObject("weights");
			for (String key : weights.keySet()) {
				if (Arrays.stream(MATERIALS).noneMatch(material -> material.key().equals(key))) {
					throw new IllegalStateException("heavy_weights.json: unknown material " + key);
				}
			}
			for (Material material : MATERIALS) {
				JsonObject items = weights.getAsJsonObject(material.key());
				if (items == null) {
					continue;
				}
				for (String id : items.keySet()) {
					Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
					if (item == null) {
						SimpleBlock.LOGGER.warn("heavy_weights.json: unknown item {}", id);
						continue;
					}
					int units = parseUnits(id, items.get(id).getAsString());
					COMPOSITION.computeIfAbsent(item, i -> new int[MATERIALS.length])[material.ordinal()] += units;
					WEIGHTS.merge(item, units, Integer::sum);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
		loaded = true;
		SimpleBlock.LOGGER.info("Heavy load: {} weighted items, capacity {} ingots", WEIGHTS.size(), CAPACITY_INGOTS);
	}

	static int parseUnits(String id, String value) {
		String[] parts = value.trim().split("/");
		long numerator = Long.parseLong(parts[0].trim());
		long denominator = parts.length > 1 ? Long.parseLong(parts[1].trim()) : 1;
		long scaled = numerator * UNITS_PER_INGOT;
		if (denominator <= 0 || scaled % denominator != 0) {
			throw new IllegalStateException("heavy_weights.json: " + id + " = " + value + " is not a whole number of 1/" + UNITS_PER_INGOT + " ingot");
		}
		return (int) (scaled / denominator);
	}

	public static Map<Item, Integer> table() {
		load();
		return java.util.Collections.unmodifiableMap(WEIGHTS);
	}

	/** Weight of one item of this stack in 1/216 ingot, including what a bundle holds. */
	public static int unitWeight(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		load();
		int weight = WEIGHTS.getOrDefault(stack.getItem(), 0);
		BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (contents != null) {
			for (ItemStack inside : (Iterable<ItemStack>) contents.itemCopies()::iterator) {
				weight += weight(inside);
			}
		}
		return weight;
	}

	public static int weight(ItemStack stack) {
		return stack.isEmpty() ? 0 : unitWeight(stack) * stack.getCount();
	}

	/** Adds the weight of {@code stack} (bundle contents included) to {@code units}, per {@link Material}. */
	private static void addComposition(ItemStack stack, long[] units) {
		if (stack.isEmpty()) {
			return;
		}
		load();
		int[] composition = COMPOSITION.get(stack.getItem());
		if (composition != null) {
			for (int i = 0; i < composition.length; i++) {
				units[i] += (long) composition[i] * stack.getCount();
			}
		}
		BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (contents != null) {
			long[] inside = new long[MATERIALS.length];
			for (ItemStack item : (Iterable<ItemStack>) contents.itemCopies()::iterator) {
				addComposition(item, inside);
			}
			for (int i = 0; i < inside.length; i++) {
				units[i] += inside[i] * stack.getCount();
			}
		}
	}

	/** Units of each material in one item of this kind, indexed by {@link Material#ordinal()} (all zero if it weighs nothing). */
	public static int[] composition(Item item) {
		load();
		int[] composition = COMPOSITION.get(item);
		return composition == null ? new int[MATERIALS.length] : composition.clone();
	}

	public static boolean isHeavy(ItemStack stack) {
		return unitWeight(stack) > 0;
	}

	/** Creative and spectator players carry anything (QA). */
	public static boolean bypasses(Player player) {
		return player.isCreative() || player.isSpectator();
	}

	/** Everything the player carries, in 1/216 ingot. Worn armor is exempt. */
	public static int carried(Player player) {
		long total = 0;
		for (ItemStack stack : carriedStacks(player)) {
			total += weight(stack);
		}
		return (int) Math.min(Integer.MAX_VALUE, total);
	}

	/**
	 * What {@link #carried} is made of: units per {@link Material}, indexed by its ordinal. The
	 * entries add up to {@link #carried}.
	 */
	public static long[] carriedByMaterial(Player player) {
		long[] units = new long[MATERIALS.length];
		for (ItemStack stack : carriedStacks(player)) {
			addComposition(stack, units);
		}
		return units;
	}

	/** The stacks that count as carried: inventory, offhand, cursor(s) and the 2x2 crafting grid. */
	private static List<ItemStack> carriedStacks(Player player) {
		Inventory inventory = player.getInventory();
		List<ItemStack> stacks = new ArrayList<>(inventory.getNonEquipmentItems());
		stacks.add(player.getOffhandItem());
		stacks.add(player.containerMenu.getCarried());
		if (player.containerMenu != player.inventoryMenu) {
			stacks.add(player.inventoryMenu.getCarried());
		}
		CraftingContainer grid = player.inventoryMenu.getCraftSlots();
		for (int i = 0; i < grid.getContainerSize(); i++) {
			stacks.add(grid.getItem(i));
		}
		return stacks;
	}

	/** Room left under the cap; negative when over it. */
	public static int remaining(Player player) {
		return CAPACITY - carried(player);
	}

	/**
	 * How many items of {@code stack} the player may take in now. Exempt transfers (moving
	 * something that is already carried) and players who bypass the limit get the whole stack.
	 */
	public static int allowedCount(Player player, ItemStack stack) {
		if (stack.isEmpty() || isExempt() || bypasses(player)) {
			return stack.getCount();
		}
		int unit = unitWeight(stack);
		if (unit == 0) {
			return stack.getCount();
		}
		int room = remaining(player);
		return room <= 0 ? 0 : Math.min(stack.getCount(), room / unit);
	}

	/** Whether taking in {@code addedWeight} more is allowed (always, if it does not add weight). */
	public static boolean canTakeIn(Player player, int addedWeight) {
		return addedWeight <= 0 || isExempt() || bypasses(player) || addedWeight <= remaining(player);
	}

	/** Runs {@code action} as a transfer of items that are already carried (no limit applies). */
	public static <T> T exempt(Supplier<T> action) {
		EXEMPT.set(EXEMPT.get() + 1);
		try {
			return action.get();
		} finally {
			EXEMPT.set(EXEMPT.get() - 1);
		}
	}

	public static void exempt(Runnable action) {
		exempt(() -> {
			action.run();
			return null;
		});
	}

	public static boolean isExempt() {
		return EXEMPT.get() > 0;
	}

	/** Display text, e.g. "37.4 / 64". The load is rounded up, so the display never shows room that is not there. */
	public static String describe(int units) {
		double ingots = Math.ceil(units * 10.0 / UNITS_PER_INGOT) / 10.0;
		String amount = ingots == Math.floor(ingots) ? Integer.toString((int) ingots) : String.format(java.util.Locale.ROOT, "%.1f", ingots);
		return amount + " / " + CAPACITY_INGOTS;
	}
}
