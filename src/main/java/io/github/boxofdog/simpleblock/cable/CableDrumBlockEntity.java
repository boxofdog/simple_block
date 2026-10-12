package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.List;

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

/**
 * A drum's cable: how many blocks are still wound on it ({@link #stored}) and the anchors the laid-out
 * cable runs through, in order from the drum ({@link #route}). Each span's length is fixed by its
 * end points ({@link CableRoutes#spanLength}), so the cable laid out is always the sum over the route.
 * Saved with the block and sent to clients (for drawing the cable).
 */
public class CableDrumBlockEntity extends BlockEntity {
	private int stored;
	private final List<BlockPos> route = new ArrayList<>();
	/** The loose end's entity id on the server; not saved (the hook is put back after loading). */
	int hookId = -1;
	/** Whether the route has been checked against the world since loading (its anchors were all loaded). */
	boolean validated;
	/** Whether the route's spans are in {@link CableIndex} since loading. */
	boolean indexed;

	public CableDrumBlockEntity(BlockPos pos, BlockState state) {
		super(CableContent.CABLE_DRUM_ENTITY, pos, state);
	}

	public int stored() {
		return this.stored;
	}

	public List<BlockPos> route() {
		return List.copyOf(this.route);
	}

	/** The last support the cable runs through: the last anchor, or the drum itself. */
	public BlockPos routeEnd() {
		return this.route.isEmpty() ? this.getBlockPos() : this.route.getLast();
	}

	public boolean hasCable() {
		return this.stored > 0 || !this.route.isEmpty();
	}

	/** Cable laid out along the route, in blocks. */
	public int laidOut() {
		int total = 0;
		BlockPos previous = this.getBlockPos();
		boolean fromDrum = true;
		for (BlockPos anchor : this.route) {
			total += CableRoutes.spanLength(previous, fromDrum, anchor);
			previous = anchor;
			fromDrum = false;
		}
		return total;
	}

	void setStored(int stored) {
		this.stored = stored;
		this.changed();
	}

	void addAnchor(BlockPos anchor) {
		this.route.add(anchor.immutable());
		this.changed();
	}

	/** Removes the anchors from {@code index} on and returns them, in route order. */
	List<BlockPos> cutFrom(int index) {
		List<BlockPos> removed = new ArrayList<>(this.route.subList(index, this.route.size()));
		this.route.subList(index, this.route.size()).clear();
		this.changed();
		return removed;
	}

	/** Saves, updates clients and the span index, and shows the barrel wound or bare. */
	private void changed() {
		this.setChanged();
		if (this.level instanceof ServerLevel serverLevel) {
			BlockState state = this.getBlockState();
			if (state.hasProperty(CableDrumBlock.WOUND) && state.getValue(CableDrumBlock.WOUND) != this.hasCable()) {
				serverLevel.setBlock(this.getBlockPos(), state.setValue(CableDrumBlock.WOUND, this.hasCable()), Block.UPDATE_ALL);
			}
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
			CableIndex.update(serverLevel, this);
		}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CableDrumBlockEntity drum) {
		CableRoutes.tickDrum((ServerLevel) level, drum);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("cable", this.stored);
		output.store("route", BlockPos.CODEC.listOf(), List.copyOf(this.route));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.stored = Math.max(0, input.getIntOr("cable", 0));
		this.route.clear();
		this.route.addAll(input.read("route", BlockPos.CODEC.listOf()).orElse(List.of()));
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

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (this.level instanceof ServerLevel serverLevel) {
			CableIndex.remove(serverLevel, this.getBlockPos());
		}
	}

	/** The drum is going: its cable (wound and laid out) drops as coils and cables; the anchors are freed. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level instanceof ServerLevel serverLevel) {
			CableRoutes.drumRemoved(serverLevel, this);
		}
	}
}
