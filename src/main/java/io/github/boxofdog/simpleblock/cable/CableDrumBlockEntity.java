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
import net.minecraft.world.phys.Vec3;

/**
 * A drum's cable: how many blocks are still wound on it ({@link #stored}) and the anchors the laid-out
 * cable runs through, in order from the drum ({@link #route}). Each span's length is fixed by its
 * end points ({@link CableRoutes#spanLength}), so the cable laid out is always the sum over the route.
 * With the route closed into a loop (M05, {@link CableLoops}), {@link #returnLength} more blocks form
 * the return strand. Wound, laid out and return together are always the coils wound on.
 *
 * <p>While a loop is driven ({@link CableDrive}), {@link #speed} is how fast the working strand moves
 * away from the drum (blocks per tick; negative: towards it). Saved with the block and sent to clients
 * (for drawing the cable and turning the spool).
 */
public class CableDrumBlockEntity extends BlockEntity {
	private int stored;
	private final List<BlockPos> route = new ArrayList<>();
	private boolean looped;
	private int returnLength;
	/** Working-strand speed, blocks per tick; not saved (a drum loads standing still). */
	private float speed;
	/** The speed clients last heard of. */
	private float sentSpeed;
	/** Distance the cable has travelled (client: turns the spool and moves the cable's texture). */
	public float travel;
	public float previousTravel;
	/** The loose end's entity id on the server; not saved (the hook is put back after loading). */
	int hookId = -1;
	/** Whether the route has been checked against the world since loading (its anchors were all loaded). */
	boolean validated;
	/** Whether the route's spans are in {@link CableIndex} since loading. */
	boolean indexed;
	/** Ticks the crank keeps turning after the last use (holding the use key repeats it), and which way. */
	int crankTicks;
	int crankDirection;
	/** Load reported by gripping carts this tick and the last ({@link CableDrive}). */
	double loadForward;
	double loadBackward;
	double lastLoadForward;
	double lastLoadBackward;

	public CableDrumBlockEntity(BlockPos pos, BlockState state) {
		super(CableContent.CABLE_DRUM_ENTITY, pos, state);
	}

	public int stored() {
		return this.stored;
	}

	public List<BlockPos> route() {
		return List.copyOf(this.route);
	}

	public boolean looped() {
		return this.looped;
	}

	public int returnLength() {
		return this.returnLength;
	}

	public float speed() {
		return this.speed;
	}

	/** The last support the cable runs through: the last anchor, or the drum itself. */
	public BlockPos routeEnd() {
		return this.route.isEmpty() ? this.getBlockPos() : this.route.getLast();
	}

	public boolean hasCable() {
		return this.stored > 0 || !this.route.isEmpty();
	}

	public boolean hanging() {
		BlockState state = this.getBlockState();
		return state.hasProperty(CableDrumBlock.HANGING) && state.getValue(CableDrumBlock.HANGING);
	}

	/** Where the cable leaves this drum (over the barrel's middle, top of the cable when it is wound). */
	public Vec3 exitPoint() {
		return CableRoutes.drumPoint(this.getBlockPos(), this.hanging());
	}

	/** The working strand: the drum's exit, then each anchor's roller. */
	public List<Vec3> points() {
		List<Vec3> points = new ArrayList<>();
		points.add(this.exitPoint());
		for (BlockPos anchor : this.route) {
			points.add(CableRoutes.point(anchor, false));
		}
		return points;
	}

	/** Cable laid out along the route, in blocks. */
	public int laidOut() {
		int total = 0;
		List<Vec3> points = this.points();
		for (int i = 1; i < points.size(); i++) {
			total += (int) Math.ceil(points.get(i - 1).distanceTo(points.get(i)));
		}
		return total;
	}

	/** All the drum's cable, in blocks: wound, laid out and return strand. */
	public int totalCable() {
		return this.stored + this.laidOut() + this.returnLength;
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

	/** Closes or opens the loop; {@code returnLength} blocks move between the drum and the return strand. */
	void setLoop(boolean looped, int returnLength) {
		this.looped = looped;
		this.returnLength = returnLength;
		if (!looped) {
			this.speed = 0.0F;
		}
		this.changed();
	}

	void setSpeed(float speed) {
		this.speed = speed;
		boolean startOrStop = (speed == 0.0F) != (this.sentSpeed == 0.0F) || Math.signum(speed) != Math.signum(this.sentSpeed);
		if ((startOrStop || Math.abs(speed - this.sentSpeed) > 0.004F) && this.level instanceof ServerLevel serverLevel) {
			this.sentSpeed = speed;
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
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
		CableDrive.tickDrum((ServerLevel) level, drum);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, CableDrumBlockEntity drum) {
		drum.previousTravel = drum.travel;
		drum.travel += drum.speed;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("cable", this.stored);
		output.store("route", BlockPos.CODEC.listOf(), List.copyOf(this.route));
		if (this.looped) {
			output.putBoolean("loop", true);
			output.putInt("return_length", this.returnLength);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.stored = Math.max(0, input.getIntOr("cable", 0));
		this.route.clear();
		this.route.addAll(input.read("route", BlockPos.CODEC.listOf()).orElse(List.of()));
		this.looped = input.getBooleanOr("loop", false);
		this.returnLength = this.looped ? Math.max(0, input.getIntOr("return_length", 0)) : 0;
		this.speed = input.getFloatOr("speed", 0.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		this.saveAdditional(output);
		output.putFloat("speed", this.speed);
		return output.buildResult();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (this.level instanceof ServerLevel serverLevel) {
			CableIndex.remove(serverLevel, this.getBlockPos());
		}
	}

	/** The drum is going: its cable (wound, laid out and return) drops as coils and cables; the anchors are freed. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level instanceof ServerLevel serverLevel) {
			CableRoutes.drumRemoved(serverLevel, this);
		}
	}
}
