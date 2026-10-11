package io.github.boxofdog.simpleblock.feature;

/**
 * FOOD-01: routine physical activity costs half of vanilla's exhaustion.
 *
 * <p>Halved (26.3 vanilla values in brackets): sprinting (0.1 per block), jumping (0.05) and
 * sprint-jumping (0.2), swimming and moving in or under water (0.01 per block), and breaking a block
 * (0.005). Walking and sneaking cost nothing in vanilla and still cost nothing. Unchanged: attacking
 * (0.1 per hit), taking damage (the damage type's exhaustion), natural regeneration (6.0 per half
 * heart, or the saturation it spends), the Hunger effect and enchantment effects.
 *
 * <p>Applied where vanilla charges these costs: {@code mixin/food/ServerPlayerMixin} (movement
 * statistics and jumping) and {@code mixin/food/BlockMixin} ({@code Block.playerDestroy}).
 * Exhaustion is server-side only, so there is nothing to do on the client.
 */
public final class Exhaustion {
	/**
	 * Locked for the first balance pass ({@code docs/M02_DECISIONS_AND_TUNING.md}), so a constant and
	 * not a tuning key.
	 */
	public static final float ACTIVITY_FACTOR = 0.5F;

	private Exhaustion() {
	}

	public static float activity(float vanillaExhaustion) {
		return vanillaExhaustion * ACTIVITY_FACTOR;
	}
}
