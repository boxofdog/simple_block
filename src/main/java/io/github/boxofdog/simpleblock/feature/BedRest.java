package io.github.boxofdog.simpleblock.feature;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M13: lying in a bed at night heals, paid for with food. The night is never skipped (SF-01).
 *
 * <p>Vanilla decides whether the player may lie down at all: too far, obstructed, monsters nearby
 * ("You may not rest now"), and the Nether and End, where beds explode. This only acts on a player who
 * is lying in a bed while the Overworld clock says night (vanilla also lets players lie down in a
 * daytime thunderstorm; that rests without healing). Every {@link #TICKS_PER_HEALTH} ticks in bed
 * restores one health point (half a heart: a heart every two seconds), until full health.
 *
 * <p>The price is a quarter food point per health point (a full bar of 20 for 5 food points). Food is
 * paid a whole point at a time, before the healing it buys, and the unspent quarters are kept with the
 * player ({@link #CREDIT}, saved with the player and kept through death), so getting up and lying down
 * again, or healing a little at a time, never heals for free. No food and no credit: no healing.
 */
public final class BedRest {
	public static final int TICKS_PER_HEALTH = 20;
	/** Health points one food point pays for. */
	public static final int HEALTH_PER_FOOD = 4;
	/** The Overworld clock's night, as vanilla's beds once had it (sunset dusk to dawn), within a 24000-tick day. */
	public static final long NIGHT_START = 12542L;
	public static final long NIGHT_END = 23460L;

	/** Health points already paid for and not yet healed (0-3). */
	public static final AttachmentType<Integer> CREDIT = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "rest_credit"),
		builder -> builder.persistent(Codec.INT).initializer(() -> 0).copyOnDeath());

	/** Ticks each resting player has rested (not saved: getting up starts the count again). */
	private static final Map<ServerPlayer, Integer> RESTED = new WeakHashMap<>();

	private BedRest() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> level.players().forEach(player -> tick(level, player)));
	}

	public static boolean isNight(ServerLevel level) {
		if (level.dimensionType().hasFixedTime()) {
			return false;
		}
		long time = level.getOverworldClockTime() % 24000L;
		return time >= NIGHT_START && time < NIGHT_END;
	}

	static void tick(ServerLevel level, ServerPlayer player) {
		if (!player.isSleeping() || !player.isAlive() || !isNight(level) || player.getHealth() >= player.getMaxHealth()) {
			RESTED.remove(player);
			return;
		}
		int rested = RESTED.merge(player, 1, Integer::sum);
		if (rested % TICKS_PER_HEALTH == 0) {
			heal(player);
		}
	}

	/** Heals one health point if it can be paid for; whether it was. */
	static boolean heal(ServerPlayer player) {
		int credit = player.getAttachedOrElse(CREDIT, 0);
		FoodData food = player.getFoodData();
		if (credit == 0) {
			if (food.getFoodLevel() <= 0) {
				return false;
			}
			int left = food.getFoodLevel() - 1;
			food.setFoodLevel(left);
			// Saturation never exceeds the food level (vanilla keeps it so).
			food.setSaturation(Math.min(food.getSaturationLevel(), left));
			credit = HEALTH_PER_FOOD;
		}
		player.setAttached(CREDIT, credit - 1);
		player.heal(1.0F);
		return true;
	}
}
