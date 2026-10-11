package io.github.boxofdog.simpleblock.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared rules for the ORE-01 iron deposits. */
final class IronDeposits {
	private static final BlockState IRON_ORE = Blocks.IRON_ORE.defaultBlockState();
	private static final BlockState DEEPSLATE_IRON_ORE = Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();

	private IronDeposits() {
	}

	/** Iron ore matching the host rock, or null when the block is not ordinary stone or deepslate. */
	static BlockState oreInRock(BlockState host) {
		if (host.is(BlockTags.STONE_ORE_REPLACEABLES)) {
			return IRON_ORE;
		}
		if (host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
			return DEEPSLATE_IRON_ORE;
		}
		return null;
	}

	/** Like {@link #oreInRock}, but also accepts the loose and soft sediments of placer deposits. */
	static BlockState oreInSediment(BlockState host) {
		if (isSediment(host)) {
			return IRON_ORE;
		}
		return oreInRock(host);
	}

	/** Features may only touch their own chunk and the eight around it. */
	static boolean inWriteArea(BlockPos origin, BlockPos pos) {
		return Math.abs(SectionPos.blockToSectionCoord(pos.getX()) - SectionPos.blockToSectionCoord(origin.getX())) <= 1
			&& Math.abs(SectionPos.blockToSectionCoord(pos.getZ()) - SectionPos.blockToSectionCoord(origin.getZ())) <= 1;
	}

	static boolean isSediment(BlockState state) {
		return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SANDSTONE)
			|| state.is(Blocks.RED_SANDSTONE) || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.COARSE_DIRT)
			|| state.is(Blocks.DIRT);
	}
}
