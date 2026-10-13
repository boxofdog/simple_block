package io.github.boxofdog.simpleblock.mixin.train;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M05 train coupling state on every minecart ({@link Couplings}): the two coupled partners (saved by
 * UUID as {@code simple_block:coupling_0/1}, synced to clients as entity ids for drawing the chains),
 * and the train controls' transient state (a held cart's pin position, whether a driver had it last
 * tick). Each server tick reports a cart with any of these to {@link Couplings#noteActive}.
 *
 * <p>M06: the loading-track state ({@link LoadingTracks.Freight}): the track a cart is docked at
 * ({@code simple_block:dock}), the track it was released from ({@code simple_block:released}) and the
 * way its front points ({@code simple_block:front}, synced for drawing its gate). M07: whether its
 * front gate is folded down as a ramp into a skip (synced, not saved: the skip keeps it down while it
 * tips the cart, and it closes by itself a few ticks after the skip stops).
 */
@Mixin(AbstractMinecart.class)
abstract class AbstractMinecartMixin extends VehicleEntity implements Couplings.Coupled, LoadingTracks.Freight {
	@Unique
	private static final EntityDataAccessor<Integer> SIMPLE_BLOCK_COUPLED_0 = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> SIMPLE_BLOCK_COUPLED_1 = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

	@Unique
	private static final EntityDataAccessor<Byte> SIMPLE_BLOCK_FRONT = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BYTE);
	@Unique
	private static final EntityDataAccessor<Boolean> SIMPLE_BLOCK_GATE_OPEN = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private int simpleBlock$gateHold;

	@Unique
	private final @Nullable UUID[] simpleBlock$couplings = new UUID[2];
	@Unique
	private @Nullable Vec3 simpleBlock$holdPos;
	@Unique
	private boolean simpleBlock$controlled;
	@Unique
	private @Nullable BlockPos simpleBlock$dock;
	@Unique
	private @Nullable BlockPos simpleBlock$released;
	@Unique
	private boolean simpleBlock$onReleased;
	@Unique
	private @Nullable BlockPos simpleBlock$lastRail;
	@Unique
	private @Nullable RailShape simpleBlock$lastShape;

	private AbstractMinecartMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Unique
	private static EntityDataAccessor<Integer> simpleBlock$idData(int slot) {
		return slot == 0 ? SIMPLE_BLOCK_COUPLED_0 : SIMPLE_BLOCK_COUPLED_1;
	}

	@Override
	public @Nullable UUID simpleBlock$coupling(int slot) {
		return this.simpleBlock$couplings[slot];
	}

	@Override
	public void simpleBlock$setCoupling(int slot, @Nullable UUID partner) {
		this.simpleBlock$couplings[slot] = partner;
	}

	@Override
	public int simpleBlock$couplingId(int slot) {
		return this.entityData.get(simpleBlock$idData(slot));
	}

	@Override
	public void simpleBlock$setCouplingId(int slot, int id) {
		this.entityData.set(simpleBlock$idData(slot), id);
	}

	@Override
	public @Nullable Vec3 simpleBlock$holdPos() {
		return this.simpleBlock$holdPos;
	}

	@Override
	public void simpleBlock$setHoldPos(@Nullable Vec3 pos) {
		this.simpleBlock$holdPos = pos;
	}

	@Override
	public boolean simpleBlock$controlled() {
		return this.simpleBlock$controlled;
	}

	@Override
	public void simpleBlock$setControlled(boolean controlled) {
		this.simpleBlock$controlled = controlled;
	}

	@Override
	public @Nullable BlockPos simpleBlock$dock() {
		return this.simpleBlock$dock;
	}

	@Override
	public void simpleBlock$setDock(@Nullable BlockPos dock) {
		this.simpleBlock$dock = dock;
	}

	@Override
	public @Nullable Direction simpleBlock$front() {
		byte front = this.entityData.get(SIMPLE_BLOCK_FRONT);
		return front < 0 ? null : Direction.from2DDataValue(front);
	}

	@Override
	public void simpleBlock$setFront(@Nullable Direction front) {
		this.entityData.set(SIMPLE_BLOCK_FRONT, front == null ? (byte) -1 : (byte) front.get2DDataValue());
	}

	@Override
	public boolean simpleBlock$gateOpen() {
		return this.entityData.get(SIMPLE_BLOCK_GATE_OPEN);
	}

	@Override
	public void simpleBlock$holdGateOpen(int ticks) {
		this.simpleBlock$gateHold = ticks;
		if (ticks > 0 != this.simpleBlock$gateOpen()) {
			this.entityData.set(SIMPLE_BLOCK_GATE_OPEN, ticks > 0);
		}
	}

	@Override
	public @Nullable BlockPos simpleBlock$released() {
		return this.simpleBlock$released;
	}

	@Override
	public void simpleBlock$setReleased(@Nullable BlockPos released) {
		this.simpleBlock$released = released;
	}

	@Override
	public boolean simpleBlock$onReleased() {
		return this.simpleBlock$onReleased;
	}

	@Override
	public void simpleBlock$setOnReleased(boolean on) {
		this.simpleBlock$onReleased = on;
	}

	@Override
	public @Nullable BlockPos simpleBlock$lastRail() {
		return this.simpleBlock$lastRail;
	}

	@Override
	public @Nullable RailShape simpleBlock$lastShape() {
		return this.simpleBlock$lastShape;
	}

	@Override
	public void simpleBlock$setLastRail(@Nullable BlockPos pos, @Nullable RailShape shape) {
		this.simpleBlock$lastRail = pos;
		this.simpleBlock$lastShape = shape;
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void simpleBlock$defineCouplings(SynchedEntityData.Builder entityData, CallbackInfo ci) {
		entityData.define(SIMPLE_BLOCK_COUPLED_0, -1);
		entityData.define(SIMPLE_BLOCK_COUPLED_1, -1);
		entityData.define(SIMPLE_BLOCK_FRONT, (byte) -1);
		entityData.define(SIMPLE_BLOCK_GATE_OPEN, false);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void simpleBlock$noteTrain(CallbackInfo ci) {
		if (!this.level().isClientSide()) {
			LoadingTracks.tick((AbstractMinecart) (Object) this);
			if (this.simpleBlock$gateHold > 0 && --this.simpleBlock$gateHold == 0) {
				this.entityData.set(SIMPLE_BLOCK_GATE_OPEN, false);
			}
		}
		if (!this.level().isClientSide() && (this.simpleBlock$couplings[0] != null || this.simpleBlock$couplings[1] != null
			|| this.simpleBlock$holdPos != null || this.simpleBlock$controlled || this.simpleBlock$dock != null)) {
			Couplings.noteActive((AbstractMinecart) (Object) this);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void simpleBlock$saveCouplings(ValueOutput output, CallbackInfo ci) {
		for (int slot = 0; slot < 2; slot++) {
			output.storeNullable("simple_block:coupling_" + slot, UUIDUtil.CODEC, this.simpleBlock$couplings[slot]);
		}
		output.storeNullable("simple_block:dock", BlockPos.CODEC, this.simpleBlock$dock);
		output.storeNullable("simple_block:released", BlockPos.CODEC, this.simpleBlock$released);
		output.storeNullable("simple_block:front", Direction.CODEC, this.simpleBlock$front());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void simpleBlock$readCouplings(ValueInput input, CallbackInfo ci) {
		for (int slot = 0; slot < 2; slot++) {
			this.simpleBlock$couplings[slot] = input.read("simple_block:coupling_" + slot, UUIDUtil.CODEC).orElse(null);
		}
		this.simpleBlock$dock = input.read("simple_block:dock", BlockPos.CODEC).orElse(null);
		this.simpleBlock$released = input.read("simple_block:released", BlockPos.CODEC).orElse(null);
		this.simpleBlock$setFront(input.read("simple_block:front", Direction.CODEC).orElse(null));
	}
}
