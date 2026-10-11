package io.github.boxofdog.simpleblock.mixin.minecart;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;

/**
 * FRT-01..08: the vanilla minecart carries cargo instead of a rider (rules in {@link MinecartCargo}),
 * or, with a cushion fitted, a rider ({@link MinecartCushions}). The cargo and cushion are three
 * extra synced values on {@link Minecart} (nothing extends it), saved under {@code simple_block:*}. The overrides below are added to {@code Minecart}, which does not
 * override these methods itself; {@code super} is {@link AbstractMinecart}, as in vanilla.
 */
@Mixin(Minecart.class)
abstract class MinecartMixin extends AbstractMinecart implements MinecartCargo.Cart {
	@Unique
	private static final EntityDataAccessor<ItemStack> SIMPLE_BLOCK_CARGO = SynchedEntityData.defineId(Minecart.class, EntityDataSerializers.ITEM_STACK);
	@Unique
	private static final EntityDataAccessor<Integer> SIMPLE_BLOCK_COUNT = SynchedEntityData.defineId(Minecart.class, EntityDataSerializers.INT);
	/** Fitted cushion colour id, or -1 for none. */
	@Unique
	private static final EntityDataAccessor<Integer> SIMPLE_BLOCK_CUSHION = SynchedEntityData.defineId(Minecart.class, EntityDataSerializers.INT);

	private MinecartMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public Item simpleBlock$cargo() {
		return this.entityData.get(SIMPLE_BLOCK_CARGO).getItem();
	}

	@Override
	public int simpleBlock$count() {
		return this.entityData.get(SIMPLE_BLOCK_COUNT);
	}

	@Override
	public void simpleBlock$setCargo(Item item, int count) {
		boolean empty = count <= 0 || item == Items.AIR;
		this.entityData.set(SIMPLE_BLOCK_CARGO, empty ? ItemStack.EMPTY : new ItemStack(item));
		this.entityData.set(SIMPLE_BLOCK_COUNT, empty ? 0 : count);
	}

	@Override
	public @Nullable DyeColor simpleBlock$cushion() {
		int id = this.entityData.get(SIMPLE_BLOCK_CUSHION);
		return id < 0 ? null : DyeColor.byId(id);
	}

	@Override
	public void simpleBlock$setCushion(@Nullable DyeColor color) {
		this.entityData.set(SIMPLE_BLOCK_CUSHION, color == null ? -1 : color.getId());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(SIMPLE_BLOCK_CARGO, ItemStack.EMPTY);
		entityData.define(SIMPLE_BLOCK_COUNT, 0);
		entityData.define(SIMPLE_BLOCK_CUSHION, -1);
	}

	/**
	 * Using a minecart loads or unloads it instead of getting in, unless it has a cushion (or one is
	 * being fitted); getting into a cushioned cart is left to vanilla.
	 */
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$cargoInsteadOfRiding(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		Minecart self = (Minecart) (Object) this;
		if (MinecartCushions.handles(self, player, hand)) {
			InteractionResult result = MinecartCushions.interact(self, player, hand);
			if (result != null) {
				cir.setReturnValue(result);
			}
			return;
		}
		cir.setReturnValue(MinecartCargo.interact(self, player, hand));
	}

	/** Rideable only with a cushion: otherwise a moving cart no longer scoops up mobs it bumps into. */
	@Inject(method = "isRideable", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$rideableOnlyWithCushion(CallbackInfoReturnable<Boolean> cir) {
		if (this.simpleBlock$cushion() == null) {
			cir.setReturnValue(false);
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.simpleBlock$cushion() != null && super.canAddPassenger(passenger);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		// Killed or destroyed (not unloaded, not changing dimension): the cargo spills.
		if (reason.shouldDestroy() && this.level() instanceof ServerLevel level) {
			MinecartCargo.dropCargo((Minecart) (Object) this, level);
			MinecartCushions.dropCushion((Minecart) (Object) this, level);
		}
		super.remove(reason);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// A creative player's hit discards the cart without destroying it; spill the cargo first.
		if (!this.isRemoved() && !this.isInvulnerableToBase(source) && source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
			MinecartCargo.dropCargo((Minecart) (Object) this, level);
			MinecartCushions.dropCushion((Minecart) (Object) this, level);
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		MinecartCargo.save(this, output);
		MinecartCushions.save(this, output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		MinecartCargo.read(this, input);
		MinecartCushions.read(this, input);
	}
}
