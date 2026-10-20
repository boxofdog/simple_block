package io.github.boxofdog.simpleblock.rope;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * M13: a bow in the main hand with a Spelunker's Rope in the offhand shoots the rope instead of an arrow
 * ({@code mixin/rope/BowItemMixin}): it draws with or without arrows, and a loosed draw sends one rope as a
 * {@link RopeShot}, using up no arrow and wearing the bow as an arrow would. No rope in the offhand: the
 * bow is vanilla's. Bow enchantments (Power, Punch, Flame, Infinity) do nothing to a rope, and every shot
 * uses a rope (none in creative, as with placing one by hand).
 *
 * <p>Tuning, for the owner's playtest: a rope leaves the bow at {@link #SPEED} of an arrow's speed for the
 * same draw; with the arrow's own drag and gravity, a full draw carries roughly four fifths as far
 * as an arrow (measured in {@code RopeGameTests}).
 */
public final class BowRope {
	public static final float SPEED = 0.9F;

	private BowRope() {
	}

	/** Whether {@code player} using the bow in {@code hand} shoots rope. */
	public static boolean armed(Player player, InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND && player.getOffhandItem().is(RopeContent.SPELUNKER_ROPE);
	}

	/** Looses the drawn bow: a rope shot, if the draw is enough to shoot at all. Whether it shot. */
	public static boolean release(ItemStack bow, Level level, Player player, int timeHeld) {
		float power = BowItem.getPowerForTime(timeHeld);
		if (power < 0.1F) {
			return false;
		}
		if (level instanceof ServerLevel serverLevel) {
			ItemStack rope = player.getOffhandItem().consumeAndReturn(1, player);
			RopeShot shot = new RopeShot(serverLevel, player, rope);
			shot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F * SPEED, 1.0F);
			serverLevel.addFreshEntity(shot);
			bow.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
			1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
		player.awardStat(Stats.ITEM_USED.get(bow.getItem()));
		return true;
	}
}
