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

import org.jspecify.annotations.Nullable;

/**
 * A drum's cable installation (M06): whether a cable coil is fitted ({@link #hasCoil}), and the anchors
 * the cable runs through, in order from the drum ({@link #route}). The route's length is geometric only:
 * one coil equips the drum for any number of anchors over any distance, a closed loop's return strand
 * costs nothing more, and taking the installation apart gives back the one coil. Spans are still at
 * most {@code cable.max_span} blocks and clear of solid blocks ({@link CableRoutes}).
 *
 * <p>The route and loop are saved here and copied into the level's {@link CableLines} whenever they
 * change. The drive (crank, coal winch lever, coal, burn time, speed) lives in that line, because it
 * changes while the drum is unloaded; this block entity keeps a copy for drawing and as a backup.
 * Saved with the block and sent to clients (for drawing the cable, spool, crank or lever and furnace).
 */
public class CableDrumBlockEntity extends BlockEntity {
	private boolean coil;
	private final List<BlockPos> route = new ArrayList<>();
	private boolean looped;
	/** Copies of the line's drive state (M06 coal winch), for drawing and as a backup. */
	private boolean powered;
	private CableLines.Lever lever = CableLines.Lever.NEUTRAL;
	private int coal;
	private int burn;
	/** Working-strand speed, blocks per tick, as last copied from the line; not saved. */
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
	/** Whether the route has been checked at least once since loading (later checks wait for missing anchors to load). */
	boolean checked;
	/** Whether this drum has met its line since loading (applied a cut, copied its geometry). */
	boolean linked;
	/**
	 * M07 playtest change 2, on the drum holding the cable: the second drum its cable runs to (a two-drum
	 * line; {@link #looped} is then set too, and there is no return strand), or null; and whether that drum hangs.
	 */
	private @Nullable BlockPos endDrum;
	private boolean endHanging;
	/** On that second drum: the drum whose cable it holds the end of, or null ({@link FarDrums}). */
	private @Nullable BlockPos farFrom;
	/** The second drum's copy of whether the first is a coal winch: it then shows a lever (no motor), not a crank. */
	private boolean farLever;

	public CableDrumBlockEntity(BlockPos pos, BlockState state) {
		super(CableContent.CABLE_DRUM_ENTITY, pos, state);
	}

	public boolean hasCoil() {
		return this.coil;
	}

	public List<BlockPos> route() {
		return List.copyOf(this.route);
	}

	public boolean looped() {
		return this.looped;
	}

	public float speed() {
		return this.speed;
	}

	/** M07 playtest change 2: the second drum this drum's cable runs to, or null. */
	public @Nullable BlockPos endDrum() {
		return this.endDrum;
	}

	/** M07 playtest change 2: the drum whose cable this drum holds the end of, or null. */
	public @Nullable BlockPos farFrom() {
		return this.farFrom;
	}

	/** Whether a lever is shown where the crank is: a coal winch, or the far drum of a coal winch's loop (no motor). */
	public boolean showsLever() {
		return this.powered || this.farLever;
	}

	/** M06: whether a furnace is fitted (the coal winch). */
	public boolean powered() {
		return this.powered;
	}

	public CableLines.Lever lever() {
		return this.lever;
	}

	public int coal() {
		return this.coal;
	}

	public int burn() {
		return this.burn;
	}

	/** The last support the cable runs through: the last anchor, or the drum itself. */
	public BlockPos routeEnd() {
		return this.route.isEmpty() ? this.getBlockPos() : this.route.getLast();
	}

	/** Whether the drum has a cable installation (the same as {@link #hasCoil}: a route needs the coil). */
	public boolean hasCable() {
		return this.coil;
	}

	public boolean hanging() {
		BlockState state = this.getBlockState();
		return state.hasProperty(CableDrumBlock.HANGING) && state.getValue(CableDrumBlock.HANGING);
	}

	/** Where the cable leaves this drum (over the barrel's middle, top of the cable when it is wound). */
	public Vec3 exitPoint() {
		return CableRoutes.drumPoint(this.getBlockPos(), this.hanging());
	}

