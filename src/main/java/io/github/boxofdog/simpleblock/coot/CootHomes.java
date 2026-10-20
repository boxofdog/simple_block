package io.github.boxofdog.simpleblock.coot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * M14 the natural Old Coot ledger of a level: one record per generated abandoned mineshaft, keyed by its
 * structure start's chunk, kept in the level's saved data. A record says once and for all whether the
 * mineshaft is a home (about two in three are, decided the first time anyone comes near it), which
 * resident is living there (living means not yet seen to die: being unloaded is not death), and when the
 * last one died (the start of the seven-day wait). Egg and command spawned Coots never touch it.
 */
public final class CootHomes extends SavedData {
	/** About two mineshafts in three are a home. */
	public static final float HOME_CHANCE = 2.0F / 3.0F;
	/** No new resident until seven Minecraft days of world time after the last one died. */
	public static final long COOLDOWN = 7L * 24000L;
	public static final long NEVER = Long.MIN_VALUE;

	/** One mineshaft: a home or not (fixed), its living resident if any, when its last resident died. */
	public static final class Home {
		static final Codec<Home> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("start").forGetter(h -> h.start),
			Codec.BOOL.fieldOf("home").forGetter(h -> h.home),
			UUIDUtil.CODEC.optionalFieldOf("resident").forGetter(h -> Optional.ofNullable(h.resident)),
			Codec.LONG.optionalFieldOf("died", NEVER).forGetter(h -> h.died),
			Codec.INT.optionalFieldOf("torches", 0).forGetter(h -> h.torches)
		).apply(i, (start, home, resident, died, torches) -> new Home(start, home, resident.orElse(null), died, torches)));

		public final long start;
		public final boolean home;
		public UUID resident;
		public long died;
		/** Torches his residents have put up here, for all time (bounded: picking them up yields no more). */
		public int torches;

		Home(long start, boolean home, UUID resident, long died, int torches) {
			this.start = start;
			this.home = home;
			this.resident = resident;
			this.died = died;
			this.torches = torches;
		}

		/** Whether this home may have a resident now: a home, nobody living there, and seven days since a death. */
		public boolean vacant(long gameTime) {
			return this.home && this.resident == null && (this.died == NEVER || gameTime - this.died >= COOLDOWN);
		}
	}

	private static final Codec<CootHomes> CODEC = Home.CODEC.listOf().xmap(CootHomes::new, homes -> List.copyOf(homes.homes.values()));
	public static final SavedDataType<CootHomes> TYPE = new SavedDataType<>(OldCootContent.id("old_coot_homes"), CootHomes::new, CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Long2ObjectOpenHashMap<Home> homes = new Long2ObjectOpenHashMap<>();

	CootHomes() {
	}

	private CootHomes(List<Home> homes) {
		homes.forEach(home -> this.homes.put(home.start, home));
	}

	public static CootHomes get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The record of the mineshaft started in this chunk, if anyone has been near it. */
	public Home find(long start) {
		return this.homes.get(start);
	}

	/** The mineshaft's record, made (and whether it is a home decided, from the world seed) the first time. */
	public Home get(ServerLevel level, long start) {
		Home home = this.homes.get(start);
		if (home == null) {
			home = new Home(start, isHome(level.getSeed(), start), null, NEVER, 0);
			this.homes.put(start, home);
			this.setDirty();
		}
		return home;
	}

	/** Whether the mineshaft started in that chunk is a home: fixed by the seed, about two in three. */
	public static boolean isHome(long seed, long start) {
		return RandomSource.create(seed ^ start * 0x9E3779B97F4A7C15L ^ 0x6F6C645F636F6F74L).nextFloat() < HOME_CHANCE;
	}

	void settle(Home home, UUID resident) {
		home.resident = resident;
		this.setDirty();
	}

	/** The resident died (or was removed for good): the home is empty from now, and the wait begins. */
	void vacate(long start, UUID resident, long gameTime) {
		Home home = this.homes.get(start);
		if (home != null && resident.equals(home.resident)) {
			home.resident = null;
			home.died = gameTime;
			this.setDirty();
		}
	}

	void torchPlaced(Home home) {
		home.torches++;
		this.setDirty();
	}

	/** For tests: forget a record (as if nobody had come near the mineshaft). */
	public void forget(long start) {
		if (this.homes.remove(start) != null) {
			this.setDirty();
		}
	}
}
