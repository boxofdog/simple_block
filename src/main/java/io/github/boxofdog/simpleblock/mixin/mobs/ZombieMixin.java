package io.github.boxofdog.simpleblock.mixin.mobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;

import io.github.boxofdog.simpleblock.feature.LightRoutes;
import io.github.boxofdog.simpleblock.feature.ZombieDoors;

/** MOB-02: pursuit-only door breaking for the zombie family (see {@link ZombieDoors}). */
@Mixin(Zombie.class)
abstract class ZombieMixin {
	/** Vanilla's random Hard-mode door breakers would also break doors while wandering through villages. */
	@ModifyVariable(method = "setCanBreakDoors", at = @At("HEAD"), argsOnly = true)
	private boolean simpleBlock$noPassiveDoorBreaking(boolean canBreakDoors) {
		return false;
	}

	/** Zombie, husk and zombie villager; drowned and zombified piglins replace this method. */
	@Inject(method = "addBehaviourGoals", at = @At("TAIL"))
	private void simpleBlock$doorGoals(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		((MobAccessor) self).simpleBlock$getGoalSelector().addGoal(1, new ZombieDoors.PursuitDoorBreakGoal(self));
		((MobAccessor) self).simpleBlock$getGoalSelector().addGoal(1, new ZombieDoors.MetalDoorAttackGoal(self));
	}

	/** Paths may lead through doors only while chasing a player (or, M09, on a light-drawn door route); otherwise zombies stop at closed doors. */
	@Inject(method = "tick", at = @At("HEAD"))
	private void simpleBlock$doorsOnlyInPursuit(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (!self.level().isClientSide() && self.getNavigation().canNavigateGround()) {
			ZombieDoors.trackPursuit(self);
			// M09: or while it takes a light-drawn route through a nearby wooden door.
			self.getNavigation().setCanOpenDoors(ZombieDoors.isPursuingPlayer(self) || LightRoutes.doorRouteActive(self));
		}
	}
}