	/** The working strand: the drum's exit, then each anchor's roller, then (two drums) the second drum's exit. */
	public List<Vec3> points() {
		List<Vec3> points = new ArrayList<>();
		points.add(this.exitPoint());
		for (BlockPos anchor : this.route) {
			points.add(CableRoutes.point(anchor, false));
		}
		if (this.endDrum != null) {
			points.add(CableRoutes.drumPoint(this.endDrum, this.endHanging));
		}
		return points;
	}

	/** Length of the working strand along the route, in blocks (for status and tests; nothing is charged for it). */
	public int laidOut() {
		double total = 0.0;
		List<Vec3> points = this.points();
		for (int i = 1; i < points.size(); i++) {
			total += points.get(i - 1).distanceTo(points.get(i));
		}
		return (int) Math.ceil(total);
	}

	void setCoil(boolean coil) {
		this.coil = coil;
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

	/** Closes or opens the loop (the return strand costs nothing: M06). Opening also takes the cable off a second drum. */
	void setLoop(boolean looped) {
		this.looped = looped;
		if (!looped) {
			this.speed = 0.0F;
			this.endDrum = null;
		}
		this.changed();
	}

	/** M07 playtest change 2: the cable's end is made fast to the second drum at {@code end}; the line is driven like a loop. */
	void setEndDrum(BlockPos end, boolean hanging) {
		this.endDrum = end.immutable();
		this.endHanging = hanging;
		this.setLoop(true);
	}

	/** Copies the line's drive state for drawing; tells clients when it changes visibly. */
	void copyDrive(CableLines.Line line) {
		boolean visible = this.powered != line.powered || this.lever != line.lever;
		boolean saved = visible || this.coal != line.coal || this.burn != line.burn;
		this.powered = line.powered;
		this.lever = line.lever;
		this.coal = line.coal;
		this.burn = line.burn;
		if (saved) {
			this.setChanged();
		}
		if (visible && this.level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
		this.setSpeed(line.shownSpeed());
	}

	private void setSpeed(float speed) {
		this.speed = speed;
		boolean startOrStop = (speed == 0.0F) != (this.sentSpeed == 0.0F) || Math.signum(speed) != Math.signum(this.sentSpeed);
		if ((startOrStop || Math.abs(speed - this.sentSpeed) > 0.004F) && this.level instanceof ServerLevel serverLevel) {
			this.sentSpeed = speed;
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Makes this drum the far drum of the loop of the drum at {@code home}, or (null) a plain drum again. */
	void setFarFrom(@Nullable BlockPos home) {
		this.farFrom = home == null ? null : home.immutable();
		if (home == null) {
			this.farLever = false;
			this.lever = CableLines.Lever.NEUTRAL;
			this.setSpeed(0.0F);
		}
		this.setChanged();
		if (this.level instanceof ServerLevel serverLevel) {
			// Its barrel shows cable wound on while it holds the end.
			BlockState state = this.getBlockState();
			boolean wound = this.hasCable() || home != null;
			if (state.hasProperty(CableDrumBlock.WOUND) && state.getValue(CableDrumBlock.WOUND) != wound) {
				serverLevel.setBlock(this.getBlockPos(), state.setValue(CableDrumBlock.WOUND, wound), Block.UPDATE_ALL);
			}
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * A far drum shows its loop's drive from its own end: the lever (if the loop's drum is a coal winch)
	 * mirrored, PULL bringing the cart here; the barrel turning the other way.
	 */
	void copyFar(CableLines.Line line) {
		CableLines.Lever shown = line.powered ? line.lever.mirrored() : CableLines.Lever.NEUTRAL;
		boolean visible = this.farLever != line.powered || this.lever != shown;
		this.farLever = line.powered;
		this.lever = shown;
		if (visible && this.level instanceof ServerLevel serverLevel) {
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
		this.setSpeed(-line.shownSpeed());
	}

	/** This drum's backup of the drive, for a line that is missing (lost saved data). */
	void restoreDrive(CableLines.Line line) {
		line.powered = this.powered;
		line.lever = CableLines.Lever.NEUTRAL;
		line.coal = this.coal;
		line.burn = this.burn;
	}

	/** Saves, updates clients and the line (and so the grip index), and shows the barrel wound or bare. */
	void changed() {
		this.setChanged();
		if (this.level instanceof ServerLevel serverLevel) {
			BlockState state = this.getBlockState();
			boolean wound = this.hasCable() || this.farFrom != null;
			if (state.hasProperty(CableDrumBlock.WOUND) && state.getValue(CableDrumBlock.WOUND) != wound) {
				serverLevel.setBlock(this.getBlockPos(), state.setValue(CableDrumBlock.WOUND, wound), Block.UPDATE_ALL);
			}
			serverLevel.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
			if (this.linked) {
				CableLines.mirror(serverLevel, this);
			} else {
				// A drum changed before its first tick (or in a chunk that does not tick) still gets its line.
				CableLines.link(serverLevel, this);
			}
		}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CableDrumBlockEntity drum) {
		ServerLevel serverLevel = (ServerLevel) level;
		if (!drum.linked) {
			CableLines.link(serverLevel, drum);
		}
		CableRoutes.tickDrum(serverLevel, drum);
		CableLines.Line line = CableLines.line(serverLevel, pos);
		if (line != null) {
			CableLines.applyCut(serverLevel, drum, line);
			if (drum.farFrom == null) {
				drum.copyDrive(line);
			}
		}
		FarDrums.tick(serverLevel, drum);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, CableDrumBlockEntity drum) {
		drum.previousTravel = drum.travel;
		drum.travel += drum.speed;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("coil", this.coil);
		output.store("route", BlockPos.CODEC.listOf(), List.copyOf(this.route));
		if (this.looped) {
			output.putBoolean("loop", true);
		}
		if (this.powered) {
			// The line's coal and burn time are exact; this drum's copy may be a tick behind.
			CableLines.Line line = this.level instanceof ServerLevel serverLevel ? CableLines.line(serverLevel, this.getBlockPos()) : null;
			output.putBoolean("powered", true);
			output.store("lever", CableLines.Lever.CODEC, this.lever);
			output.putInt("coal", line != null ? line.coal() : this.coal);
			output.putInt("burn", line != null ? line.burn() : this.burn);
		}
		if (this.endDrum != null) {
			output.store("end_drum", BlockPos.CODEC, this.endDrum);
			output.putBoolean("end_hanging", this.endHanging);
		}
		if (this.farFrom != null) {
			output.store("far_from", BlockPos.CODEC, this.farFrom);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.route.clear();
		this.route.addAll(input.read("route", BlockPos.CODEC.listOf()).orElse(List.of()));
		this.coil = input.getBooleanOr("coil", false);
		this.looped = input.getBooleanOr("loop", false);
		this.powered = input.getBooleanOr("powered", false);
		this.lever = input.read("lever", CableLines.Lever.CODEC).orElse(CableLines.Lever.NEUTRAL);
		this.coal = Math.max(0, input.getIntOr("coal", 0));
		this.burn = Math.max(0, input.getIntOr("burn", 0));
		this.speed = input.getFloatOr("speed", 0.0F);
		this.endDrum = input.read("end_drum", BlockPos.CODEC).orElse(null);
		this.endHanging = input.getBooleanOr("end_hanging", false);
		this.farFrom = input.read("far_from", BlockPos.CODEC).orElse(null);
		// Sent to clients only: a far drum's lever (its own is never saved; it is copied from the loop).
		this.farLever = this.farFrom != null && input.getBooleanOr("far_lever", false);
		if (this.farLever) {
			this.lever = input.read("far_setting", CableLines.Lever.CODEC).orElse(CableLines.Lever.NEUTRAL);
		}
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
		if (this.farLever) {
			output.putBoolean("far_lever", true);
			output.store("far_setting", CableLines.Lever.CODEC, this.lever);
		}
		return output.buildResult();
	}

	/** The drum is going (broken, not unloaded): its coil (and furnace and coal) drop; the anchors are freed; its line goes. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level instanceof ServerLevel serverLevel) {
			FarDrums.removed(serverLevel, this);
			CableRoutes.drumRemoved(serverLevel, this);
		}
	}
}
