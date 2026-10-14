package io.github.boxofdog.simpleblock.client.workwear;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import io.github.boxofdog.simpleblock.network.TrailMarkersPayload;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.workwear.TrailMarkers;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Trail Blazer, client: torches, ladders and Spelunker's Ropes within {@code workwear.trail_blazer_range}
 * blocks show as faint outlines through walls, yellow if a player put them there (the server's list,
 * {@link TrailMarkersPayload}; ropes always), blue if the world did, fading with distance. Drawn as
 * always-on-top gizmos added each client tick; the blocks are looked for twice a second, only in chunk
 * sections that can hold one. Only these blocks: nothing else is ever revealed.
 */
public final class TrailBlazerGhosts {
	private static final int YELLOW = 0xFFE0B020;
	private static final int BLUE = 0xFF4A8CFF;

	/** A ghost: where, its outline (the block's shape), and whether a player placed it. */
	public record Ghost(BlockPos pos, AABB box, boolean placed) {
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
			int colour = ghost.placed() ? YELLOW : BLUE;
			int stroke = (Math.round(150 * fade) << 24) | (colour & 0xFFFFFF);
			int fill = (Math.round(45 * fade) << 24) | (colour & 0xFFFFFF);
			Gizmos.cuboid(ghost.box(), GizmoStyle.strokeAndFill(stroke, 1.5F, fill)).setAlwaysOnTop();
		}
	}

	private static List<Ghost> scan(ClientLevel level, BlockPos centre, int range) {
		List<Ghost> found = new ArrayList<>();
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
								VoxelShape shape = state.getShape(level, pos);
								AABB box = (shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos)).inflate(0.02);
								found.add(new Ghost(pos.immutable(), box, TrailMarkers.alwaysPlaced(state) || placed.contains(pos.asLong())));
							}
						}
					}
				}
			}
		}
		return List.copyOf(found);
	}
}
