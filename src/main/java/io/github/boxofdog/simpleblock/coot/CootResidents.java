package io.github.boxofdog.simpleblock.coot;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/**
 * M14 natural Old Coots: every abandoned mineshaft a player is in or above (its bounding box, looked up from
 * the structure references of the player's own chunk: no scan of the world; mineshafts overlap, so all of
 * them) is checked every {@link #INTERVAL} ticks against its {@link CootHomes} record. A vacant home gets its
 * first resident soon after anyone first comes near ({@link #FIRST_CHANCE} a check); after a resident dies,
 * nobody for seven days, then only now and then ({@link #RETRY_CHANCE} a check). The resident appears at any
 * light, standing in one of the mineshaft's own pieces (its corridors, crossings and rooms), in a loaded,
 * ticking chunk, out of the way of players.
 */
public final class CootResidents {
	public static final int INTERVAL = 200;
	public static final float FIRST_CHANCE = 0.5F;
	public static final float RETRY_CHANCE = 0.02F;
	/** Not within this many blocks of a player: he is already there when they arrive, never appears in front of them. */
	public static final double PLAYER_CLEARANCE = 16.0;
	private static final int TRIES = 12;

	private CootResidents() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % INTERVAL == 0 && !level.players().isEmpty()) {
				check(level);
			}
		});
	}

	/** One round of checks: every mineshaft each player is in or over (every {@link #INTERVAL} ticks). */
	public static void check(ServerLevel level) {
		Registry<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		HolderSet<Structure> mineshafts = structures.get(StructureTags.MINESHAFT).<HolderSet<Structure>>map(set -> set).orElse(null);
		if (mineshafts == null) {
			return;
		}
		LongSet seen = new LongOpenHashSet();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			BlockPos at = player.blockPosition();
			// Mineshafts overlap: all of those around the player, not just the first found.
			for (StructureStart start : level.structureManager().startsForStructure(SectionPos.blockToSectionCoord(at.getX()),
				SectionPos.blockToSectionCoord(at.getZ()), structure -> mineshafts.contains(structures.wrapAsHolder(structure)))) {
				if (start.isValid() && start.getBoundingBox().isInside(at) && seen.add(start.getChunkPos().pack())) {
					visit(level, start, level.getRandom(), false);
				}
			}
		}
	}

	/**
	 * One check of a mineshaft: spawns its resident if the home is vacant and the roll allows ({@code always}
	 * skips the roll, for tests). Returns the new resident, or null.
	 */
	public static OldCoot visit(ServerLevel level, StructureStart start, RandomSource random, boolean always) {
		CootHomes homes = CootHomes.get(level);
		long key = start.getChunkPos().pack();
		CootHomes.Home home = homes.get(level, key);
		if (!home.vacant(level.getGameTime())) {
			return null;
		}
		float chance = home.died == CootHomes.NEVER ? FIRST_CHANCE : RETRY_CHANCE;
		if (!always && random.nextFloat() >= chance) {
			return null;
		}
		BlockPos at = findSpot(level, start, random);
		if (at == null) {
			return null;
		}
		OldCoot coot = OldCootContent.OLD_COOT.create(level, EntitySpawnReason.NATURAL);
		if (coot == null) {
			return null;
		}
		coot.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		coot.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
		coot.settle(key, at);
		if (!level.addFreshEntity(coot)) {
			return null;
		}
		homes.settle(home, coot.getUUID());
		return coot;
	}

	/** A place to stand inside one of the mineshaft's pieces, in a ticking chunk, clear of players; any light. */
	public static BlockPos findSpot(ServerLevel level, StructureStart start, RandomSource random) {
		List<StructurePiece> pieces = new ArrayList<>(start.getPieces());
		if (pieces.isEmpty()) {
			return null;
		}
		for (int i = 0; i < TRIES; i++) {
			BoundingBox box = pieces.get(random.nextInt(pieces.size())).getBoundingBox();
			int x = box.minX() + random.nextInt(box.getXSpan());
			int z = box.minZ() + random.nextInt(box.getZSpan());
			for (int y = box.minY(); y <= box.maxY(); y++) {
				BlockPos pos = new BlockPos(x, y, z);
				if (standable(level, start, pos)) {
					return pos;
				}
			}
		}
		return null;
	}

	/** Feet here: inside a piece of the mineshaft, loaded and ticking, firm floor, room for him, no fluid, no player near. */
	public static boolean standable(ServerLevel level, StructureStart start, BlockPos pos) {
		if (!level.isPositionEntityTicking(pos) || !level.structureManager().structureHasPieceAt(pos, start)) {
			return false;
		}
		BlockPos below = pos.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
			return false;
		}
		AABB room = OldCootContent.OLD_COOT.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		return level.noCollision(room) && !level.containsAnyLiquid(room)
			&& level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, PLAYER_CLEARANCE, false) == null;
	}

	/** A resident left the world for good (killed, or discarded): his home empties and its wait begins. Unloading is not this. */
	static void departed(ServerLevel level, OldCoot coot, long home) {
		CootHomes.get(level).vacate(home, coot.getUUID(), level.getGameTime());
	}
}
