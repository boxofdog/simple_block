package io.github.boxofdog.simpleblock.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * ORE-01 geothermal pocket: an irregular, rich blob of iron ore in the rock wall beside natural
 * underground lava (lava lakes and the lava sea of deep caves). The feature runs once during world
 * generation, so player-placed lava never produces ore and nothing happens per tick.
 *
 * <p>From its start position it looks for lava within {@code key.search_radius} blocks. Only
 * {@code key.selected_chance} of the lava found gets a pocket, so not every lava body is ringed with
 * iron. The pocket is centred in the rock next to the lava, radius {@code key.radius}, filled to
 * {@code key.density}.
 */
public final class GeothermalIronFeature implements Feature {
	public static final MapCodec<GeothermalIronFeature> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(Tuning.prefixCodec("search_radius", "selected_chance", "radius", "density").fieldOf("key").forGetter(f -> f.key))
			.apply(i, GeothermalIronFeature::new));

	private static final Direction[] DIRECTIONS = Direction.values();

	private final String key;

	public GeothermalIronFeature(String key) {
		this.key = key;
	}

	@Override
	public MapCodec<GeothermalIronFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		BlockPos lava = this.findLava(level, origin);
		if (lava == null || random.nextDouble() >= Tuning.getDouble(this.key + ".selected_chance")) {
			return false;
		}
		int radius = Math.clamp(Tuning.getInt(this.key + ".radius"), 1, 5);
		double density = Tuning.getDouble(this.key + ".density");

		// Centre the pocket inside the rock wall that touches the lava.
		BlockPos center = null;
		int start = random.nextInt(DIRECTIONS.length);
		for (int i = 0; i < DIRECTIONS.length && center == null; i++) {
			BlockPos candidate = lava.relative(DIRECTIONS[(start + i) % DIRECTIONS.length], radius);
			if (IronDeposits.oreInRock(level.getBlockState(candidate)) != null) {
				center = candidate;
			}
		}
		if (center == null) {
			return false;
		}

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int placed = 0;
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dy = -radius - 1; dy <= radius + 1; dy++) {
				for (int dz = -radius - 1; dz <= radius + 1; dz++) {
					double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) + random.nextDouble() - 0.5;
					if (distance > radius || random.nextDouble() >= density) {
						continue;
					}
					pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (level.isOutsideBuildHeight(pos.getY()) || !IronDeposits.inWriteArea(origin, pos)) {
						continue;
					}
					BlockState ore = IronDeposits.oreInRock(level.getBlockState(pos));
					if (ore != null) {
						level.setBlock(pos, ore, 2);
						placed++;
					}
				}
			}
		}
		return placed > 0;
	}

	private BlockPos findLava(WorldGenLevel level, BlockPos origin) {
		int radius = Math.clamp(Tuning.getInt(this.key + ".search_radius"), 1, 8);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dy = -radius; dy <= radius; dy += 2) {
			for (int dx = -radius; dx <= radius; dx += 2) {
				for (int dz = -radius; dz <= radius; dz += 2) {
					pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					if (!level.isOutsideBuildHeight(pos.getY()) && level.getFluidState(pos).is(FluidTags.LAVA)) {
						return pos.immutable();
					}
				}
			}
		}
		return null;
	}
}
