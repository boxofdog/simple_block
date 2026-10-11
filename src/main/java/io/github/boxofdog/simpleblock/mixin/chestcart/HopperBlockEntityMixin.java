package io.github.boxofdog.simpleblock.mixin.chestcart;

import java.util.List;
import java.util.function.Predicate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FRT-11: hoppers, hopper minecarts, droppers and crafters find containers in the world through
 * {@code HopperBlockEntity.getEntityContainer}; chest minecarts are left out of what it finds, so
 * automation can neither fill nor empty one.
 */
@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
	@WrapOperation(
			method = "getEntityContainer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;")
	)
	private static List<Entity> simple_block$skipChestMinecarts(Level level, Entity except, AABB box, Predicate<? super Entity> selector, Operation<List<Entity>> original) {
		return original.call(level, except, box, selector.and(entity -> !(entity instanceof MinecartChest)));
	}
}
