package io.github.boxofdog.simpleblock.workwear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M10: the Headlamp's light. While a player wears it (alive, not a spectator), an invisible vanilla
 * light block of level {@code workwear.headlamp_light} stands in the open air (or still water) where
 * their head is, or at their feet if the head is in something else; it follows them block by block.
 * No fuel, no beam: ordinary light, the same for every player and for mobs.
 *
 * <p>Nothing is left behind: the light goes as soon as its player moves on, takes the Headlamp off,
 * dies, becomes a spectator or leaves, and when the server stops. Only air and still water are ever
 * replaced, and only a light block is ever removed. Each light placed is also recorded in the level's
 * saved data until it is removed, so a light stranded by a crash is cleared when its chunk next loads.
 * Players sharing a block share its light.
 */
public final class Headlamps {
	/** Each player's current light. */
	private static final Map<UUID, GlobalPos> LIGHTS = new HashMap<>();
	/** Recorded lights found in a loaded chunk with nobody wearing them: cleared next tick. */
	private static final Set<GlobalPos> STALE = new HashSet<>();

	private Headlamps() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Headlamps::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> release(server, handler.player.getUUID())));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (UUID player : List.copyOf(LIGHTS.keySet())) {
				release(server, player);
			}
		});
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			Recorded recorded = Recorded.get(level);
			ChunkPos chunkPos = chunk.getPos();
			for (BlockPos pos : recorded.positions) {
				GlobalPos at = GlobalPos.of(level.dimension(), pos);
				if (ChunkPos.containing(pos).equals(chunkPos) && !LIGHTS.containsValue(at)) {
					STALE.add(at);
				}
			}
		});
	}

	/** The light of the player {@code player} is wearing, or null. For tests. */
	public static @Nullable GlobalPos lightOf(UUID player) {
		return LIGHTS.get(player);
	}

	private static void tick(MinecraftServer server) {
		for (GlobalPos at : List.copyOf(STALE)) {
			ServerLevel level = server.getLevel(at.dimension());
			if (level != null && level.isLoaded(at.pos()) && !LIGHTS.containsValue(at)) {
				remove(level, at.pos());
			}
		}
		STALE.clear();
		Set<UUID> online = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			online.add(player.getUUID());
			update(player);
		}
		for (UUID gone : List.copyOf(LIGHTS.keySet())) {
			if (!online.contains(gone)) {
				release(server, gone);
			}
		}
	}

	private static boolean lit(ServerPlayer player) {
		return Workwear.headlamp(player) && player.isAlive() && !player.isSpectator() && !player.isRemoved();
	}

	/** Where the light should be now: the head's block, else the feet's, if it is open air or still water (or already a light). */
	private static @Nullable BlockPos target(ServerPlayer player) {
		ServerLevel level = player.level();
		for (BlockPos pos : new BlockPos[] {BlockPos.containing(player.getEyePosition()), player.blockPosition()}) {
			if (level.isLoaded(pos) && placeable(level.getBlockState(pos))) {
				return pos.immutable();
			}
		}
		return null;
	}

	private static boolean placeable(BlockState state) {
		return state.isAir() || state.is(Blocks.LIGHT) || state.is(Blocks.WATER) && state.getFluidState().isSource();
	}

	private static void update(ServerPlayer player) {
		UUID id = player.getUUID();
		GlobalPos current = LIGHTS.get(id);
		BlockPos want = lit(player) ? target(player) : null;
		GlobalPos wanted = want == null ? null : GlobalPos.of(player.level().dimension(), want);
		if (wanted != null && wanted.equals(current)) {
			ServerLevel level = player.level();
			if (!level.getBlockState(want).is(Blocks.LIGHT)) {
				// Something replaced it (a block placed, water flowed): put it back if it can be.
				place(level, want);
			}
			return;
		}
		if (current != null) {
			LIGHTS.remove(id);
			ServerLevel level = player.level().getServer().getLevel(current.dimension());
			if (level != null && !LIGHTS.containsValue(current)) {
				remove(level, current.pos());
			}
		}
		if (wanted != null && place(player.level(), want)) {
			LIGHTS.put(id, wanted);
		}
	}

	private static boolean place(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!placeable(state)) {
			return false;
		}
		int light = Math.clamp(Tuning.getInt("workwear.headlamp_light"), 1, 15);
		boolean water = state.is(Blocks.WATER) || state.is(Blocks.LIGHT) && state.getValue(LightBlock.WATERLOGGED);
		BlockState lamp = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light).setValue(LightBlock.WATERLOGGED, water);
		if (!state.equals(lamp)) {
			level.setBlock(pos, lamp, Block.UPDATE_ALL);
		}
		Recorded.get(level).add(pos);
		return true;
	}

	/** Takes the light at {@code pos} away (only a light block; water stays water). */
	private static void remove(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(Blocks.LIGHT)) {
			level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Fluids.WATER.defaultFluidState().createLegacyBlock() : Blocks.AIR.defaultBlockState(),
				Block.UPDATE_ALL);
		}
		Recorded.get(level).remove(pos);
	}

	private static void release(MinecraftServer server, UUID player) {
		GlobalPos current = LIGHTS.remove(player);
		if (current != null && !LIGHTS.containsValue(current)) {
			ServerLevel level = server.getLevel(current.dimension());
			if (level != null && level.isLoaded(current.pos())) {
				remove(level, current.pos());
			}
		}
	}

	/** The lights placed in a level and not yet removed, kept with the level so a crash cannot strand them. */
	static final class Recorded extends SavedData {
		private static final Codec<Recorded> CODEC = BlockPos.CODEC.listOf().xmap(Recorded::new, recorded -> List.copyOf(recorded.positions));
		static final SavedDataType<Recorded> TYPE = new SavedDataType<>(WorkwearContent.id("headlamp_lights"), Recorded::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

		final Set<BlockPos> positions = new HashSet<>();

		Recorded() {
		}

		Recorded(List<BlockPos> positions) {
			this.positions.addAll(positions);
		}

		static Recorded get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(TYPE);
		}

		void add(BlockPos pos) {
			if (this.positions.add(pos.immutable())) {
				this.setDirty();
			}
		}

		void remove(BlockPos pos) {
			if (this.positions.remove(pos)) {
				this.setDirty();
			}
		}

		/** Copy, for tests. */
		List<BlockPos> list() {
			return new ArrayList<>(this.positions);
		}
	}
}
