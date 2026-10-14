package io.github.boxofdog.simpleblock.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A Spelunker's Rope anchor: how many lengths of 12 have been paid for (the Spelunker's Rope's own, plus one per Rope). */
public class RopeAnchorBlockEntity extends BlockEntity {
	private int paid = 1;

	public RopeAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(RopeContent.ROPE_ANCHOR_ENTITY, pos, state);
	}

	public int paid() {
		return this.paid;
	}

	void addPaid() {
		this.paid++;
		this.setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("paid", this.paid);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.paid = Math.max(1, input.getIntOr("paid", 1));
	}

	/** The anchor is going (broken, support gone, exploded): the whole line comes down and drops here. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level instanceof ServerLevel serverLevel) {
			SpelunkerRopes.retrieve(serverLevel, pos, null, null, pos, this);
		}
	}
}
