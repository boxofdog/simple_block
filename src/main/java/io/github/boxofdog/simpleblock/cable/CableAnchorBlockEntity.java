package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/** Which drum's cable runs through this anchor, if any. An anchor carries one cable. */
public class CableAnchorBlockEntity extends BlockEntity {
	private @Nullable BlockPos drum;

	public CableAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(CableContent.CABLE_ANCHOR_ENTITY, pos, state);
	}

	public @Nullable BlockPos drum() {
		return this.drum;
	}

	public void setDrum(@Nullable BlockPos drum) {
		this.drum = drum == null ? null : drum.immutable();
		this.setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.storeNullable("drum", BlockPos.CODEC, this.drum);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.drum = input.read("drum", BlockPos.CODEC).orElse(null);
	}

	/** The anchor is going (broken, exploded, support removed): the cable is cut back to the support before it. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.drum != null && this.level instanceof ServerLevel level) {
			CableRoutes.anchorRemoved(level, this.drum, pos);
		}
	}
}
