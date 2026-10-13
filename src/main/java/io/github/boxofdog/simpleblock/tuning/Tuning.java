package io.github.boxofdog.simpleblock.tuning;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.fabricmc.loader.api.FabricLoader;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * Central table of the provisional balance numbers (Milestone 02).
 *
 * <p>Only values the design documents call tunable live here. Locked values (torch 12, lantern 15,
 * 20-block attraction radius, 27 slots, 64-ingot heavy cap, Hard door-break pace, 50% exhaustion)
 * are constants in their features and are deliberately not configurable.
 *
 * <p>Values are read once at startup. {@code config/simple_block/tuning-defaults.json} is rewritten
 * every start with every key and its default; to change a value, copy its line into
 * {@code config/simple_block/tuning.json} (overrides only). Worldgen values only affect chunks
 * generated after a restart.
 */
public final class Tuning {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();
	private static final Map<String, Object> CONFIGURED = new ConcurrentHashMap<>();
	private static final Map<String, Object> VALUES = new ConcurrentHashMap<>();

	/** Validating codec for a tuning key referenced from data (worldgen JSON). */
	public static final Codec<String> KEY_CODEC = Codec.STRING.validate(
		key -> DEFAULTS.containsKey(key) ? DataResult.success(key) : DataResult.error(() -> "Unknown simple_block tuning key: " + key));

	/**
	 * Codec for a key prefix referenced from data, checking that {@code prefix.suffix} exists for
	 * every given suffix.
	 */
	public static Codec<String> prefixCodec(String... suffixes) {
		return Codec.STRING.validate(prefix -> {
			for (String suffix : suffixes) {
				if (!DEFAULTS.containsKey(prefix + "." + suffix)) {
					return DataResult.error(() -> "Unknown simple_block tuning key: " + prefix + "." + suffix);
				}
			}
			return DataResult.success(prefix);
		});
	}

