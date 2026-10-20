package io.github.boxofdog.simpleblock.gametest.client;

import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.assertTrue;
import static io.github.boxofdog.simpleblock.gametest.client.CableClientTest.lookAtPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import io.github.boxofdog.simpleblock.coot.CootHomes;
import io.github.boxofdog.simpleblock.coot.CootResidents;
import io.github.boxofdog.simpleblock.coot.OldCoot;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;

/**
 * M14 the Old Coot in a real generated world and over a real network ({@code m14-world-*} screenshots).
 * A normally generated world (fixed seed): the nearest abandoned mineshaft that is a home gets its natural
 * resident once a player stands in it, in one of its own pieces; after save, quit and reopen the same
 * resident is there (same entity, same home), still the only one, and more checks bring no other. Then
 * a dedicated server with the client connected as a remote player: a command-spawned Coot is seen with his
 * pickaxe; the player's blow angers him, he swings and hurts the player; his death drops are seen.
 */
public class M14WorldClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> log = new ArrayList<>();
		TestWorldSave save;
		long[] home = new long[1];
		UUID[] resident = new UUID[1];
		int[] room = new int[3];
		List<Structure> kind = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false)
			.adjustSettings(settings -> settings.setSeed("simple-block-m14")).create()) {
			save = world.getWorldSave();
			TestServerContext server = world.getServer();
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:spawn_monsters false");
			server.runCommand("time set noon");
			server.runCommand("gamemode creative @a");
			String found = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				var mineshafts = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(StructureTags.MINESHAFT);
				for (int i = 0; i < 16; i++) {
					Pair<BlockPos, Holder<Structure>> near = level.getChunkSource().getGenerator()
						.findNearestMapStructure(level, mineshafts, new BlockPos(i * 320, 0, -i * 160), 20, false);
					if (near == null) {
						continue;
					}
					ChunkPos chunk = ChunkPos.containing(near.getFirst());
					StructureStart start = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(near.getSecond().value());
					if (start == null || !start.isValid() || !CootHomes.isHome(level.getSeed(), chunk.pack())) {
						continue;
					}
					BoundingBox first = start.getPieces().getFirst().getBoundingBox();
					home[0] = chunk.pack();
					kind.add(near.getSecond().value());
					room[0] = first.getCenter().getX();
					room[1] = first.minY() + 1;
					room[2] = first.getCenter().getZ();
					return near.getSecond().unwrapKey().orElseThrow().identifier() + " started in chunk " + chunk + ", " + start.getPieces().size()
						+ " pieces, first " + first;
				}
				return null;
			});
			assertTrue(found != null, "a mineshaft that is a home near the seed's origin");
			log.add("home: " + found);
			server.runCommand("tp @a " + (room[0] + 0.5) + " " + room[1] + " " + (room[2] + 0.5));
			world.getConnection().waitForChunksRender();
			int waited = 0;
			while (waited < 6000 && server.computeOnServer(s -> CootHomes.get(s.overworld()).find(home[0]) == null
				|| CootHomes.get(s.overworld()).find(home[0]).resident == null)) {
				context.waitTicks(100);
				waited += 100;
			}
			String placed = server.computeOnServer(s -> {
				CootHomes.Home record = CootHomes.get(s.overworld()).find(home[0]);
				if (record == null || record.resident == null) {
					return null;
				}
				resident[0] = record.resident;
				OldCoot coot = (OldCoot) s.overworld().getEntity(record.resident);
				// His own mineshaft's start (others may overlap it).
				ChunkPos chunk = ChunkPos.unpack(home[0]);
				StructureStart start = s.overworld().getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(kind.getFirst());
				return "at " + coot.blockPosition() + " (block light " + s.overworld().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, coot.blockPosition())
					+ ", sky light " + s.overworld().getBrightness(net.minecraft.world.level.LightLayer.SKY, coot.blockPosition()) + "), in a piece of his mineshaft "
					+ (start != null && start.isValid() && s.overworld().structureManager().structureHasPieceAt(coot.blockPosition(), start));
			});
			log.add("natural resident after " + waited + " ticks: " + placed);
			if (placed == null) {
				log.add(server.computeOnServer(s -> {
					ServerLevel level = s.overworld();
					var player = s.getPlayerList().getPlayers().getFirst();
					StructureStart start = level.structureManager().getStructureAt(player.blockPosition(),
						level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(StructureTags.MINESHAFT));
					int spots = 0;
					int ticking = 0;
					for (var piece : start.getPieces()) {
						ticking += level.isPositionEntityTicking(piece.getBoundingBox().getCenter()) ? 1 : 0;
					}
					for (int i = 0; i < 100; i++) {
						spots += start.isValid() && CootResidents.findSpot(level, start, level.getRandom()) != null ? 1 : 0;
					}
					return "debug: player at " + player.blockPosition() + " " + player.gameMode() + ", start found " + start.isValid() + " "
						+ start.getChunkPos() + ", record " + CootHomes.get(level).find(home[0]) + ", pieces ticking " + ticking + " of "
						+ start.getPieces().size() + ", findSpot " + spots + "/100, game time " + level.getGameTime();
				}));
			}
			assertTrue(placed != null && placed.endsWith("true"), "the home's resident appears in one of its pieces: " + log);
			// A look at him in his mineshaft (as a spectator, down a clear stretch of passage beside him).
			server.runCommand("gamemode spectator @a");
			Vec3 eye = server.computeOnServer(s -> {
				OldCoot coot = (OldCoot) s.overworld().getEntity(resident[0]);
				for (int reach = 4; reach >= 2; reach--) {
					for (Direction side : Direction.Plane.HORIZONTAL) {
						boolean clear = true;
						for (int step = 1; step <= reach; step++) {
							BlockPos at = coot.blockPosition().relative(side, step);
							clear &= s.overworld().getBlockState(at).isAir() && s.overworld().getBlockState(at.above()).isAir();
						}
						if (clear) {
							return Vec3.atBottomCenterOf(coot.blockPosition().relative(side, reach));
						}
					}
				}
				return Vec3.atBottomCenterOf(coot.blockPosition().above(2));
			});
			server.runCommand("tp @a " + eye.x + " " + eye.y + " " + eye.z);
			context.waitTicks(20);
			Vec3 at = server.computeOnServer(s -> s.overworld().getEntity(resident[0]).position());
			lookAtPoint(context, at.add(0.0, 0.8, 0.0));
			server.runCommand("effect give @a minecraft:night_vision 60 0 true");
			context.waitTicks(10);
			context.takeScreenshot("m14-world-natural-resident");
			server.runCommand("gamemode creative @a");
		}

		// Save, quit and open again.
		try (TestSingleplayerContext world = save.open()) {
			TestServerContext server = world.getServer();
			server.runCommand("tp @a " + (room[0] + 0.5) + " " + room[1] + " " + (room[2] + 0.5));
			world.getConnection().waitForChunksRender();
			context.waitTicks(100);
			String after = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int i = 0; i < 50; i++) {
					CootResidents.check(level);
				}
				OldCoot coot = (OldCoot) level.getEntity(resident[0]);
				long residents = 0;
				for (var entity : level.getAllEntities()) {
					if (entity instanceof OldCoot c && c.home() == home[0]) {
						residents++;
					}
				}
				CootHomes.Home record = CootHomes.get(level).find(home[0]);
				return "same resident " + (coot != null && coot.home() == home[0] && coot.anchor() != null) + ", residents of the home " + residents
					+ ", the ledger names him " + resident[0].equals(record.resident);
			});
			log.add("after save and reload and 50 more checks: " + after);
			assertTrue(after.equals("same resident true, residents of the home 1, the ledger names him true"), "he outlives a reload, alone: " + log);
		}

		// A dedicated server, the client a remote player.
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
			TestDedicatedServerConnection connection = server.connect()) {
			int g = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("time set noon");
			server.runCommand("gamemode survival @a");
			server.runCommand("effect give @a minecraft:resistance 60 2 true");
			server.runCommand("tp @a 0.5 " + g + " 3.5 180 10");
			connection.waitForChunksRender();
			server.runCommand("summon simple_block:old_coot 0.5 " + g + " 0.5");
			context.waitTicks(5);
			boolean seen = context.computeOnClient(client -> client.level.getEntitiesOfClass(OldCoot.class, new AABB(-4, g - 2, -4, 4, g + 4, 4)).stream()
				.anyMatch(c -> c.getMainHandItem().is(Items.IRON_PICKAXE)));
			float health = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getHealth());
			Vec3 coot = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(OldCoot.class, new AABB(-8, g - 2, -8, 8, g + 4, 8)).getFirst().position());
			lookAtPoint(context, coot.add(0.0, 0.7, 0.0));
			context.getInput().pressKey(options -> options.keyAttack);
			boolean swung = false;
			for (int i = 0; i < 200 && !(swung && server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getHealth()) < health); i++) {
				context.waitTicks(1);
				swung |= context.computeOnClient(client -> client.level.getEntitiesOfClass(OldCoot.class, new AABB(-8, g - 2, -8, 8, g + 4, 8)).stream()
					.anyMatch(c -> c.getSwingAnimation(1.0F) > 0.0F));
			}
			boolean angry = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(OldCoot.class, new AABB(-8, g - 2, -8, 8, g + 4, 8)).stream()
				.anyMatch(OldCoot::isAngry));
			float hurt = health - server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getHealth());
			context.takeScreenshot("m14-world-server-fight");
			// Well back from him (not to pick up his drops), then his death.
			server.runCommand("execute as @e[type=simple_block:old_coot] at @s run tp @a ~ ~ ~12");
			context.waitTicks(5);
			server.runCommand("kill @e[type=simple_block:old_coot]");
			context.waitTicks(20);
			String drops = context.computeOnClient(client -> client.level.getEntitiesOfClass(ItemEntity.class, new AABB(-32, g - 4, -32, 32, g + 8, 32)).stream()
				.map(item -> item.getItem().getCount() + " " + item.getItem().getItem()).toList().toString());
			log.add("dedicated server: client sees him with his pickaxe " + seen + "; after the player's blow he is angry " + angry + ", swung " + swung
				+ ", hurt the player " + hurt + "; drops seen by the client " + drops);
			SimpleBlockTestLog.info("CLIENT M14 WORLD: {}", log);
			assertTrue(seen && angry && swung && hurt > 0.0F && drops.contains("emerald") && drops.contains("iron_pickaxe"), "over the network: " + log);
		}
	}
}
