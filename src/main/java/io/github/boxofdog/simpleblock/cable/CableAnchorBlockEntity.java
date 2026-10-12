package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Which drum's cable runs through this anchor, if any (an anchor carries one cable), and, on the last
 * anchor of a closed loop (M05), the copper anchor fitted as its return wheel. Sent to clients, which
 * need it to tell the last anchor of a route.
 */
public class CableAnchorBlockEntity extends BlockEntity {
	private @Nullable BlockPos drum;
	private boolean wheel;

	public CableAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(CableContent.CABLE_ANCHOR_ENTITY, pos, state);
	}

	public @Nullable BlockPos drum() {
		return this.drum;
	}

	public void setDrum(@Nullable BlockPos drum) {
		this.drum = drum == null ? null : drum.immutable();
		this.changed();
	}

	/** Whether a copper anchor is fitted here as a loop's return wheel (it comes back when the loop opens). */
	public boolean wheel() {
		return this.wheel;
	}

	public void setWheel(boolean wheel) {
		this.wheel = wheel;
		this.changed();
	}

	private void changed() {
		this.setChanged();
		if (this.level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.storeNullable("drum", BlockPos.CODEC, this.drum);
		if (this.wheel) {
			output.putBoolean("wheel", true);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.drum = input.read("drum", BlockPos.CODEC).orElse(null);
		this.wheel = input.getBooleanOr("wheel", false);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		this.saveAdditional(output);
		return output.buildResult();
	}

	/**
	 * The anchor is going (broken, exploded, support removed): a fitted return wheel's anchor drops with
	 * it, a loop through it opens and the cable is cut back to the support before it.
	 */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level instanceof ServerLevel level) {
			if (this.wheel) {
				this.wheel = false;
				Block.popResource(level, pos, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
			}
			if (this.drum != null) {
				CableRoutes.anchorRemoved(level, this.drum, pos);
			}
		}
	}
}
