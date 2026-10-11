package io.github.boxofdog.simpleblock.worldgen;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * Placement modifiers whose numbers come from the {@link Tuning} table, so the worldgen JSON says
 * <em>what</em> is placed and the tuning file says <em>how much</em>.
 */
public final class TunedPlacements {
	private TunedPlacements() {
	}

	/** Rounds a mean count to an integer whose expected value is that mean. */
	static int stochasticRound(RandomSource random, double mean) {
		int whole = (int) Math.floor(mean);
		return whole + (random.nextDouble() < mean - whole ? 1 : 0);
	}

	/** {@code count}, but the mean comes from tuning key {@code key} and may be fractional. */
	public record Count(String key) implements PlacementModifier {
		public static final MapCodec<Count> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Tuning.KEY_CODEC.fieldOf("key").forGetter(Count::key)).apply(i, Count::new));

		@Override
		public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
			int count = stochasticRound(random, Tuning.getDouble(this.key));
			for (int i = 0; i < count; i++) {
				output.accept(origin);
			}
		}

		@Override
		public MapCodec<Count> codec() {
			return CODEC;
		}
	}

	/** Keeps each position with the probability in tuning key {@code key}. */
	public record Chance(String key) implements PlacementFilter {
		public static final MapCodec<Chance> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Tuning.KEY_CODEC.fieldOf("key").forGetter(Chance::key)).apply(i, Chance::new));

		@Override
		public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
			return random.nextDouble() < Tuning.getDouble(this.key);
		}

		@Override
		public MapCodec<Chance> codec() {
			return CODEC;
		}
	}

	/** Uniform height between tuning keys {@code key.min_y} and {@code key.max_y}. */
	public record Height(String key) implements PlacementModifier {
		public static final MapCodec<Height> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Tuning.prefixCodec("min_y", "max_y").fieldOf("key").forGetter(Height::key)).apply(i, Height::new));

		@Override
		public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
			int minY = Tuning.getInt(this.key + ".min_y");
			int maxY = Math.max(minY, Tuning.getInt(this.key + ".max_y"));
			output.accept(origin.atY(minY + random.nextInt(maxY - minY + 1)));
		}

		@Override
		public MapCodec<Height> codec() {
			return CODEC;
		}
	}

	/**
	 * Spreads ore attempts evenly through the rock of a y range: on average
	 * {@code key.veins_per_64_layers} attempts per chunk for every 64 layers between {@code key.min_y}
	 * and {@code key.max_y}, at random x/z in the chunk and uniform y, dropping attempts that fall
	 * above the local terrain. The result is the same density per block of rock in plains and in
	 * mountains, which is what ORE-01 compares. Replaces {@code count}, {@code in_square} and
	 * {@code height_range}.
	 */
	public record Volume(String key) implements PlacementModifier {
		public static final MapCodec<Volume> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Tuning.prefixCodec("veins_per_64_layers", "min_y", "max_y").fieldOf("key").forGetter(Volume::key))
				.apply(i, Volume::new));

		@Override
		public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
			int minY = Math.max(Tuning.getInt(this.key + ".min_y"), context.getMinGenY());
			int maxY = Math.min(Tuning.getInt(this.key + ".max_y"), context.getMinGenY() + context.getGenDepth() - 1);
			if (maxY < minY) {
				return;
			}
			int layers = maxY - minY + 1;
			int count = stochasticRound(random, Tuning.getDouble(this.key + ".veins_per_64_layers") * layers / 64.0);
			for (int i = 0; i < count; i++) {
				int x = origin.getX() + random.nextInt(16);
				int z = origin.getZ() + random.nextInt(16);
				int y = minY + random.nextInt(layers);
				if (y < context.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)) {
					output.accept(new BlockPos(x, y, z));
				}
			}
		}

		@Override
		public MapCodec<Volume> codec() {
			return CODEC;
		}
	}

	/** Emits {@code key.veins} positions within {@code key.spread} blocks of the incoming position. */
	public record Cluster(String key) implements PlacementModifier {
		public static final MapCodec<Cluster> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(Tuning.prefixCodec("veins", "spread").fieldOf("key").forGetter(Cluster::key)).apply(i, Cluster::new));

		@Override
		public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
			int veins = Tuning.getInt(this.key + ".veins");
			int spread = Math.max(0, Tuning.getInt(this.key + ".spread"));
			for (int i = 0; i < veins; i++) {
				output.accept(origin.offset(
					random.nextInt(2 * spread + 1) - spread,
					random.nextInt(spread + 1) - spread / 2,
					random.nextInt(2 * spread + 1) - spread));
			}
		}

		@Override
		public MapCodec<Cluster> codec() {
			return CODEC;
		}
	}

	/**
	 * Keeps positions whose <em>surface</em> biome (the biome at the top of the terrain or water
	 * column) is in the tag. Deposits tied to a landscape, such as iron under deep ocean, use this
	 * instead of {@code minecraft:biome}, which looks at the 3D biome at the ore's own depth.
	 */
	public record SurfaceBiome(TagKey<Biome> biomes) implements PlacementFilter {
		public static final MapCodec<SurfaceBiome> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(TagKey.codec(Registries.BIOME).fieldOf("biomes").forGetter(SurfaceBiome::biomes)).apply(i, SurfaceBiome::new));

		@Override
		public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
			int surface = context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
			return context.getLevel().getBiome(new BlockPos(origin.getX(), surface, origin.getZ())).is(this.biomes);
		}

		@Override
		public MapCodec<SurfaceBiome> codec() {
			return CODEC;
		}
	}
}
