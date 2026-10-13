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
	/** Cable between the support and the top of a hanging hook. */
	private static final double HANG = 0.5;

	public CableHook(EntityType<? extends CableHook> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	CableHook(ServerLevel level, CableDrumBlockEntity drum) {
		this(CableContent.CABLE_HOOK, level);
		this.entityData.set(DATA_DRUM, drum.getBlockPos());
		this.entityData.set(DATA_END, drum.routeEnd());
		this.setPos(restPosition(drum.getBlockPos(), drum.getBlockState(), drum.routeEnd()));
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
		if (end.equals(drum)) {
			Direction facing = drumState.hasProperty(CableDrumBlock.FACING) ? drumState.getValue(CableDrumBlock.FACING) : Direction.NORTH;
			boolean hanging = drumState.hasProperty(CableDrumBlock.HANGING) && drumState.getValue(CableDrumBlock.HANGING);
			return new Vec3(drum.getX() + 0.5 + facing.getStepX() * 0.62, drum.getY() + (hanging ? -0.3 : 0.02),
				drum.getZ() + 0.5 + facing.getStepZ() * 0.62);
		}
		return CableRoutes.point(end, false).subtract(0.0, HANG + CableContent.CABLE_HOOK.getHeight(), 0.0);
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
		if (holder != null) {
			target = this.heldPosition(holder, 1.0F);
			double reach = CableRoutes.maxSpan();
			if (this.eyePoint(target).distanceTo(this.endPoint()) > reach + CableRoutes.HOLD_SLACK) {
				this.release();
				CableRoutes.tell(holder, CableRoutes.message("slipped", "The cable end slipped from your hand: it only reaches %s blocks from the last support",
					(int) reach));
				target = restPosition(drum.getBlockPos(), drum.getBlockState(), end);
			}
		} else {
			target = restPosition(drum.getBlockPos(), drum.getBlockState(), end);
		}
		if (!target.equals(this.position())) {
			this.setPos(target);
		}
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
