package io.github.boxofdog.simpleblock.mixin.train;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.EngineSelector;

/**
 * M05: vanilla's furnace minecart is the Cobblestone Engine ({@link CobblestoneEngine}). Its own
 * fuel-and-push handling is bypassed: uses go to the engine's controls, its push stays zero, and each
 * server tick runs the engine's traction after vanilla has moved it. Vanilla's synced "has fuel" flag
 * is kept true while it burns coal, for the smoke. The engine's state is saved under
 * {@code simple_block:engine_*}. M15: its rear selector lever ({@link EngineSelector}), synced and saved.
 */
@Mixin(MinecartFurnace.class)
abstract class MinecartFurnaceMixin extends AbstractMinecart implements CobblestoneEngine.Engine {
	@Shadow
	@Final
	private static EntityDataAccessor<Boolean> DATA_ID_FUEL;

	@Unique
	private int simpleBlock$fuel;
	@Unique
	private boolean simpleBlock$running;
	@Unique
	private Vec3 simpleBlock$heading = Vec3.ZERO;
	@Unique
	private double simpleBlock$speed;
	@Unique
	private boolean simpleBlock$burning;
	@Unique
	private static final EntityDataAccessor<Byte> SIMPLE_BLOCK_SELECTOR = SynchedEntityData.defineId(MinecartFurnace.class, EntityDataSerializers.BYTE);
	@Unique
	private long simpleBlock$armedAt;
	@Unique
	private boolean simpleBlock$serviceBegan;
	@Unique
	private @Nullable BlockPos simpleBlock$passed;

	private MinecartFurnaceMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public int simpleBlock$fuel() {
		return this.simpleBlock$fuel;
	}

	@Override
	public void simpleBlock$setFuel(int fuel) {
		this.simpleBlock$fuel = Math.max(0, fuel);
	}

	@Override
	public boolean simpleBlock$running() {
		return this.simpleBlock$running;
	}

	@Override
	public void simpleBlock$setRunning(boolean running) {
		this.simpleBlock$running = running;
	}

	@Override
	public Vec3 simpleBlock$heading() {
		return this.simpleBlock$heading;
	}

	@Override
	public void simpleBlock$setHeading(Vec3 heading) {
		this.simpleBlock$heading = heading;
	}

	@Override
	public double simpleBlock$speed() {
		return this.simpleBlock$speed;
	}

	@Override
	public void simpleBlock$setSpeed(double speed) {
		this.simpleBlock$speed = speed;
	}

	@Override
	public void simpleBlock$setBurning(boolean burning) {
		this.simpleBlock$burning = burning;
		this.entityData.set(DATA_ID_FUEL, burning);
	}

	@Override
	public EngineSelector.Mode simpleBlock$selector() {
		return EngineSelector.Mode.of(this.entityData.get(SIMPLE_BLOCK_SELECTOR));
	}

	@Override
	public void simpleBlock$setSelector(EngineSelector.Mode mode) {
		this.entityData.set(SIMPLE_BLOCK_SELECTOR, (byte) mode.ordinal());
	}

	@Override
	public long simpleBlock$armedAt() {
		return this.simpleBlock$armedAt;
	}

	@Override
	public void simpleBlock$setArmedAt(long time) {
		this.simpleBlock$armedAt = time;
	}

	@Override
	public boolean simpleBlock$serviceBegan() {
		return this.simpleBlock$serviceBegan;
	}

	@Override
	public void simpleBlock$setServiceBegan(boolean began) {
		this.simpleBlock$serviceBegan = began;
	}

	@Override
	public @Nullable BlockPos simpleBlock$passed() {
		return this.simpleBlock$passed;
	}

	@Override
	public void simpleBlock$setPassed(@Nullable BlockPos rail) {
		this.simpleBlock$passed = rail;
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void simpleBlock$defineSelector(SynchedEntityData.Builder entityData, CallbackInfo ci) {
		// A new engine starts at THROUGH: it never stops at a station until asked to.
		entityData.define(SIMPLE_BLOCK_SELECTOR, (byte) EngineSelector.Mode.THROUGH.ordinal());
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$engineControls(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		cir.setReturnValue(CobblestoneEngine.interact((MinecartFurnace) (Object) this, player, hand, location));
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void simpleBlock$drive(CallbackInfo ci) {
		if (!this.level().isClientSide()) {
			MinecartFurnace self = (MinecartFurnace) (Object) this;
			CobblestoneEngine.followTrack(self, self.position().subtract(this.xo, this.yo, this.zo));
			CobblestoneEngine.tick(self);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void simpleBlock$saveEngine(ValueOutput output, CallbackInfo ci) {
		output.putInt("simple_block:engine_fuel", this.simpleBlock$fuel);
		output.putBoolean("simple_block:engine_running", this.simpleBlock$running);
		output.putDouble("simple_block:engine_heading_x", this.simpleBlock$heading.x);
		output.putDouble("simple_block:engine_heading_z", this.simpleBlock$heading.z);
		output.putDouble("simple_block:engine_speed", this.simpleBlock$speed);
		output.putByte("simple_block:engine_selector", (byte) this.simpleBlock$selector().ordinal());
		output.putLong("simple_block:engine_armed_at", this.simpleBlock$armedAt);
		output.putBoolean("simple_block:engine_service_began", this.simpleBlock$serviceBegan);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void simpleBlock$readEngine(ValueInput input, CallbackInfo ci) {
		this.simpleBlock$fuel = Math.max(0, input.getIntOr("simple_block:engine_fuel", 0));
		this.simpleBlock$running = input.getBooleanOr("simple_block:engine_running", false);
		this.simpleBlock$heading = new Vec3(input.getDoubleOr("simple_block:engine_heading_x", 0.0), 0.0, input.getDoubleOr("simple_block:engine_heading_z", 0.0));
		this.simpleBlock$speed = Math.max(0.0, input.getDoubleOr("simple_block:engine_speed", 0.0));
		this.simpleBlock$setSelector(EngineSelector.Mode.of(input.getByteOr("simple_block:engine_selector", (byte) 0)));
		this.simpleBlock$armedAt = input.getLongOr("simple_block:engine_armed_at", 0L);
		this.simpleBlock$serviceBegan = input.getBooleanOr("simple_block:engine_service_began", false);
	}
}
