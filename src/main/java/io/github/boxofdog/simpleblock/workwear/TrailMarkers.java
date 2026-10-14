package io.github.boxofdog.simpleblock.workwear;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.boxofdog.simpleblock.network.TrailMarkersPayload;
import io.github.boxofdog.simpleblock.rope.RopeContent;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M10 Trail Blazer's infrastructure: torches (any kind), ladders and Spelunker's Ropes near a player with
 * the full spelunking outfit show faintly through walls (client {@code workwear/TrailBlazerGhosts}),
 * yellow where a player put them, blue where the world did.
 *
 * <p><b>Provenance.</b> Every torch or ladder a player places is recorded in its level's saved data
 * ({@code mixin/workwear/BlockItemMixin}); a record is dropped once the block there is no longer a torch
 * or ladder. Spelunker's Ropes are always player-made. <b>Unknown origin:</b> anything without a record is
 * shown as the world's (blue): in a world made with M10 that is what structures placed, plus anything
 * moved in by commands or structure blocks.
 *
 * <p>Every second the server sends each Trail Blazer the recorded positions within
 * {@code workwear.trail_blazer_range} blocks ({@link TrailMarkersPayload}). It is not an ore scan: only
 * these three kinds of block are ever shown.
 */
public final class TrailMarkers {
	private TrailMarkers() {
	}

	/** Whether a block is one Trail Blazer shows. */
	public static boolean marker(BlockState state) {
		return state.getBlock() instanceof BaseTorchBlock || state.getBlock() instanceof LadderBlock || state.is(RopeContent.ROPE_ANCHOR)
			|| state.is(RopeContent.ROPE_BLOCK) || state.is(RopeContent.ROPE_COIL);
	}

	/** Whether a marker is always player-made (the Spelunker's Rope). */
	public static boolean alwaysPlaced(BlockState state) {
		return state.is(RopeContent.ROPE_ANCHOR) || state.is(RopeContent.ROPE_BLOCK) || state.is(RopeContent.ROPE_COIL);
	}

	/** A player placed {@code state} at {@code pos}: recorded if it is a torch or ladder. */
	public static void placed(ServerLevel level, BlockPos pos, BlockState state) {
		if (marker(state) && !alwaysPlaced(state)) {
			Recorded.get(level).add(pos);
		}
	}

	public static boolean wasPlaced(ServerLevel level, BlockPos pos) {
		return Recorded.get(level).contains(pos);
	}

	/** The recorded torches and ladders within {@code range} blocks of {@code centre} that are still there. */
	public static List<BlockPos> near(ServerLevel level, BlockPos centre, int range) {
		Recorded recorded = Recorded.get(level);
		List<BlockPos> found = new ArrayList<>();
		int chunks = (range >> 4) + 1;
		ChunkPos middle = ChunkPos.containing(centre);
		for (int cx = middle.x() - chunks; cx <= middle.x() + chunks; cx++) {
			for (int cz = middle.z() - chunks; cz <= middle.z() + chunks; cz++) {
				LongSet set = recorded.byChunk.get(ChunkPos.pack(cx, cz));
				if (set == null) {
					continue;
				}
				for (long packed : set.toLongArray()) {
					BlockPos pos = BlockPos.of(packed);
					if (pos.distManhattan(centre) > 3 * range || pos.distSqr(centre) > (double) range * range || !level.isLoaded(pos)) {
						continue;
					}
					if (marker(level.getBlockState(pos))) {
						found.add(pos);
					} else {
						recorded.remove(pos);
					}
				}
			}
		}
		return found;
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(TrailMarkersPayload.TYPE, TrailMarkersPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			int range = Tuning.getInt("workwear.trail_blazer_range");
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (Workwear.bonus(player) == Workwear.Bonus.TRAIL_BLAZER && ServerPlayNetworking.canSend(player, TrailMarkersPayload.TYPE)) {
					ServerPlayNetworking.send(player, new TrailMarkersPayload(near(player.level(), player.blockPosition(), range)));
				}
			}
		});
	}

	/** The player-placed torches and ladders of a level, by chunk. */
	static final class Recorded extends SavedData {
		private static final Codec<Recorded> CODEC = Codec.LONG.listOf().xmap(Recorded::new, recorded -> {
			List<Long> all = new ArrayList<>();
			recorded.byChunk.values().forEach(set -> set.forEach(all::add));
			return all;
		});
		static final SavedDataType<Recorded> TYPE = new SavedDataType<>(WorkwearContent.id("trail_markers"), Recorded::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

		final Long2ObjectOpenHashMap<LongSet> byChunk = new Long2ObjectOpenHashMap<>();

		Recorded() {
		}

		Recorded(List<Long> positions) {
			positions.forEach(packed -> this.put(BlockPos.of(packed)));
		}

		static Recorded get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(TYPE);
		}

		private boolean put(BlockPos pos) {
			return this.byChunk.computeIfAbsent(ChunkPos.containing(pos).pack(), key -> new LongOpenHashSet()).add(pos.asLong());
		}

		void add(BlockPos pos) {
			if (this.put(pos)) {
				this.setDirty();
			}
		}

		boolean contains(BlockPos pos) {
			LongSet set = this.byChunk.get(ChunkPos.containing(pos).pack());
			return set != null && set.contains(pos.asLong());
		}

		void remove(BlockPos pos) {
			long key = ChunkPos.containing(pos).pack();
			LongSet set = this.byChunk.get(key);
			if (set != null && set.remove(pos.asLong())) {
				if (set.isEmpty()) {
					this.byChunk.remove(key);
				}
				this.setDirty();
			}
		}
	}
}
