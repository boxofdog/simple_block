package io.github.boxofdog.simpleblock.client.workwear;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
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
 * blocks show as outlines through walls, yellow if a player put them there (the server's list,
 * {@link TrailMarkersPayload}; ropes always), blue if the world did, dimming with distance. The blocks are
 * looked for twice a second, only in chunk sections that can hold one. Only these blocks: nothing else is
 * ever revealed.
 *
 * <p>M13 BUG-016: each outline follows the block's own model, as the glowing effect outlines a mob: the
 * block's model is submitted to the game's entity-outline pass only (never drawn itself), which traces the
 * silhouette of what it draws (the torch's stick and flame, the ladder's rails and rungs, the rope) and shows
 * it over everything in front.
 */
public final class TrailBlazerGhosts {
	private static final int YELLOW = 0xFFE0B020;
	private static final int BLUE = 0xFF4A8CFF;

	/** A ghost: where, the block, the parts of its model (outlined; found with the ghost), and whether a player placed it. */
	public record Ghost(BlockPos pos, BlockState state, List<BlockStateModelPart> parts, boolean placed) {
	}

	/** Draws only into the outline pass: the model's silhouette, with the blocks' textures' cut-outs. */
	private static final RenderType OUTLINE = RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS);

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
		LevelRenderEvents.COLLECT_SUBMITS.register(TrailBlazerGhosts::submit);
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
	}

	private static void submit(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (ghosts.isEmpty() || player == null) {
			return;
		}
		int range = Tuning.getInt("workwear.trail_blazer_range");
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		for (Ghost ghost : ghosts) {
			if (ghost.parts().isEmpty()) {
				continue;
			}
			double distance = Math.sqrt(ghost.pos().distToCenterSqr(player.getEyePosition()));
			float fade = (float) Math.clamp(1.0 - distance / (range + 1.0), 0.3, 1.0);
			int colour = ARGB.scaleRGB(ghost.placed() ? YELLOW : BLUE, fade) | 0xFF000000;
			poseStack.pushPose();
			Vec3 offset = ghost.state().getOffset(ghost.pos());
			poseStack.translate(ghost.pos().getX() - camera.x + offset.x, ghost.pos().getY() - camera.y + offset.y, ghost.pos().getZ() - camera.z + offset.z);
			context.submitNodeCollector().submitBlockModel(poseStack, OUTLINE, ghost.parts(), BlockModelRenderState.EMPTY_TINTS,
				LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
			poseStack.popPose();
		}
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
								List<BlockStateModelPart> parts = new ArrayList<>();
								models.get(state).collectParts(RandomSource.create(state.getSeed(pos)), parts);
								found.add(new Ghost(pos.immutable(), state, List.copyOf(parts), TrailMarkers.alwaysPlaced(state) || placed.contains(pos.asLong())));
							}
						}
					}
				}
			}
		}
		return List.copyOf(found);
	}
}
