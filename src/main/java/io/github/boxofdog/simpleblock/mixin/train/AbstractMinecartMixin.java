package io.github.boxofdog.simpleblock.mixin.train;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
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

/**
 * M05 train coupling state on every minecart ({@link Couplings}): the two coupled partners (saved by
 * UUID as {@code simple_block:coupling_0/1}, synced to clients as entity ids for drawing the chains),
 * and the train controls' transient state (a held cart's pin position, whether a driver had it last
 * tick). Each server tick reports a cart with any of these to {@link Couplings#noteActive}.
 */
@Mixin(AbstractMinecart.class)
abstract class AbstractMinecartMixin extends VehicleEntity implements Couplings.Coupled {
	@Unique
	private static final EntityDataAccessor<Integer> SIMPLE_BLOCK_COUPLED_0 = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> SIMPLE_BLOCK_COUPLED_1 = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

	@Unique
	private final @Nullable UUID[] simpleBlock$couplings = new UUID[2];
	@Unique
	private @Nullable Vec3 simpleBlock$holdPos;
	@Unique
	private boolean simpleBlock$controlled;
	@Unique
	private @Nullable BlockPos simpleBlock$dock;

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

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void simpleBlock$defineCouplings(SynchedEntityData.Builder entityData, CallbackInfo ci) {
		entityData.define(SIMPLE_BLOCK_COUPLED_0, -1);
		entityData.define(SIMPLE_BLOCK_COUPLED_1, -1);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void simpleBlock$noteTrain(CallbackInfo ci) {
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
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void simpleBlock$readCouplings(ValueInput input, CallbackInfo ci) {
		for (int slot = 0; slot < 2; slot++) {
			this.simpleBlock$couplings[slot] = input.read("simple_block:coupling_" + slot, UUIDUtil.CODEC).orElse(null);
		}
		this.simpleBlock$dock = input.read("simple_block:dock", BlockPos.CODEC).orElse(null);
	}
}