	static {
		// ORE-02: share of vanilla diamond placements kept (target 0.35, accepted range 0.30-0.40).
		define("ore.diamond.keep_chance", 0.35);

		// ORE-01 background iron in ordinary stone. "veins_per_64_layers" is the mean number of
		// vein attempts per chunk for every 64 layers of the y range; attempts above the local
		// terrain surface are dropped, so density per block of rock is constant across terrain.
		define("ore.iron.stone.veins_per_64_layers", 7.5);
		define("ore.iron.stone.min_y", 0);
		define("ore.iron.stone.max_y", 320);
		define("ore.iron.stone.size", 7);

		// Background iron in ordinary deepslate (all Overworld biomes).
		define("ore.iron.deepslate.veins_per_64_layers", 8.0);
		define("ore.iron.deepslate.min_y", -64);
		define("ore.iron.deepslate.max_y", 4);
		define("ore.iron.deepslate.size", 9);

		// Extra background iron in deepslate under deep ocean (biome tag simple_block:deep_ocean_iron).
		define("ore.iron.deep_ocean.veins_per_64_layers", 8.0);
		define("ore.iron.deep_ocean.min_y", -64);
		define("ore.iron.deep_ocean.max_y", 4);
		define("ore.iron.deep_ocean.size", 12);
		// Large clustered ore bodies under deep ocean.
		define("ore.iron.deep_ocean_body.chance_per_chunk", 0.08);
		define("ore.iron.deep_ocean_body.min_y", -60);
		define("ore.iron.deep_ocean_body.max_y", -8);
		define("ore.iron.deep_ocean_body.veins", 6);
		define("ore.iron.deep_ocean_body.spread", 6);
		define("ore.iron.deep_ocean_body.size", 16);

		// Mountain banded iron formations (biome tag simple_block:banded_iron_deposits).
		define("ore.iron.banded.chance_per_chunk", 0.12);
		define("ore.iron.banded.min_surface_y", 90);
		define("ore.iron.banded.min_depth", 4);
		define("ore.iron.banded.max_depth", 36);
		define("ore.iron.banded.half_length", 16);
		define("ore.iron.banded.half_width", 8);
		define("ore.iron.banded.layers", 3);
		define("ore.iron.banded.layer_spacing", 3);
		define("ore.iron.banded.max_dip", 0.25);
		define("ore.iron.banded.density", 0.6);

		// Alluvial/placer deposits (biome tag simple_block:placer_iron_deposits).
		define("ore.iron.placer.attempts_per_chunk", 2.0);
		define("ore.iron.placer.water_search_radius", 5);
		define("ore.iron.placer.depression_radius", 8);
		define("ore.iron.placer.depression_depth", 3);
		define("ore.iron.placer.radius", 4);
		define("ore.iron.placer.max_depth", 4);
		define("ore.iron.placer.density", 0.45);

		// Geothermal pockets beside natural lava (all Overworld biomes).
		define("ore.iron.geothermal.attempts_per_chunk", 3.0);
		define("ore.iron.geothermal.min_y", -58);
		define("ore.iron.geothermal.max_y", 40);
		define("ore.iron.geothermal.search_radius", 6);
		define("ore.iron.geothermal.selected_chance", 0.5);
		define("ore.iron.geothermal.radius", 3);
		define("ore.iron.geothermal.density", 0.45);

		// MOB-01 entrancement (the 20-block detection radius is locked, see feature/Entrancement).
		// "enabled" exists for negative-control tests; it is not a gameplay option.
		define("mob.entrancement.enabled", true);
		// Random positions in the 20-block sphere checked for block light at each wander decision.
		define("mob.entrancement.samples", 48);
		// Lit spots a mob remembers between decisions (spreads mobs over a lit area).
		define("mob.entrancement.memory", 8);
		// Chance per wander decision to ignore the light and wander aimlessly.
		define("mob.entrancement.lose_interest_chance", 0.15);
		// How far one wander step may go towards a lit spot, in blocks.
		define("mob.entrancement.step", 10);

		// MOB-03: how long a creeper smokes in direct sunlight before it is gone (ticks; 0.3-0.6 s target).
		define("mob.creeper.evaporation_ticks", 10);

		// M04 cable (provisional; see docs/DECISIONS.md). M06: one coil equips a drum for any route length,
		// so there is no drum capacity any more.
		// Longest straight span between two supports (drum or anchor), in blocks.
		define("cable.max_span", 12);
		// Height of the cable above a minecart (its position on the rail) that a connecting rod grips.
		define("cable.rod_reach_min", 1.5);
		define("cable.rod_reach_max", 3.0);
		// M05 cable loops (provisional). Working-strand speed while the crank is held, blocks per tick
		// (0.06 = 1.2 blocks a second on the level).
		define("cable.crank_speed", 0.06);
		// Extra effort per unit of hauled mass on a rail climb (vanilla slopes: one up per one along):
		// speed = crank speed / (1 + grade_load x mass climbing).
		define("cable.grade_load", 1.0);
		// How close to the drum's exit or the return wheel (along the track) a hauled cart stops.
		define("cable.end_clearance", 1.5);
		// M06 coal winch (provisional). Working-strand speed with the lever at PULL or FEED, blocks per
		// tick (0.15 = 3 blocks a second, 2.5x the hand crank), before the uphill load.
		define("winch.powered_speed", 0.15);
		// How much the powered speed may change per tick (smooth starts and PULL-to-FEED reversals).
		define("winch.acceleration", 0.01);
		// Ticks of actual hauling one coal gives (a furnace smelts for 1600 ticks on one coal): 1600 ticks
		// at full speed is 240 blocks of haulage per coal.
		define("winch.burn_ticks_per_coal", 1600);
		// Coal a winch holds (one is put in per right-click).
		define("winch.coal_capacity", 64);

		// M05 rail mass (provisional), in empty-minecart units: an empty cart, a full load of cargo on
		// top (scaled by the load), a rider, a Cobblestone Engine.
		define("rail.cart_mass", 1.0);
		define("rail.cargo_mass", 2.0);
		define("rail.passenger_mass", 0.5);
		define("train.engine_mass", 2.0);
		// Cobblestone Engine (provisional), in blocks per tick per tick and mass units: driving force,
		// the pull of a 45-degree rail on each unit of mass, rolling resistance, braking, top speed.
		define("train.engine_force", 0.02);
		define("train.grade_force", 0.0078);
		define("train.rolling_resistance", 0.0005);
		define("train.brake", 0.02);
		define("train.max_speed", 0.2);
	}

	private Tuning() {
	}

