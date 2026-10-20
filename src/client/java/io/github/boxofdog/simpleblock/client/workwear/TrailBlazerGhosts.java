package io.github.boxofdog.simpleblock.client.workwear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.network.TrailMarkersPayload;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.workwear.TrailMarkers;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Trail Blazer, client: torches, ladders and Spelunker's Ropes within {@code workwear.trail_blazer_range}
 * blocks show as faint outlines through walls, yellow if a player put them there (the server's list,
 * {@link TrailMarkersPayload}; ropes always), blue if the world did, fading with distance. Drawn as
 * always-on-top gizmo lines added each client tick; the blocks are looked for twice a second, only in chunk
 * sections that can hold one. Only these blocks: nothing else is ever revealed.
 *
 * <p>M13 BUG-016 (owner direction): the lines follow the block's own model, not its hit box: every edge of the
 * model's boxes (the torch's stick, upright or leaning from a wall; the rope, its anchor and the turns of its
 * coil), found with the block. The ladder's 3D model (stiles and rungs, tools/models/generate_ladder_models.py)
 * is traced the same way.
 */
public final class TrailBlazerGhosts {
	private static final int YELLOW = 0xFFE0B020;
	private static final int BLUE = 0xFF4A8CFF;

	/** A ghost: where, its outline (line segments from-to, in the block's own 0-1 space), and whether a player placed it. */
	public record Ghost(BlockPos pos, List<Vec3[]> lines, boolean placed) {
	}

	private static LongSet placed = new LongOpenHashSet();
	private static List<Ghost> ghosts = List.of();
	private static int scanIn;

	private TrailBlazerGhosts() {
	}

	/** The ghosts shown now (for the client test). */
	public static List<Ghost> ghosts() {
		return ghosts;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(TrailMarkersPayload.TYPE, (payload, context) -> {
			LongSet set = new LongOpenHashSet();
			payload.placed().forEach(pos -> set.add(pos.asLong()));
			placed = set;
			scanIn = 0;
		});
		ClientTickEvents.END_CLIENT_TICK.register(TrailBlazerGhosts::tick);
	}

	private static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || Workwear.bonus(player) != Workwear.Bonus.TRAIL_BLAZER) {
			ghosts = List.of();
			return;
		}
		int range = Tuning.getInt("workwear.trail_blazer_range");
		if (--scanIn <= 0) {
			scanIn = 10;
			ghosts = scan(level, player.blockPosition(), range);
		}
		for (Ghost ghost : ghosts) {
			double distance = Math.sqrt(ghost.pos().distToCenterSqr(player.getEyePosition()));
			float fade = (float) Math.clamp(1.0 - distance / (range + 1.0), 0.15, 1.0);
			int colour = (Math.round(170 * fade) << 24) | ((ghost.placed() ? YELLOW : BLUE) & 0xFFFFFF);
			Vec3 at = Vec3.atLowerCornerOf(ghost.pos());
			for (Vec3[] line : ghost.lines()) {
				Gizmos.line(at.add(line[0]), at.add(line[1]), colour, 1.5F).setAlwaysOnTop();
			}
		}
	}

	/** The outline of a marker: its model's box edges. */
	static List<Vec3[]> outline(BlockState state, BlockPos pos, BlockStateModelSet models) {
		Map<String, Vec3[]> edges = new LinkedHashMap<>();
		List<BlockStateModelPart> parts = new ArrayList<>();
		models.get(state).collectParts(RandomSource.create(state.getSeed(pos)), parts);
		Vec3 offset = state.getOffset(pos);
		for (BlockStateModelPart part : parts) {
			for (Direction direction : FACES) {
				for (BakedQuad quad : part.getQuads(direction)) {
					for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
						Vector3fc a = quad.position(i);
						Vector3fc b = quad.position((i + 1) % BakedQuad.VERTEX_COUNT);
						edge(edges, new Vec3(a.x(), a.y(), a.z()).add(offset), new Vec3(b.x(), b.y(), b.z()).add(offset));
					}
				}
			}
		}
		return List.copyOf(edges.values());
	}

	/** Every direction and none (the faces a model part may give). */
	private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

	/** One edge, once: the same edge of two touching faces is drawn once. */
	private static void edge(Map<String, Vec3[]> edges, Vec3 a, Vec3 b) {
		String ka = key(a);
		String kb = key(b);
		if (ka.equals(kb)) {
			return;
		}
		edges.putIfAbsent(ka.compareTo(kb) < 0 ? ka + "|" + kb : kb + "|" + ka, new Vec3[] {a, b});
	}

	private static String key(Vec3 v) {
		return Math.round(v.x * 1024) + "," + Math.round(v.y * 1024) + "," + Math.round(v.z * 1024);
	}

	private static List<Ghost> scan(ClientLevel level, BlockPos centre, int range) {
		List<Ghost> found = new ArrayList<>();
		BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		long rangeSqr = (long) range * range;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int minSection = SectionPos.blockToSectionCoord(centre.getY() - range);
		int maxSection = SectionPos.blockToSectionCoord(centre.getY() + range);
		for (int cx = SectionPos.blockToSectionCoord(centre.getX() - range); cx <= SectionPos.blockToSectionCoord(centre.getX() + range); cx++) {
			for (int cz = SectionPos.blockToSectionCoord(centre.getZ() - range); cz <= SectionPos.blockToSectionCoord(centre.getZ() + range); cz++) {
				if (!(level.getChunkSource().getChunk(cx, cz, false) instanceof LevelChunk chunk)) {
					continue;
				}
				for (int sy = Math.max(minSection, level.getMinSectionY()); sy <= Math.min(maxSection, level.getMaxSectionY()); sy++) {
					LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
					if (section.hasOnlyAir() || !section.getStates().maybeHas(TrailMarkers::marker)) {
						continue;
					}
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								BlockState state = section.getBlockState(x, y, z);
								if (!TrailMarkers.marker(state)) {
									continue;
								}
								pos.set(SectionPos.sectionToBlockCoord(cx) + x, SectionPos.sectionToBlockCoord(sy) + y, SectionPos.sectionToBlockCoord(cz) + z);
								if (pos.distSqr(centre) > rangeSqr) {
									continue;
								}
								found.add(new Ghost(pos.immutable(), outline(state, pos, models),
									TrailMarkers.alwaysPlaced(state) || placed.contains(pos.asLong())));
							}
						}
					}
				}
			}
		}
		return List.copyOf(found);
	}
}
