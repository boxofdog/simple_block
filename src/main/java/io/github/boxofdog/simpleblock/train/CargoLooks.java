package io.github.boxofdog.simpleblock.train;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * M11: what a freight cart's cargo looks like in it, worked out only from the cart's synced cargo item
 * and count ({@link MinecartCargo.Cart}); nothing else is stored or sent.
 *
 * <p>Each cargo item has a {@link Look}: a material (its own texture) in one of five {@link Form}s, the
 * shape of the load: bars, loose gems, stacked storage blocks, rough rocks, or a loose heap. An item
 * with no look of its own (only possible if the cargo tag grows) shows as {@link Look#COVERED}, a load
 * under a tarpaulin, which names no material. The load fills the cart in four rising {@linkplain #band
 * bands}: 1-64, 65-128, 129-192 and 193-256 items; an empty cart shows nothing.
 */
public final class CargoLooks {
	/** The shape of a load. */
	public enum Form {
		INGOTS, GEMS, BLOCKS, ROCKS, HEAP, COVERED
	}

	/** A cargo's look: its material and form, and the cargo item it is for (null for the fallback). */
	public enum Look implements StringRepresentable {
		IRON_INGOT(Form.INGOTS, "iron_ingot"),
		COPPER_INGOT(Form.INGOTS, "copper_ingot"),
		GOLD_INGOT(Form.INGOTS, "gold_ingot"),
		NETHERITE_INGOT(Form.INGOTS, "netherite_ingot"),
		DIAMOND(Form.GEMS, "diamond"),
		EMERALD(Form.GEMS, "emerald"),
		LAPIS_LAZULI(Form.GEMS, "lapis_lazuli"),
		IRON_BLOCK(Form.BLOCKS, "iron_block"),
		COPPER_BLOCK(Form.BLOCKS, "copper_block"),
		GOLD_BLOCK(Form.BLOCKS, "gold_block"),
		DIAMOND_BLOCK(Form.BLOCKS, "diamond_block"),
		NETHERITE_BLOCK(Form.BLOCKS, "netherite_block"),
		STONE(Form.BLOCKS, "stone"),
		DEEPSLATE(Form.BLOCKS, "deepslate"),
		IRON_ORE(Form.ROCKS, "iron_ore"),
		COPPER_ORE(Form.ROCKS, "copper_ore"),
		GOLD_ORE(Form.ROCKS, "gold_ore"),
		DEEPSLATE_IRON_ORE(Form.ROCKS, "deepslate_iron_ore"),
		DEEPSLATE_COPPER_ORE(Form.ROCKS, "deepslate_copper_ore"),
		DEEPSLATE_GOLD_ORE(Form.ROCKS, "deepslate_gold_ore"),
		RAW_IRON(Form.ROCKS, "raw_iron"),
		RAW_COPPER(Form.ROCKS, "raw_copper"),
		RAW_GOLD(Form.ROCKS, "raw_gold"),
		COAL(Form.ROCKS, "coal"),
		SAND(Form.HEAP, "sand"),
		GRAVEL(Form.HEAP, "gravel"),
		COBBLESTONE(Form.HEAP, "cobblestone"),
		DIRT(Form.HEAP, "dirt"),
		COBBLED_DEEPSLATE(Form.HEAP, "cobbled_deepslate"),
		REDSTONE(Form.HEAP, "redstone"),
		COVERED(Form.COVERED, null);

		private final Form form;
		private final Identifier item;

		Look(Form form, String item) {
			this.form = form;
			this.item = item == null ? null : Identifier.withDefaultNamespace(item);
		}

		public Form form() {
			return this.form;
		}

		/** The vanilla cargo item this look is for, or null for {@link #COVERED}. */
		public Identifier item() {
			return this.item;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** Items in each band: 64, so the bands are 1-64, 65-128, 129-192, 193-256. */
	public static final int BAND_SIZE = MinecartCargo.CAPACITY / 4;

	private static final Map<Identifier, Look> BY_ITEM = new HashMap<>();

	static {
		for (Look look : Look.values()) {
			if (look.item != null) {
				BY_ITEM.put(look.item, look);
			}
		}
	}

	private CargoLooks() {
	}

	/** The look of a load of {@code item}: its own, or {@link Look#COVERED}. */
	public static Look look(Item item) {
		return BY_ITEM.getOrDefault(BuiltInRegistries.ITEM.getKey(item), Look.COVERED);
	}

	/** How full a cart with {@code count} items looks: 0 empty, else 1 to 4. */
	public static int band(int count) {
		return count <= 0 ? 0 : Math.min(4, (count - 1) / BAND_SIZE + 1);
	}
}