	private static void define(String key, Object defaultValue) {
		DEFAULTS.put(key, defaultValue);
		CONFIGURED.put(key, defaultValue);
		VALUES.put(key, defaultValue);
	}

	public static Map<String, Object> defaults() {
		return Collections.unmodifiableMap(DEFAULTS);
	}

	public static double getDouble(String key) {
		return ((Number) value(key)).doubleValue();
	}

	public static int getInt(String key) {
		return (int) Math.round(((Number) value(key)).doubleValue());
	}

	public static boolean getBoolean(String key) {
		return (Boolean) value(key);
	}

	/** Overrides a value for the running game only (used by tests and negative controls). */
	public static void set(String key, Object value) {
		Object current = value(key);
		if (current instanceof Boolean != value instanceof Boolean) {
			throw new IllegalArgumentException("Wrong type for tuning key " + key + ": " + value);
		}
		VALUES.put(key, value);
	}

	/** Undoes {@link #set}, restoring the value from the tuning file. */
	public static void reset(String key) {
		value(key);
		VALUES.put(key, CONFIGURED.get(key));
	}

	private static Object value(String key) {
		Object value = VALUES.get(key);
		if (value == null) {
			throw new IllegalArgumentException("Unknown simple_block tuning key: " + key);
		}
		return value;
	}

	public static void load() {
		Path dir = FabricLoader.getInstance().getConfigDir().resolve(SimpleBlock.MOD_ID);
		load(dir.resolve("tuning.json"), dir.resolve("tuning-defaults.json"));
	}

	/**
	 * Reads overrides from {@code overridesFile} (created as an empty object if missing) and writes
	 * every key with its current default to {@code defaultsFile} for reference. Only keys present in
	 * the overrides file change anything, so a later build's retuned defaults still take effect
	 * for every key the player has not set.
	 */
	static void load(Path overridesFile, Path defaultsFile) {
		JsonObject defaults = new JsonObject();
		DEFAULTS.forEach((key, value) -> defaults.add(key, toJson(value)));
		write(defaultsFile, defaults);

		if (!Files.exists(overridesFile)) {
			write(overridesFile, new JsonObject());
			SimpleBlock.LOGGER.info("Tuning: {} defaults (see {}), no overrides", DEFAULTS.size(), defaultsFile);
			return;
		}
		JsonObject overrides;
		try (Reader reader = Files.newBufferedReader(overridesFile, StandardCharsets.UTF_8)) {
			overrides = JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			SimpleBlock.LOGGER.error("Could not read {}; using default tuning values", overridesFile, e);
			return;
		}

		int applied = 0;
		for (String key : overrides.keySet()) {
			Object defaultValue = DEFAULTS.get(key);
			JsonElement element = overrides.get(key);
			if (defaultValue == null) {
				SimpleBlock.LOGGER.warn("Tuning: ignoring unknown key {} in {}", key, overridesFile);
				continue;
			}
			JsonPrimitive primitive = element.isJsonPrimitive() ? element.getAsJsonPrimitive() : null;
			Object value = null;
			if (primitive != null && defaultValue instanceof Boolean && primitive.isBoolean()) {
				value = primitive.getAsBoolean();
			} else if (primitive != null && defaultValue instanceof Number && primitive.isNumber()) {
				value = primitive.getAsDouble();
			}
			if (value == null) {
				SimpleBlock.LOGGER.warn("Tuning: ignoring {} = {}: expected a {}", key, element,
					defaultValue instanceof Boolean ? "boolean" : "number");
				continue;
			}
			CONFIGURED.put(key, value);
			VALUES.put(key, value);
			SimpleBlock.LOGGER.info("Tuning: {} = {} (default {})", key, value, defaultValue);
			applied++;
		}
		SimpleBlock.LOGGER.info("Tuning: {} defaults (see {}), {} overrides from {}", DEFAULTS.size(), defaultsFile, applied, overridesFile);
	}

	private static void write(Path file, JsonObject json) {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			SimpleBlock.LOGGER.warn("Could not write {}", file, e);
		}
	}

	private static JsonPrimitive toJson(Object value) {
		return value instanceof Boolean b ? new JsonPrimitive(b) : new JsonPrimitive((Number) value);
	}
}
