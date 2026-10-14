package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
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
	/** How often a loaded anchor checks itself against its drum's line, in ticks. */
	private static final int RECONCILE_TICKS = 40;
	/** Whether this anchor has been checked against its drum's line since it loaded. */
	private boolean reconciled;

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
	 * After loading, then every {@value #RECONCILE_TICKS} ticks (M06): this anchor may have been
	 * unrouted, or its loop opened, while its chunk was not loaded or not ticking. Its drum's line
	 * (always available) says what it carries now: anything stale is cleared, without dropping anything
	 * (whoever opened the loop already gave the wheel's anchor back). A map lookup, so cheap.
	 */
	public static void serverTick(Level level, BlockPos pos, BlockState state, CableAnchorBlockEntity anchor) {
		if (level instanceof ServerLevel serverLevel
			&& (!anchor.reconciled || (level.getGameTime() + (pos.hashCode() & 0xFFFF)) % RECONCILE_TICKS == 0)) {
			anchor.reconcile(serverLevel);
		}
	}

	/** Clears what this anchor no longer carries, by its drum's line. */
	public void reconcile(ServerLevel level) {
		this.reconciled = true;
		BlockPos pos = this.getBlockPos();
		CableLines.Line line = this.drum == null ? null : CableLines.line(level, this.drum);
		boolean routed = line != null && line.anchors.contains(pos);
		boolean wheelHere = routed && line.looped && line.far == null && line.anchors.getLast().equals(pos);
		if (this.drum != null && !routed) {
			this.setDrum(null);
		}
		if (this.wheel && !wheelHere) {
			this.setWheel(false);
		}
		BlockState state = level.getBlockState(pos);
		boolean looped = routed && line.looped && line.far == null;
		// M07 playtest patch §4: the head anchor stays a pulley only while a skip hangs from it.
		boolean pulleyHere = routed && line.hoist != null && line.anchors.getLast().equals(pos);
		if (pulleyHere && state.hasProperty(CableAnchorBlock.RETURN) && state.getValue(CableAnchorBlock.RETURN) == CableAnchorBlock.Return.PULLEY) {
			return;
		}
		if (!looped && state.hasProperty(CableAnchorBlock.RETURN) && state.getValue(CableAnchorBlock.RETURN) != CableAnchorBlock.Return.NONE) {
			level.setBlock(pos, state.setValue(CableAnchorBlock.RETURN, CableAnchorBlock.Return.NONE), Block.UPDATE_ALL);
		}
	}

	/**
	 * The anchor is going (broken, exploded, support removed): a loop through it opens (the return
	 * wheel's anchor comes back: {@link CableLoops#open}, or {@link CableLines#cut} if the drum is
	 * unloaded) and the cable is cut back to the support before it.
	 */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level instanceof ServerLevel level && this.drum != null) {
			CableRoutes.anchorRemoved(level, this.drum, pos);
		}
	}
}
