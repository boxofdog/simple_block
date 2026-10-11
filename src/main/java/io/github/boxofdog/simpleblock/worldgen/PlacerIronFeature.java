package io.github.boxofdog.simpleblock.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * ORE-01 alluvial (placer) deposit: a shallow, patchy pocket of iron ore one to a few blocks
 * under sand, red sand, gravel, clay or terracotta, where heavy minerals would wash together.
 * A spot qualifies only if it is a characteristic landform:
 * <ul>
 *   <li>a bank or bed of water (water within {@code key.water_search_radius} blocks at the
 *       surface), i.e. riverbanks, river beds and oasis shores; or</li>
 *   <li>the floor of a dry wash or canyon: at least {@code key.depression_depth} blocks below the
 *       average terrain {@code key.depression_radius} blocks around it.</li>
 * </ul>
 * Open, flat desert sand does not qualify, so this is not a buff to every desert block.
 */
public final class PlacerIronFeature implements Feature {
	public static final MapCodec<PlacerIronFeature> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(Tuning.prefixCodec("water_search_radius", "depression_radius", "depression_depth", "radius", "max_depth", "density")
				.fieldOf("key").forGetter(f -> f.key))
			.apply(i, PlacerIronFeature::new));

	private final String key;

	public PlacerIronFeature(String key) {
		this.key = key;
	}

	@Override
	public MapCodec<PlacerIronFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int x = origin.getX();
		int z = origin.getZ();
		int topY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
		BlockPos top = new BlockPos(x, topY, z);
		if (!IronDeposits.isSediment(level.getBlockState(top))) {
			return false;
		}
		if (!this.besideWater(level, top) && !this.inDepression(level, x, z, topY)) {
			return false;
		}

		// Stays within the chunk and one block row of each neighbour, inside the feature write area.
		int radius = Math.clamp(Tuning.getInt(this.key + ".radius"), 1, 12);
		int maxDepth = Math.max(1, Tuning.getInt(this.key + ".max_depth"));
		double density = Tuning.getDouble(this.key + ".density");
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int placed = 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				// Irregular outline: each column gets its own reach.
				double reach = (radius + 0.5) * (0.6 + 0.4 * random.nextDouble());
				if (dx * dx + dz * dz > reach * reach) {
					continue;
				}
				int columnTop = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + dx, z + dz) - 1;
				if (Math.abs(columnTop - topY) > 2) {
					continue;
				}
				for (int depth = 1; depth <= maxDepth; depth++) {
					if (random.nextDouble() >= density) {
						continue;
					}
					pos.set(x + dx, columnTop - depth, z + dz);
					BlockState ore = IronDeposits.oreInSediment(level.getBlockState(pos));
					if (ore != null) {
						level.setBlock(pos, ore, 2);
						placed++;
					}
				}
			}
		}
		return placed > 0;
	}

	private boolean besideWater(WorldGenLevel level, BlockPos top) {
		int radius = Math.clamp(Tuning.getInt(this.key + ".water_search_radius"), 0, 12);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dy = 0; dy <= 2; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					pos.set(top.getX() + dx, top.getY() + dy, top.getZ() + dz);
					if (level.getFluidState(pos).is(FluidTags.WATER)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean inDepression(WorldGenLevel level, int x, int z, int topY) {
		int radius = Math.clamp(Tuning.getInt(this.key + ".depression_radius"), 1, 15);
		double total = 0.0;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0;
			total += level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,
				x + Mth.floor(Math.cos(angle) * radius), z + Mth.floor(Math.sin(angle) * radius)) - 1;
		}
		return total / 8.0 - topY >= Tuning.getInt(this.key + ".depression_depth");
	}
}
