package io.github.boxofdog.simpleblock.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/** A vanilla ore blob ({@code minecraft:ore}) whose vein size is tuning key {@code key.size}. */
public final class TunedOreFeature implements Feature {
	public static final MapCodec<TunedOreFeature> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(
				Tuning.prefixCodec("size").fieldOf("key").forGetter(f -> f.key),
				Codec.list(BlockReplacement.CODEC).fieldOf("targets").forGetter(f -> f.targets))
			.apply(i, TunedOreFeature::new));

	private final String key;
	private final List<BlockReplacement> targets;
	private volatile OreFeature delegate;

	public TunedOreFeature(String key, List<BlockReplacement> targets) {
		this.key = key;
		this.targets = targets;
	}

	@Override
	public MapCodec<TunedOreFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int size = Math.clamp(Tuning.getInt(this.key + ".size"), 1, 64);
		OreFeature ore = this.delegate;
		if (ore == null || ore.size() != size) {
			ore = new OreFeature(this.targets, size);
			this.delegate = ore;
		}
		return ore.place(level, chunkGenerator, random, origin);
	}
}
