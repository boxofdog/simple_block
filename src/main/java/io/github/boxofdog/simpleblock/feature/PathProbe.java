package io.github.boxofdog.simpleblock.feature;

import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import io.github.boxofdog.simpleblock.mixin.mobs.PathNavigationAccessor;

/**
 * M09: a path search that only looks ("could it get there?") without disturbing where the mob is
 * going. Vanilla's {@code PathNavigation.createPath} remembers each searched target and hands back
 * the path being followed when asked for that target again; a probe that left its target behind
 * would make the next real search for it return the mob's old path. Probes search afresh and
 * restore the navigation's remembered target.
 */
public final class PathProbe {
	/** A remembered target no search ever asks for (null would break vanilla's {@code Set.contains} check). */
	private static final BlockPos NOWHERE = new BlockPos(Integer.MAX_VALUE / 2, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);

	private PathProbe() {
	}

	public static @Nullable Path probe(Mob mob, Function<PathNavigation, @Nullable Path> search) {
		PathNavigation navigation = mob.getNavigation();
		PathNavigationAccessor accessor = (PathNavigationAccessor) navigation;
		BlockPos target = accessor.simpleBlock$getTargetPos();
		int reach = accessor.simpleBlock$getReachRange();
		accessor.simpleBlock$setTargetPos(NOWHERE);
		try {
			return search.apply(navigation);
		} finally {
			accessor.simpleBlock$setTargetPos(target);
			accessor.simpleBlock$setReachRange(reach);
		}
	}

	/**
	 * A real search (to be followed) that never gets back the path already being followed: vanilla
	 * returns that path when asked again for the same target, even when it was cut short by the
	 * search range and the mob is now much closer.
	 */
	public static @Nullable Path fresh(Mob mob, Function<PathNavigation, @Nullable Path> search) {
		((PathNavigationAccessor) mob.getNavigation()).simpleBlock$setTargetPos(NOWHERE);
		return search.apply(mob.getNavigation());
	}

	/**
	 * A probe to exactly {@code pos}. Ground navigation's own {@code createPath(BlockPos, int)} moves a
	 * solid target (a door counts) up its column to the first open space, which for a door in a wall
	 * is the roof; the set form searches for the block as given.
	 */
	public static @Nullable Path probe(Mob mob, BlockPos pos, int reach) {
		return probe(mob, navigation -> exact(navigation, pos, reach));
	}

	/** A path to exactly {@code pos} (see {@link #probe(Mob, BlockPos, int)}), for real moves. */
	public static @Nullable Path exact(PathNavigation navigation, BlockPos pos, int reach) {
		return navigation.createPath(Set.of(pos), reach);
	}
}
