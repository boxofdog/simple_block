package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.coot.CootAnger;
import io.github.boxofdog.simpleblock.coot.CootHomes;
import io.github.boxofdog.simpleblock.coot.CootMineGoal;
import io.github.boxofdog.simpleblock.coot.CootResidents;
import io.github.boxofdog.simpleblock.coot.OldCoot;
import io.github.boxofdog.simpleblock.coot.OldCootContent;

/**
 * M14 the Old Coot on the server. Each test builds its own block of rock in a large empty test area, since
 * game-test worlds are superflat and hold no mineshafts: the natural-home test stands a real
 * {@code minecraft:mineshaft} structure start over a carved corridor, registered in the chunks as world
 * generation would.
 */
public class OldCootGameTests {
	private static final String CAVERN = "simple_block-gametest:coot_cavern";

	/** A solid block of stone (corners inclusive). */
	private static void rock(ServerLevel level, BlockPos min, BlockPos max, Block block) {
		fill(level, min, max, block);
	}

	private static void fill(ServerLevel level, BlockPos min, BlockPos max, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private static void carve(ServerLevel level, BlockPos min, BlockPos max) {
		fill(level, min, max, Blocks.AIR);
	}

	/** A piece of a mineshaft: just its box (these never generate anything). */
	private static final class Piece extends StructurePiece {
		Piece(BoundingBox box) {
			super(StructurePieceType.MINE_SHAFT_CORRIDOR, 0, box);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		}
	}

	/** A mineshaft start in the given chunk made of these pieces, registered in the level's chunks as generation would. */
	private static StructureStart mineshaft(ServerLevel level, ChunkPos start, BoundingBox... boxes) {
		Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(BuiltinStructures.MINESHAFT);
		List<StructurePiece> pieces = new ArrayList<>();
		for (BoundingBox box : boxes) {
			pieces.add(new Piece(box));
		}
		StructureStart mineshaft = new StructureStart(structure, start, 0, new PiecesContainer(pieces));
		level.structureManager().setStartForStructure(structure, mineshaft, level.getChunk(start.x(), start.z()));
		BoundingBox all = mineshaft.getBoundingBox();
		for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
			for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
				level.structureManager().addReferenceForStructure(structure, start.pack(), level.getChunk(cx, cz));
			}
		}
		return mineshaft;
	}

	/** A start chunk near (cx, cz) whose mineshaft is (or is not) a home under this world's seed. */
	private static ChunkPos startChunk(ServerLevel level, int cx, int cz, boolean home) {
		for (int i = 0; ; i++) {
			ChunkPos pos = new ChunkPos(cx + i, cz);
			if (CootHomes.isHome(level.getSeed(), pos.pack()) == home) {
				return pos;
			}
		}
	}

	private static OldCoot egg(ServerLevel level, BlockPos pos) {
		return OldCootContent.OLD_COOT.spawn(level, pos, EntitySpawnReason.SPAWN_ITEM_USE);
	}

	/**
	 * About two mineshafts in three are homes, decided by the seed: 3000 mineshafts come out within 3 points of
	 * two thirds, the same every time, and a mineshaft's record keeps its choice (made once, when first met).
	 */
	@GameTest
	public void homesAreChosenOnceAboutTwoInThree(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int homes = 0;
		int total = 3000;
		for (int i = 0; i < total; i++) {
			long key = ChunkPos.pack(i * 37 - 50000, i * -11 + 7000);
			boolean home = CootHomes.isHome(level.getSeed(), key);
			helper.assertTrue(home == CootHomes.isHome(level.getSeed(), key), "the choice is fixed");
			homes += home ? 1 : 0;
		}
		float share = homes / (float) total;
		CootHomes ledger = CootHomes.get(level);
		long key = ChunkPos.pack(-77777, 77777);
		ledger.forget(key);
		CootHomes.Home first = ledger.get(level, key);
		CootHomes.Home again = ledger.get(level, key);
		SimpleBlockTestLog.info("M14 HOMES: {} of {} mineshafts are homes ({})", homes, total, share);
		helper.assertTrue(Math.abs(share - 2.0F / 3.0F) < 0.03F, "about two in three mineshafts are homes: " + share);
		helper.assertTrue(first == again && first.home == CootHomes.isHome(level.getSeed(), key), "a mineshaft's record is made once");
		ledger.forget(key);
		helper.succeed();
	}

	/**
	 * Natural residents: two mineshaft corridors carved in rock, one lit and one dark, with a cave beside them
	 * that is not part of the mineshaft. The level's own structure lookup finds the mineshaft from inside
	 * it and not from outside. Spawn spots are always in the corridor, lit or dark alike, never in the
	 * cave (which is a fine spot once a mineshaft's piece covers it: the negative control). A home gets one
	 * resident and no second; an unloaded resident still holds it; an egg-spawned Coot in the corridor
	 * neither counts nor changes anything, alive or dead. When the resident dies the home waits seven days,
	 * then fills only by chance; a mineshaft that is no home never gets one. Resident and ledger survive a
	 * save and load.
	 */
	@GameTest(structure = CAVERN, maxTicks = 200)
	public void naturalResidentsOnePerMineshaft(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int x0 = origin.getX();
		int Y = origin.getY();
		int z0 = origin.getZ();
		rock(level, new BlockPos(x0, Y, z0), new BlockPos(x0 + 23, Y + 6, z0 + 23), Blocks.STONE);
		// Two corridors of the mineshaft, three wide and three high on a floor at Y, sealed from each other:
		// one lit by a glowstone ceiling, one in darkness.
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 4), new BlockPos(x0 + 19, Y + 3, z0 + 6));
		fill(level, new BlockPos(x0 + 2, Y + 4, z0 + 4), new BlockPos(x0 + 19, Y + 4, z0 + 6), Blocks.GLOWSTONE);
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 10), new BlockPos(x0 + 19, Y + 3, z0 + 12));
		// A cave beside it, outside the mineshaft.
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 16), new BlockPos(x0 + 10, Y + 2, z0 + 17));
		BoundingBox litCorridor = new BoundingBox(x0 + 2, Y, z0 + 4, x0 + 19, Y + 3, z0 + 6);
		BoundingBox corridor = new BoundingBox(x0 + 2, Y, z0 + 10, x0 + 19, Y + 3, z0 + 12);
		BoundingBox caveBox = new BoundingBox(x0 + 2, Y, z0 + 16, x0 + 10, Y + 2, z0 + 17);
		ChunkPos homeChunk = startChunk(level, x0 >> 4, z0 >> 4, true);
		ChunkPos vacantChunk = startChunk(level, (x0 >> 4) + 40, z0 >> 4, false);
		CootHomes ledger = CootHomes.get(level);
		ledger.forget(homeChunk.pack());
		ledger.forget(vacantChunk.pack());
		List<String> log = new ArrayList<>();
		helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getBrightness(LightLayer.BLOCK, new BlockPos(x0 + 10, Y + 1, z0 + 5)) > 0,
			"waiting for the light to spread")).thenExecute(() -> {
			StructureStart home = mineshaft(level, homeChunk, litCorridor, corridor);
			StructureStart cave = new StructureStart(home.getStructure(), new ChunkPos(0, 0), 0, new PiecesContainer(List.of(new Piece(caveBox))));
			var mineshafts = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(net.minecraft.tags.StructureTags.MINESHAFT);
			StructureStart found = level.structureManager().getStructureAt(new BlockPos(x0 + 5, Y + 1, z0 + 11), mineshafts);
			StructureStart outside = level.structureManager().getStructureAt(new BlockPos(x0 + 5, Y + 1, z0 + 30), mineshafts);
			helper.assertTrue(found.isValid() && found.getChunkPos().equals(homeChunk), "the level finds the mineshaft from inside it");
			helper.assertTrue(!outside.isValid(), "and not from outside it");

			// Spawn spots: always in the corridors, lit and dark alike, never in the cave.
			RandomSource random = RandomSource.create(14L);
			int lit = 0;
			int dark = 0;
			for (int i = 0; i < 200; i++) {
				BlockPos spot = CootResidents.findSpot(level, home, random);
				helper.assertTrue(spot != null && (corridor.isInside(spot) || litCorridor.isInside(spot)) && spot.getY() == Y + 1,
					"spot " + spot + " in a corridor");
				if (level.getBrightness(LightLayer.BLOCK, spot) > 0) {
					lit++;
				} else {
					dark++;
				}
			}
			BlockPos inCave = new BlockPos(x0 + 5, Y + 1, z0 + 16);
			helper.assertTrue(!CootResidents.standable(level, home, inCave), "the cave is not the mineshaft's");
			helper.assertTrue(CootResidents.standable(level, cave, inCave), "negative control: a mineshaft piece over the cave makes it a spot");
			log.add("spots lit " + lit + ", dark " + dark);
			helper.assertTrue(lit > 20 && dark > 20, "spots lit and dark alike: " + lit + " lit, " + dark + " dark");

			// One resident.
			long gameTime = level.getGameTime();
			OldCoot resident = CootResidents.visit(level, home, random, true);
			helper.assertTrue(resident != null && resident.home() == homeChunk.pack() && resident.isPersistenceRequired(), "a resident moves in");
			CootHomes.Home record = ledger.find(homeChunk.pack());
			helper.assertTrue(record.home && resident.getUUID().equals(record.resident), "the record holds him");
			helper.assertTrue(CootResidents.visit(level, home, random, true) == null, "no second resident");
			resident.onRemoval(Entity.RemovalReason.UNLOADED_TO_CHUNK);
			helper.assertTrue(resident.getUUID().equals(record.resident) && CootResidents.visit(level, home, random, true) == null,
				"unloaded is still living: the home stays his");

			// An egg-spawned Coot in the corridor is no resident and changes nothing.
			OldCoot visitor = egg(level, new BlockPos(x0 + 15, Y + 1, z0 + 11));
			helper.assertTrue(visitor != null && visitor.home() == OldCoot.NO_HOME && visitor.getMainHandItem().is(Items.IRON_PICKAXE), "an egg Coot");
			visitor.kill(level);
			helper.assertTrue(resident.getUUID().equals(record.resident) && record.died == CootHomes.NEVER, "the egg Coot's death touches no record");

			// Save and load: his home and anchor, and the ledger.
			TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			resident.saveWithoutId(out);
			OldCoot copy = OldCootContent.OLD_COOT.create(level, EntitySpawnReason.LOAD);
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
			helper.assertTrue(copy.home() == resident.home() && resident.anchor().equals(copy.anchor()) && copy.isPersistenceRequired(),
				"he keeps his home through a save");
			Tag saved = CootHomes.TYPE.codec().encodeStart(NbtOps.INSTANCE, ledger).getOrThrow();
			CootHomes loaded = CootHomes.TYPE.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
			helper.assertTrue(resident.getUUID().equals(loaded.find(homeChunk.pack()).resident) && loaded.find(homeChunk.pack()).home,
				"the ledger keeps him through a save");

			// His death: seven days' wait, then only by chance.
			resident.kill(level);
			helper.assertTrue(record.resident == null && record.died >= gameTime, "his death empties the home and starts the wait");
			helper.assertTrue(CootResidents.visit(level, home, random, true) == null, "nobody new within seven days");
			record.died = level.getGameTime() - CootHomes.COOLDOWN + 1;
			helper.assertTrue(CootResidents.visit(level, home, random, true) == null, "nor a tick short of seven days");
			record.died = level.getGameTime() - CootHomes.COOLDOWN;
			int misses = 0;
			OldCoot second = null;
			for (int i = 0; i < 400 && second == null; i++) {
				second = CootResidents.visit(level, home, random, false);
				misses += second == null ? 1 : 0;
			}
			log.add("after the wait, " + misses + " checks missed before a new resident");
			helper.assertTrue(second != null && misses >= 5, "after the wait a new resident comes, but only by chance: " + misses + " misses");

			// A mineshaft that is no home stays empty, however often it is checked.
			StructureStart vacant = new StructureStart(home.getStructure(), vacantChunk, 0, new PiecesContainer(List.of(new Piece(corridor))));
			for (int i = 0; i < 20; i++) {
				helper.assertTrue(CootResidents.visit(level, vacant, random, true) == null, "no resident in a mineshaft that is no home");
			}
			helper.assertTrue(!ledger.find(vacantChunk.pack()).home, "recorded as no home");
			second.discard();
			ledger.forget(homeChunk.pack());
			ledger.forget(vacantChunk.pack());
			SimpleBlockTestLog.info("M14 NATURAL RESIDENTS: {}", log);
			helper.succeed();
		});
	}

	/**
	 * The live check: a player standing at one end of a long mineshaft corridor (a home) brings its resident,
	 * who appears in the corridor at least 16 blocks from the player; however many checks follow, he stays the
	 * only one. A player in the rock outside any mineshaft brings nobody.
	 */
	@GameTest(structure = CAVERN, maxTicks = 100)
	public void aPlayerInAMineshaftBringsItsResident(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int x0 = origin.getX();
		int Y = origin.getY();
		int z0 = origin.getZ();
		rock(level, new BlockPos(x0, Y, z0), new BlockPos(x0 + 63, Y + 5, z0 + 12), Blocks.STONE);
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 4), new BlockPos(x0 + 60, Y + 3, z0 + 6));
		BoundingBox corridor = new BoundingBox(x0 + 2, Y, z0 + 4, x0 + 60, Y + 3, z0 + 6);
		ChunkPos homeChunk = startChunk(level, (x0 >> 4) - 60, z0 >> 4, true);
		CootHomes ledger = CootHomes.get(level);
		ledger.forget(homeChunk.pack());
		AABB around = new AABB(Vec3.atCenterOf(new BlockPos(x0, Y, z0)), Vec3.atCenterOf(new BlockPos(x0 + 63, Y + 5, z0 + 12))).inflate(2);
		helper.startSequence().thenIdle(1).thenExecute(() -> {
			mineshaft(level, homeChunk, corridor);
			ServerPlayer outside = TestPlayers.create(helper, GameType.SURVIVAL, BlockPos.ZERO);
			outside.teleportTo(x0 + 30.5, Y + 9, z0 + 30.5);
			for (int i = 0; i < 50; i++) {
				CootResidents.check(level);
			}
			helper.assertTrue(level.getEntitiesOfClass(OldCoot.class, around).isEmpty() && ledger.find(homeChunk.pack()) == null,
				"a player outside the mineshaft brings nobody, and the mineshaft is not even looked at");
			ServerPlayer explorer = TestPlayers.create(helper, GameType.SURVIVAL, BlockPos.ZERO);
			explorer.teleportTo(x0 + 3.5, Y + 1, z0 + 5.5);
			int checks = 0;
			while (level.getEntitiesOfClass(OldCoot.class, around).isEmpty() && checks < 400) {
				CootResidents.check(level);
				checks++;
			}
			long t0 = System.nanoTime();
			for (int i = 0; i < 1000; i++) {
				CootResidents.check(level);
			}
			double checkMicros = (System.nanoTime() - t0) / 1000 / 1e3;
			List<OldCoot> coots = level.getEntitiesOfClass(OldCoot.class, around);
			CootMineGoal look = new CootMineGoal(coots.getFirst());
			t0 = System.nanoTime();
			for (int i = 0; i < 1000; i++) {
				coots.getFirst().mineCooldown = 0;
				look.canUse();
			}
			double lookMicros = (System.nanoTime() - t0) / 1000 / 1e3;
			SimpleBlockTestLog.info("M14 COST: a check round (one player in a mineshaft) {} us; a look for a face to dig {} us", checkMicros, lookMicros);
			SimpleBlockTestLog.info("M14 LIVE CHECK: resident after {} checks, {} Coots after 1000 more{}", checks, coots.size(),
				coots.isEmpty() ? "" : ", " + coots.getFirst().distanceTo(explorer) + " blocks from the player");
			helper.assertTrue(coots.size() == 1, "exactly one resident: " + coots.size());
			OldCoot resident = coots.getFirst();
			helper.assertTrue(corridor.isInside(resident.blockPosition()) && resident.distanceTo(explorer) >= CootResidents.PLAYER_CLEARANCE
				&& resident.home() == homeChunk.pack(), "in the corridor, out of the player's way, and of this home");
			resident.discard();
			ledger.forget(homeChunk.pack());
			TestPlayers.remove(outside);
			TestPlayers.remove(explorer);
			helper.succeed();
		});
	}

	/**
	 * Mining: an egg-spawned Coot in a small room cut in rock digs real passages into its stone and deepslate
	 * walls (each blow a swing of his arm) with nothing dropped and no experience. Coal ore, cobblestone and
	 * a stone block with water behind it stay; nothing below his feet or above head height goes. The rules
	 * themselves: ore, cobbled deepslate and stone beyond his territory are not to be dug; stone and
	 * deepslate are. Torches are capped for good.
	 */
	@GameTest(structure = CAVERN, maxTicks = 4000)
	public void minesOnlyStoneAndDeepslateWithNoDrops(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int x0 = origin.getX();
		int Y = origin.getY();
		int z0 = origin.getZ();
		BlockPos min = new BlockPos(x0, Y, z0);
		BlockPos max = new BlockPos(x0 + 40, Y + 6, z0 + 18);
		rock(level, min, max, Blocks.STONE);
		carve(level, new BlockPos(x0 + 7, Y + 1, z0 + 7), new BlockPos(x0 + 11, Y + 3, z0 + 11));
		fill(level, new BlockPos(x0 + 12, Y + 1, z0 + 7), new BlockPos(x0 + 14, Y + 2, z0 + 11), Blocks.DEEPSLATE);
		BlockPos coal = new BlockPos(x0 + 6, Y + 1, z0 + 9);
		BlockPos cobble = new BlockPos(x0 + 6, Y + 2, z0 + 9);
		BlockPos dammed = new BlockPos(x0 + 9, Y + 1, z0 + 6);
		level.setBlockAndUpdate(coal, Blocks.COAL_ORE.defaultBlockState());
		level.setBlockAndUpdate(cobble, Blocks.COBBLESTONE.defaultBlockState());
		level.setBlockAndUpdate(dammed.north(), Blocks.WATER.defaultBlockState());
		// A dark gallery for the torch checks, its far end lit (out of his territory's way of digging: no stone face in reach).
		BlockPos darkSpot = new BlockPos(x0 + 20, Y + 1, z0 + 3);
		BlockPos litSpot = new BlockPos(x0 + 37, Y + 1, z0 + 3);
		carve(level, new BlockPos(x0 + 19, Y + 1, z0 + 2), new BlockPos(x0 + 38, Y + 2, z0 + 4));
		fill(level, new BlockPos(x0 + 37, Y + 3, z0 + 3), new BlockPos(x0 + 37, Y + 3, z0 + 3), Blocks.GLOWSTONE);
		Map<BlockPos, BlockState> before = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			before.put(pos.immutable(), level.getBlockState(pos));
		}
		OldCoot[] coot = new OldCoot[1];
		boolean[] swung = new boolean[1];
		helper.startSequence().thenIdle(1).thenExecute(() -> {
			coot[0] = egg(level, new BlockPos(x0 + 9, Y + 1, z0 + 9));
			coot[0].mineCooldown = 0;
		}).thenWaitUntil(() -> {
			swung[0] |= coot[0].isSwinging();
			helper.assertTrue(coot[0].blocksMined >= 6, "mining: " + coot[0].passages + " passages, " + coot[0].blocksMined + " blocks so far, at "
				+ coot[0].blockPosition().subtract(new BlockPos(x0, Y, z0)));
		}).thenExecute(() -> {
			OldCoot c = coot[0];
			List<String> removed = new ArrayList<>();
			for (Map.Entry<BlockPos, BlockState> entry : before.entrySet()) {
				BlockState now = level.getBlockState(entry.getKey());
				if (!entry.getValue().isAir() && now.isAir()) {
					BlockPos pos = entry.getKey();
					removed.add(pos.getX() - x0 + "," + (pos.getY() - Y) + "," + (pos.getZ() - z0) + " " + entry.getValue().getBlock());
					helper.assertTrue(CootMineGoal.mineable(entry.getValue()), "only stone and deepslate dug: " + entry.getValue() + " at " + pos);
					helper.assertTrue(pos.getY() == Y + 1 || pos.getY() == Y + 2, "nothing dug below his feet or above his head: " + pos);
				}
			}
			AABB site = new AABB(Vec3.atCenterOf(min), Vec3.atCenterOf(max)).inflate(4);
			int items = level.getEntitiesOfClass(ItemEntity.class, site).size();
			int orbs = level.getEntitiesOfClass(ExperienceOrb.class, site).size();
			SimpleBlockTestLog.info("M14 MINING: {} passages begun, {} blocks mined, swung {}, removed {}, items {}, orbs {}", c.passages, c.blocksMined,
				swung[0], removed, items, orbs);
			helper.assertTrue(swung[0], "his arm swings as he mines");
			helper.assertTrue(removed.size() == c.blocksMined, "every block he mined is gone, and nothing else: " + removed);
			helper.assertTrue(items == 0 && orbs == 0, "no drops and no experience: " + items + " items, " + orbs + " orbs");
			helper.assertTrue(level.getBlockState(coal).is(Blocks.COAL_ORE) && level.getBlockState(cobble).is(Blocks.COBBLESTONE)
				&& level.getBlockState(dammed).is(Blocks.STONE), "ore, cobblestone and the dam stay");

			// The rules on their own.
			helper.assertTrue(!CootMineGoal.diggable(level, c, coal) && !CootMineGoal.diggable(level, c, dammed), "ore and a dam are never dug");
			BlockPos cobbledDeepslate = new BlockPos(x0 + 3, Y + 1, z0 + 3);
			level.setBlockAndUpdate(cobbledDeepslate, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
			helper.assertTrue(!CootMineGoal.diggable(level, c, cobbledDeepslate), "cobbled deepslate is never dug");
			helper.assertTrue(CootMineGoal.diggable(level, c, new BlockPos(x0 + 3, Y + 2, z0 + 3)), "stone in his territory is his to dig");
			BlockPos deepslate = new BlockPos(x0 + 3, Y + 2, z0 + 15);
			level.setBlockAndUpdate(deepslate, Blocks.DEEPSLATE.defaultBlockState());
			helper.assertTrue(CootMineGoal.diggable(level, c, deepslate), "and so is deepslate");
			BlockPos far = c.anchor().east(OldCoot.TERRITORY + 2);
			level.setBlockAndUpdate(far, Blocks.STONE.defaultBlockState());
			helper.assertTrue(!CootMineGoal.diggable(level, c, far), "stone beyond his territory is not his to dig");
			// Torches: a real one on a dark floor; not in the light, not on an occupied spot, not again too soon,
			// and never past the lifetime cap.
			boolean lit = CootMineGoal.placeTorch(level, c, litSpot);
			boolean placed = CootMineGoal.placeTorch(level, c, darkSpot);
			boolean again = CootMineGoal.placeTorch(level, c, darkSpot.east());
			SimpleBlockTestLog.info("M14 TORCHES: in the light {}, in the dark {}, straight after {}; {} left", lit, placed, again, c.torchesLeft());
			helper.assertTrue(!lit && placed && level.getBlockState(darkSpot).is(Blocks.TORCH) && !again,
				"a torch in the dark only, and not twice in a row: light " + lit + ", dark " + placed + ", again " + again);
			helper.assertTrue(c.torchesLeft() == OldCoot.TORCH_CAP - 1, "torches counted");
			while (c.torchesLeft() > 0) {
				c.torchPlaced();
			}
			helper.assertTrue(!c.mayPlaceTorch(), "no more torches once the cap is reached");
			c.discard();
			helper.succeed();
		});
	}

	private static boolean angry(OldCoot coot, ServerPlayer player) {
		return coot.isAngry() && coot.getTarget() == player;
	}

	/**
	 * Anger: ore broken 14 blocks away through solid rock angers him (coal, then deepslate copper for another
	 * Coot); stone broken beside him and ore broken 18 blocks away do not. A cave spider hit before his eyes
	 * angers him; one hit behind a wall does not. A blow to him angers him. Every vanilla ore and its
	 * deepslate twin counts; stone, deepslate and cobblestone do not.
	 */
	@GameTest(structure = CAVERN, maxTicks = 200)
	public void angerAtOreTheftSpiderHitsAndBlows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int x0 = origin.getX();
		int Y = origin.getY();
		int z0 = origin.getZ();
		rock(level, new BlockPos(x0, Y, z0), new BlockPos(x0 + 40, Y + 6, z0 + 30), Blocks.STONE);
		// Sealed cells for the Coots: near (14 from the ore), far (18), and one for deepslate copper.
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 2), new BlockPos(x0 + 2, Y + 2, z0 + 2));
		carve(level, new BlockPos(x0 + 34, Y + 1, z0 + 2), new BlockPos(x0 + 34, Y + 2, z0 + 2));
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 8), new BlockPos(x0 + 2, Y + 2, z0 + 8));
		BlockPos coal = new BlockPos(x0 + 16, Y + 1, z0 + 2);
		BlockPos copper = new BlockPos(x0 + 16, Y + 1, z0 + 8);
		BlockPos stone = new BlockPos(x0 + 3, Y + 1, z0 + 2);
		// An open hall for the spider: the Coot with a clear view, another behind a wall.
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 14), new BlockPos(x0 + 30, Y + 4, z0 + 28));
		fill(level, new BlockPos(x0 + 20, Y + 1, z0 + 14), new BlockPos(x0 + 20, Y + 4, z0 + 28), Blocks.STONE);
		List<String> log = new ArrayList<>();
		helper.startSequence().thenIdle(1).thenExecute(() -> {
			level.setBlockAndUpdate(coal, Blocks.COAL_ORE.defaultBlockState());
			level.setBlockAndUpdate(copper, Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState());
			OldCoot near = egg(level, new BlockPos(x0 + 2, Y + 1, z0 + 2));
			OldCoot farAway = egg(level, new BlockPos(x0 + 34, Y + 1, z0 + 2));
			OldCoot copperCoot = egg(level, new BlockPos(x0 + 2, Y + 1, z0 + 8));
			ServerPlayer thief = TestPlayers.create(helper, GameType.SURVIVAL, BlockPos.ZERO);
			thief.teleportTo(coal.getX() + 0.5, Y + 7, coal.getZ() + 0.5);
			thief.gameMode.destroyBlock(stone);
			helper.assertTrue(!near.isAngry() && level.getBlockState(stone).isAir(), "breaking stone beside him is no theft");
			thief.gameMode.destroyBlock(coal);
			helper.assertTrue(level.getBlockState(coal).isAir(), "the coal is mined");
			log.add("coal 14 away: near " + near.isAngry() + ", 18 away " + farAway.isAngry() + ", copper Coot " + copperCoot.isAngry());
			helper.assertTrue(angry(near, thief), "ore mined 14 blocks away through rock angers him");
			helper.assertTrue(!farAway.isAngry(), "ore mined 18 blocks away does not");
			helper.assertTrue(!copperCoot.isAngry(), "coal mined 15.2 blocks from the copper Coot does not anger him");
			thief.gameMode.destroyBlock(copper);
			helper.assertTrue(angry(copperCoot, thief), "deepslate copper ore counts too");

			for (Block ore : List.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.IRON_ORE,
				Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
				Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.EMERALD_ORE,
				Blocks.DEEPSLATE_EMERALD_ORE)) {
				helper.assertTrue(CootAnger.isOre(ore.defaultBlockState()), ore + " is his ore");
			}
			for (Block rock : List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.COBBLESTONE, Blocks.ANDESITE)) {
				helper.assertTrue(!CootAnger.isOre(rock.defaultBlockState()), rock + " is not ore");
			}

			// Cave spiders.
			OldCoot watcher = egg(level, new BlockPos(x0 + 4, Y + 1, z0 + 20));
			OldCoot walled = egg(level, new BlockPos(x0 + 24, Y + 1, z0 + 20));
			ServerPlayer hunter = TestPlayers.create(helper, GameType.SURVIVAL, BlockPos.ZERO);
			hunter.teleportTo(x0 + 12.5, Y + 1, z0 + 20.5);
			var spider = EntityTypes.CAVE_SPIDER.spawn(level, new BlockPos(x0 + 13, Y + 1, z0 + 20), EntitySpawnReason.MOB_SUMMONED);
			spider.setNoAi(true);
			helper.assertTrue(watcher.hasLineOfSight(hunter) && !walled.hasLineOfSight(hunter), "one sees the hunter, the other cannot");
			hunter.attack(spider);
			log.add("spider hit: watcher " + watcher.isAngry() + ", walled " + walled.isAngry() + ", spider health " + spider.getHealth());
			helper.assertTrue(spider.getHealth() < spider.getMaxHealth(), "the spider is hit");
			helper.assertTrue(angry(watcher, hunter), "a cave spider hit before his eyes angers him");
			helper.assertTrue(!walled.isAngry(), "one hit out of his sight does not");

			// A blow.
			hunter.resetAttackStrengthTicker();
			hunter.attack(walled);
			helper.assertTrue(angry(walled, hunter), "a blow to him angers him");
			SimpleBlockTestLog.info("M14 ANGER: {}", log);
			for (OldCoot c : List.of(near, farAway, copperCoot, watcher, walled)) {
				c.discard();
			}
			spider.discard();
			TestPlayers.remove(thief);
			TestPlayers.remove(hunter);
			helper.succeed();
		});
	}

	/**
	 * The chase: angry, he hits like a player's iron pickaxe (4, with the swing), chases at a spider's pace and
	 * stays angry when the player goes behind a wall; when they get 45 blocks off he gives up and is calm.
	 * Through it all the monsters in his hall (zombie, skeleton, creeper, spider, cave spider) never take
	 * him for a target, nor he them. His health is a villager's.
	 */
	@GameTest(structure = CAVERN, maxTicks = 600)
	public void chasesHitsAndCalmsDownWithMonstersNeutral(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int x0 = origin.getX();
		int Y = origin.getY();
		int z0 = origin.getZ();
		rock(level, new BlockPos(x0, Y, z0), new BlockPos(x0 + 60, Y + 6, z0 + 20), Blocks.STONE);
		carve(level, new BlockPos(x0 + 2, Y + 1, z0 + 2), new BlockPos(x0 + 58, Y + 3, z0 + 18));
		// The monsters' pen: glass, so they see the Coot and the player but cannot reach or shoot them.
		fill(level, new BlockPos(x0 + 3, Y + 1, z0 + 13), new BlockPos(x0 + 17, Y + 3, z0 + 18), Blocks.GLASS);
		carve(level, new BlockPos(x0 + 4, Y + 1, z0 + 14), new BlockPos(x0 + 16, Y + 3, z0 + 17));
		List<Mob> monsters = new ArrayList<>();
		OldCoot[] coot = new OldCoot[1];
		ServerPlayer[] player = new ServerPlayer[1];
		float[] health = new float[1];
		boolean[] swung = new boolean[1];
		helper.startSequence().thenIdle(1).thenExecute(() -> {
			coot[0] = egg(level, new BlockPos(x0 + 10, Y + 1, z0 + 10));
			for (EntityType<? extends Mob> type : List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.SPIDER,
				EntityTypes.CAVE_SPIDER)) {
				monsters.add(type.spawn(level, new BlockPos(x0 + 6 + monsters.size() * 2, Y + 1, z0 + 15), EntitySpawnReason.MOB_SUMMONED));
			}
		}).thenIdle(60).thenExecute(() -> {
			// (Held-item attribute bonuses apply from his first tick.)
			OldCoot c = coot[0];
			helper.assertTrue(c.getMaxHealth() == 20.0F, "a villager's health");
			helper.assertTrue(Math.abs(c.getAttributeValue(Attributes.ATTACK_DAMAGE) - 4.0) < 1e-6, "a player's iron-pickaxe hit: "
				+ c.getAttributeValue(Attributes.ATTACK_DAMAGE));
			helper.assertTrue(Math.abs(c.getAttributeValue(Attributes.MOVEMENT_SPEED) * OldCoot.CHASE - 0.3) < 1e-6, "a spider's pace in the chase");
			for (Mob monster : monsters) {
				helper.assertTrue(monster.getTarget() != coot[0], monster + " leaves him alone");
			}
			helper.assertTrue(coot[0].getTarget() == null, "he leaves them alone");
			player[0] = TestPlayers.create(helper, GameType.SURVIVAL, BlockPos.ZERO);
			player[0].teleportTo(x0 + 12.5, Y + 1, z0 + 10.5);
			player[0].setHealth(20.0F);
			health[0] = player[0].getHealth();
			coot[0].provoke(player[0]);
		}).thenWaitUntil(() -> {
			swung[0] |= coot[0].isSwinging();
			for (Mob monster : monsters) {
				helper.assertTrue(monster.getTarget() != coot[0], monster + " leaves him alone during the fight");
			}
			helper.assertTrue(player[0].getHealth() < health[0], "he hits the player");
		}).thenExecute(() -> {
			float dealt = health[0] - player[0].getHealth();
			SimpleBlockTestLog.info("M14 CHASE: hit for {} on {}, swung {}", dealt, level.getDifficulty(), swung[0]);
			helper.assertTrue(swung[0], "his arm swings as he hits");
			helper.assertTrue(level.getDifficulty() != net.minecraft.world.Difficulty.NORMAL || Math.abs(dealt - 4.0F) < 1e-3, "a 4-point hit: " + dealt);
			// Behind a wall (out of sight) he stays angry.
			fill(level, new BlockPos(x0 + 20, Y + 1, z0 + 2), new BlockPos(x0 + 20, Y + 3, z0 + 18), Blocks.STONE);
			player[0].teleportTo(x0 + 24.5, Y + 1, z0 + 10.5);
		}).thenIdle(40).thenExecute(() -> {
			helper.assertTrue(coot[0].isAngry(), "out of sight is not out of mind");
			for (Mob monster : monsters) {
				helper.assertTrue(coot[0].getTarget() != monster, "he never targets a monster");
			}
			player[0].teleportTo(x0 + 56.5, Y + 1, z0 + 10.5);
			coot[0].teleportTo(x0 + 10.5, Y + 1, z0 + 10.5);
		}).thenIdle(5).thenExecute(() -> {
			helper.assertTrue(!coot[0].isAngry() && coot[0].getTarget() == null, "45 blocks off, the player has got away and he is calm");
			coot[0].discard();
			monsters.forEach(Entity::discard);
			TestPlayers.remove(player[0]);
			helper.succeed();
		});
	}

	/**
	 * Death: 1-3 emeralds and exactly one iron pickaxe with 5-50% of its durability left, once (another blow to
	 * the body drops nothing more). 2000 worn pickaxes all fall in that band and none is broken.
	 */
	@GameTest(structure = CAVERN, maxTicks = 100)
	public void dropsEmeraldsAndOneWornPickaxeOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		int x0 = origin.getX();
		int Y = origin.getY();
		int z0 = origin.getZ();
		rock(level, new BlockPos(x0, Y, z0), new BlockPos(x0 + 10, Y, z0 + 10), Blocks.STONE);
		RandomSource random = RandomSource.create(5L);
		for (int i = 0; i < 2000; i++) {
			ItemStack pick = OldCoot.wornPickaxe(random);
			float left = (pick.getMaxDamage() - pick.getDamageValue()) / (float) pick.getMaxDamage();
			helper.assertTrue(left >= 0.05F - 1e-4 && left <= 0.5F + 1e-4 && pick.getDamageValue() < pick.getMaxDamage(), "5-50% left: " + left);
		}
		helper.startSequence().thenIdle(1).thenExecute(() -> {
			OldCoot coot = egg(level, new BlockPos(x0 + 5, Y + 1, z0 + 5));
			ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, BlockPos.ZERO);
			player.teleportTo(x0 + 7.5, Y + 1, z0 + 5.5);
			coot.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
			coot.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
			AABB around = new AABB(new BlockPos(x0 + 5, Y + 1, z0 + 5)).inflate(4);
			int emeralds = 0;
			int picks = 0;
			float left = 0;
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around)) {
				ItemStack stack = item.getItem();
				if (stack.is(Items.EMERALD)) {
					emeralds += stack.getCount();
				} else if (stack.is(Items.IRON_PICKAXE)) {
					picks += stack.getCount();
					left = (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage();
				}
				item.discard();
			}
			SimpleBlockTestLog.info("M14 DROPS: {} emeralds, {} iron pickaxes ({} left)", emeralds, picks, left);
			helper.assertTrue(emeralds >= 1 && emeralds <= 3, "1-3 emeralds: " + emeralds);
			helper.assertTrue(picks == 1 && left >= 0.05F - 1e-4 && left <= 0.5F + 1e-4, "one worn pickaxe: " + picks + ", " + left);
			TestPlayers.remove(player);
			helper.succeed();
		});
	}
}
