package io.github.boxofdog.simpleblock.mixin.mobs;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

/** M09: the target the navigation last searched for, saved round probe searches ({@code feature/PathProbe}). */
@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
	@Accessor("targetPos")
	@Nullable BlockPos simpleBlock$getTargetPos();

	@Accessor("targetPos")
	void simpleBlock$setTargetPos(@Nullable BlockPos targetPos);

	@Accessor("reachRange")
	int simpleBlock$getReachRange();

	@Accessor("reachRange")
	void simpleBlock$setReachRange(int reachRange);
}
