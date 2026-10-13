package io.github.boxofdog.simpleblock.metallurgy;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.jspecify.annotations.Nullable;

/**
 * M08: newly generated village pieces (and trail ruins, {@link #appliesTo}) place an ordinary furnace
 * wherever their template has a vanilla blast furnace, facing the same way. Only the block changes; villager professions, job
 * sites and trades are not touched (deferred to the village backlog). Added to village pieces by
 * {@code mixin/metallurgy/SinglePoolElementMixin}; never serialized, so it needs no registered type.
 */
public final class VillageFurnaces implements StructureProcessor {
	public static final VillageFurnaces INSTANCE = new VillageFurnaces();
	private static final MapCodec<VillageFurnaces> CODEC = MapCodec.unit(INSTANCE);

	private VillageFurnaces() {
	}

	/**
	 * Whether a jigsaw template gets the swap: every village piece, and trail ruins, whose two large
	 * rooms are the only other vanilla structures with a blast furnace (mining one would otherwise still
	 * give the retired item).
	 */
	public static boolean appliesTo(String templatePath) {
		return templatePath.startsWith("village/") || templatePath.startsWith("trail_ruins/");
	}

	@Override
	public StructureTemplate.@Nullable StructureBlockInfo processBlock(LevelReader level, BlockPos targetPosition, BlockPos referencePos,
		BlockPos templateRelativePos, StructureTemplate.StructureBlockInfo processedBlockInfo, StructurePlaceSettings settings) {
		BlockState state = processedBlockInfo.state();
		if (!state.is(Blocks.BLAST_FURNACE)) {
			return processedBlockInfo;
		}
		BlockState furnace = Blocks.FURNACE.defaultBlockState()
			.setValue(AbstractFurnaceBlock.FACING, state.getValue(AbstractFurnaceBlock.FACING))
			.setValue(AbstractFurnaceBlock.LIT, state.getValue(AbstractFurnaceBlock.LIT));
		return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), furnace, null);
	}

	@Override
	public MapCodec<? extends StructureProcessor> codec() {
		return CODEC;
	}
}
