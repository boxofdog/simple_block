package io.github.boxofdog.simpleblock.coot;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * M14 what makes the Old Coot angry, besides a blow to himself ({@link OldCoot#hurtServer}):
 * <ul>
 * <li>a player breaking ore (coal, copper, iron, gold, redstone, lapis, diamond or emerald, in stone or
 * deepslate) within {@link #EARSHOT} blocks of him: he hears his ore being stolen, through rock;
 * <li>a player hurting a cave spider while he can see that player, within {@link #SIGHT} blocks.
 * </ul>
 */
public final class CootAnger {
	public static final double EARSHOT = 15.0;
	public static final double SIGHT = 16.0;

	private CootAnger() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server) {
				oreTaken(server, player, pos, state);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof CaveSpider && source.getEntity() instanceof Player player && entity.level() instanceof ServerLevel server) {
				spiderHit(server, player);
			}
		});
	}

	/** One of his ores: the vanilla ore tags, so every ore named in the brief and its deepslate twin. */
	public static boolean isOre(BlockState state) {
		return state.is(BlockItemTags.COAL_ORES.block()) || state.is(BlockItemTags.COPPER_ORES.block())
			|| state.is(BlockItemTags.IRON_ORES.block()) || state.is(BlockItemTags.GOLD_ORES.block())
			|| state.is(BlockItemTags.REDSTONE_ORES.block()) || state.is(BlockItemTags.LAPIS_ORES.block())
			|| state.is(BlockItemTags.DIAMOND_ORES.block()) || state.is(BlockItemTags.EMERALD_ORES.block());
	}

	public static void oreTaken(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		if (!isOre(state)) {
			return;
		}
		Vec3 at = Vec3.atCenterOf(pos);
		for (OldCoot coot : level.getEntitiesOfClass(OldCoot.class, new AABB(pos).inflate(EARSHOT))) {
			if (coot.position().add(0.0, coot.getBbHeight() / 2.0, 0.0).distanceTo(at) <= EARSHOT) {
				coot.provoke(player);
			}
		}
	}

	public static void spiderHit(ServerLevel level, Player player) {
		for (OldCoot coot : level.getEntitiesOfClass(OldCoot.class, player.getBoundingBox().inflate(SIGHT))) {
			if (coot.distanceTo(player) <= SIGHT && coot.hasLineOfSight(player)) {
				coot.provoke(player);
			}
		}
	}
}
