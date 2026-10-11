package io.github.boxofdog.simpleblock.feature;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * LIGHT-01: ordinary torches give block light 12 instead of 14. The copper torch is the same kind
 * of hand-made torch and gets 12 too. Lanterns already give 15 in vanilla and are unchanged, as are
 * soul torches/lanterns, redstone torches and every other light source.
 * Applied in {@code mixin/light/BlockStateBaseMixin}.
 *
 * <p><b>Work in progress:</b> not wired in yet (its mixin is not in simple_block.mixins.json); see
 * docs/MILESTONE_02_CHECKPOINT.md.
 */
public final class LightLevels {
	public static final int TORCH = 12;

	private LightLevels() {
	}

	public static boolean isTorch(Block block) {
		return block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.COPPER_TORCH || block == Blocks.COPPER_WALL_TORCH;
	}
}
