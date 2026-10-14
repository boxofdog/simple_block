package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.monster.skeleton.Skeleton;

import io.github.boxofdog.simpleblock.feature.SpiderJockeys;

/** M09: the ledge-grab pose of a skeleton riding a spider, synced so the client can draw it ({@link SpiderJockeys}). */
@Mixin(Skeleton.class)
abstract class SkeletonMixin implements SpiderJockeys.Grabbing {
	@Unique
	private static final EntityDataAccessor<Boolean> SIMPLE_BLOCK$GRABBING = SynchedEntityData.defineId(Skeleton.class, EntityDataSerializers.BOOLEAN);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void simpleBlock$defineGrabbing(SynchedEntityData.Builder entityData, CallbackInfo ci) {
		entityData.define(SIMPLE_BLOCK$GRABBING, false);
	}

	@Override
	public boolean simpleBlock$grabbing() {
		return ((Skeleton) (Object) this).getEntityData().get(SIMPLE_BLOCK$GRABBING);
	}

	@Override
	public void simpleBlock$setGrabbing(boolean grabbing) {
		((Skeleton) (Object) this).getEntityData().set(SIMPLE_BLOCK$GRABBING, grabbing);
	}
}
