package io.github.boxofdog.simpleblock.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * ORE-01 mountain deposit inspired by banded iron formations: a few thin, parallel, gently dipping
 * sheets of iron ore separated by plain stone, up to about two chunks long. The sheets start a few
 * blocks to a few dozen blocks under high terrain, so they run out of steep slopes and cliffs as
 * striped exposures. Only stone and deepslate are replaced.
 *
 * <p>Tuning keys under {@code key}: {@code min_surface_y}, {@code min_depth}, {@code max_depth},
 * {@code half_length}, {@code half_width}, {@code layers}, {@code layer_spacing}, {@code max_dip},
 * {@code density}.
 */
public final class BandedIronFeature implements Feature {
	public static final MapCodec<BandedIronFeature> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(Tuning.prefixCodec("min_surface_y", "min_depth", "max_depth", "half_length", "half_width", "layers",
				"layer_spacing", "max_dip", "density").fieldOf("key").forGetter(f -> f.key))
			.apply(i, BandedIronFeature::new));

	private final String key;

	public BandedIronFeature(String key) {
		this.key = key;
	}

	@Override
	public MapCodec<BandedIronFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
		if (surface < Tuning.getInt(this.key + ".min_surface_y")) {
			return false;
		}
		int minDepth = Tuning.getInt(this.key + ".min_depth");
		int maxDepth = Math.max(minDepth, Tuning.getInt(this.key + ".max_depth"));
		// Features may write to their own chunk and the eight around it.
		int halfLength = Math.clamp(Tuning.getInt(this.key + ".half_length"), 1, 24);
		int halfWidth = Math.clamp(Tuning.getInt(this.key + ".half_width"), 1, halfLength);
		int layers = Math.max(1, Tuning.getInt(this.key + ".layers"));
		int spacing = Math.max(1, Tuning.getInt(this.key + ".layer_spacing"));
		double maxDip = Tuning.getDouble(this.key + ".max_dip");
		double density = Tuning.getDouble(this.key + ".density");

		int centerY = surface - minDepth - random.nextInt(maxDepth - minDepth + 1);
		double angle = random.nextDouble() * Math.PI;
		double ux = Math.cos(angle);
		double uz = Math.sin(angle);
		double dipAlong = (random.nextDouble() * 2.0 - 1.0) * maxDip;
		double dipAcross = (random.nextDouble() * 2.0 - 1.0) * maxDip * 0.5;

		int chunkX = SectionPos.blockToSectionCoord(origin.getX());
		int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
		int minX = Math.max(origin.getX() - halfLength - 1, (chunkX - 1) * 16);
		int maxX = Math.min(origin.getX() + halfLength + 1, (chunkX + 2) * 16 - 1);
		int minZ = Math.max(origin.getZ() - halfLength - 1, (chunkZ - 1) * 16);
		int maxZ = Math.min(origin.getZ() + halfLength + 1, (chunkZ + 2) * 16 - 1);

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int placed = 0;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				double rx = x - origin.getX();
				double rz = z - origin.getZ();
				double along = rx * ux + rz * uz;
				double across = -rx * uz + rz * ux;
				double edge = Mth.square(along / (halfLength + 0.5)) + Mth.square(across / (halfWidth + 0.5));
				if (edge > 1.0) {
					continue;
				}
				double baseY = centerY + along * dipAlong + across * dipAcross;
				// Thinner towards the edges so the band frays out instead of ending in a wall.
				double localDensity = density * (1.0 - 0.5 * edge);
				for (int layer = 0; layer < layers; layer++) {
					if (random.nextDouble() >= localDensity) {
						continue;
					}
					pos.set(x, Mth.floor(baseY) + layer * spacing, z);
					if (level.isOutsideBuildHeight(pos.getY())) {
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
}
