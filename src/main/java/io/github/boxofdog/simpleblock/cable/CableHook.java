package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The loose end of a drum's cable, with its hook. It hangs at the last support of the route (in front
 * of the drum, or under the last anchor) until a player picks it up; then it follows their hand like a
 * lead until it is put down, slips out of reach, or the player dies, leaves or changes dimension.
 *
 * <p>The drum owns it ({@link CableDrumBlockEntity#hookId}): it is not saved, and the drum puts a new
 * one back after loading. A hook whose drum is gone or has another hook removes itself.
 */
public class CableHook extends Entity {
	private static final EntityDataAccessor<BlockPos> DATA_DRUM = SynchedEntityData.defineId(CableHook.class, EntityDataSerializers.BLOCK_POS);
	/** The last support the cable runs through (the drum itself for an empty route). */
	private static final EntityDataAccessor<BlockPos> DATA_END = SynchedEntityData.defineId(CableHook.class, EntityDataSerializers.BLOCK_POS);
	/** The holding player's entity id, or -1. */
	private static final EntityDataAccessor<Integer> DATA_HOLDER = SynchedEntityData.defineId(CableHook.class, EntityDataSerializers.INT);
	/** Cable between the support and the top of a hanging hook in its ordinary place. */
	static final double HANG = 0.5;
	/**
	 * M07 playtest patch §5A: carried by hand, the end goes taut this far from its support, measured
	 * across the ground only (a free end hanging down a shaft is measured from straight under its anchor)...
	 */
	static final double TAUT = 12.0;
	/** ...and is put down at its support (hanging where it was) beyond this. No message: the cable says it. */
	static final double PUT_DOWN = 16.0;

	public CableHook(EntityType<? extends CableHook> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	CableHook(ServerLevel level, CableDrumBlockEntity drum) {
		this(CableContent.CABLE_HOOK, level);
		this.entityData.set(DATA_DRUM, drum.getBlockPos());
		this.entityData.set(DATA_END, drum.routeEnd());
		this.setPos(restPosition(drum.getBlockPos(), drum.getBlockState(), drum.routeEnd(), drop(level, drum.getBlockPos())));
	}

	/** The cable paid out to this drum's free hook (0 = its ordinary place). */
	static double drop(ServerLevel level, BlockPos drum) {
		CableLines.Line line = CableLines.line(level, drum);
		return line == null ? 0.0 : line.drop;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_DRUM, BlockPos.ZERO);
		builder.define(DATA_END, BlockPos.ZERO);
		builder.define(DATA_HOLDER, -1);
	}

	public BlockPos drumPos() {
		return this.entityData.get(DATA_DRUM);
	}

	public BlockPos endPos() {
		return this.entityData.get(DATA_END);
	}

	/** Where the loose cable starts: the drum's exit or the last anchor's roller. */
	public Vec3 endPoint() {
		return CableRoutes.point(this.level(), this.endPos(), this.endPos().equals(this.drumPos()));
	}

	public int holderId() {
		return this.entityData.get(DATA_HOLDER);
	}

	public boolean isHeld() {
		return this.holderId() >= 0;
	}

	public @Nullable Player holder() {
		return this.isHeld() && this.level().getEntity(this.holderId()) instanceof Player player ? player : null;
	}

	/** Where the cable meets the hook (the top of its eye), at the hook's current position. */
	public Vec3 eyePoint(Vec3 position) {
		return position.add(0.0, this.getBbHeight(), 0.0);
	}

	public @Nullable CableDrumBlockEntity drum(ServerLevel level) {
		BlockPos pos = this.drumPos();
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.hookId == this.getId() ? drum : null;
	}

	/** Puts the end down: it goes back to hang at the last support. */
	public void release() {
		if (this.isHeld()) {
			this.entityData.set(DATA_HOLDER, -1);
			this.playSound(SoundEvents.LEAD_UNTIED, 0.8F, 1.0F);
		}
	}

	/**
	 * Where a hook that nobody holds hangs: just under the last anchor, or, with no route, lying at the
	 * foot of a standing drum's front (low, so a click on the drum's upper half reaches the drum), or
	 * hanging just below the front of a drum hung under a beam.
	 */
	static Vec3 restPosition(BlockPos drum, BlockState drumState, BlockPos end) {
		return restPosition(drum, drumState, end, 0.0);
	}

	/** As {@link #restPosition(BlockPos, BlockState, BlockPos)}, with {@code drop} of cable paid out below the last anchor (M07 patch §5A). */
	static Vec3 restPosition(BlockPos drum, BlockState drumState, BlockPos end, double drop) {
		if (end.equals(drum)) {
			Direction facing = drumState.hasProperty(CableDrumBlock.FACING) ? drumState.getValue(CableDrumBlock.FACING) : Direction.NORTH;
			boolean hanging = drumState.hasProperty(CableDrumBlock.HANGING) && drumState.getValue(CableDrumBlock.HANGING);
			return new Vec3(drum.getX() + 0.5 + facing.getStepX() * 0.62, drum.getY() + (hanging ? -0.3 : 0.02),
				drum.getZ() + 0.5 + facing.getStepZ() * 0.62);
		}
		return CableRoutes.point(end, false).subtract(0.0, Math.max(HANG, drop) + CableContent.CABLE_HOOK.getHeight(), 0.0);
	}

	/** Where a held hook is: in the hand, eye up. */
	public Vec3 heldPosition(Player holder, float partialTicks) {
		return holder.getRopeHoldPosition(partialTicks).subtract(0.0, this.getBbHeight() * 0.5, 0.0);
	}

	@Override
	public void tick() {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		CableDrumBlockEntity drum = this.drum(level);
		if (drum == null || !drum.hasCable()) {
			this.discard();
			return;
		}
		BlockPos end = drum.routeEnd();
		this.entityData.set(DATA_END, end);
		Player holder = this.holder();
		if (this.isHeld() && (holder == null || !holder.isAlive() || holder.isSpectator())) {
			this.release();
			holder = null;
		}
		Vec3 target;
		Vec3 rest = restPosition(drum.getBlockPos(), drum.getBlockState(), end, drop(level, drum.getBlockPos()));
		if (holder != null) {
			target = this.heldPosition(holder, 1.0F);
			// Measured across the ground from the support (for a free end down a shaft: from under its anchor).
			Vec3 support = this.endPoint();
			double dx = target.x - support.x;
			double dz = target.z - support.z;
			double across = Math.sqrt(dx * dx + dz * dz);
			if (across > PUT_DOWN) {
				this.release();
				target = rest;
			} else if (across > TAUT) {
				// Taut: the end stays where the cable reaches, towards the hand.
				target = new Vec3(support.x + dx * TAUT / across, target.y, support.z + dz * TAUT / across);
			}
		} else {
			target = this.wind(level, drum, rest);
		}
		if (!target.equals(this.position())) {
			this.setPos(target);
		}
	}

	/**
	 * M07 playtest patch §5A: a free hook hanging under the route's last anchor (no skip on it, not a
	 * loop) is paid out and wound in by the drum's drive, crank or coal winch: FEED lowers it, PULL raises
	 * it, straight down its shaft. It stops in its ordinary place under the anchor ({@code end}) and on
	 * anything under it ({@code blocked}); it moves only in its own tick, so never while its chunk is not
	 * entity-ticking. It reports to the line as a gripping cart does (coal burns only while it moves).
	 * Returns where it hangs now.
	 */
	private Vec3 wind(ServerLevel level, CableDrumBlockEntity drum, Vec3 rest) {
		CableLines.Line line = CableLines.line(level, drum.getBlockPos());
		if (line == null || line.looped || line.hoist != null || line.anchors.isEmpty()) {
			return rest;
		}
		double speed = line.speed;
		String halt = null;
		if (Math.abs(speed) > 1.0E-6) {
			double from = Math.max(HANG, line.drop);
			double to = from + speed;
			if (to < HANG) {
				to = HANG;
				halt = "end";
			}
			double reached = from;
			double step = Math.signum(to - from) / 16.0;
			while (Math.abs(to - reached) > 1.0E-9) {
				double next = Math.abs(to - reached) < Math.abs(step) ? to : reached + step;
				Vec3 at = rest.subtract(0.0, next - from, 0.0);
				if (!level.noCollision(this, this.getDimensions(this.getPose()).makeBoundingBox(at).deflate(1.0E-4))) {
					halt = "blocked";
					break;
				}
				reached = next;
			}
			if (reached == from && halt == null) {
				halt = "blocked";
			}
			if (reached != from) {
				line.drop = reached;
				CableLines.get(level).changed();
				rest = rest.subtract(0.0, reached - from, 0.0);
			}
		}
		CableHoists.report(level, drum.getBlockPos(), this.getId(), rest.y, 0.0, halt);
		return rest;
	}

	/** Picking up the end. Client and server agree on the result; only the server changes anything. */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (this.isHeld()) {
			return InteractionResult.PASS;
		}
		if (this.level() instanceof ServerLevel level && this.drum(level) instanceof CableDrumBlockEntity drum) {
			if (CableRoutes.heldHook(level, player) != null) {
				CableRoutes.tell(player, CableRoutes.message("hands_full", "You are already holding a cable end"));
			} else if (!drum.hasCoil()) {
				CableRoutes.tell(player, CableRoutes.message("none_left", "No cable on the drum: use a cable coil on it"));
			} else {
				this.entityData.set(DATA_HOLDER, player.getId());
				this.playSound(SoundEvents.LEAD_TIED, 0.8F, 1.0F);
				CableRoutes.tell(player, CableRoutes.message("picked_up", "Cable end picked up: use an anchor to run the cable through it"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Only a hanging hook can be clicked: a held one is in the player's way. */
	@Override
	public boolean isPickable() {
		return !this.isHeld();
	}

	@Override
	public boolean skipAttackInteraction(Entity source) {
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 128.0 * 128.0;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
