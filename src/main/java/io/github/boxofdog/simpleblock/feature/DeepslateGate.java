package io.github.boxofdog.simpleblock.feature;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * MINE-01: natural deepslate and every deepslate ore need an iron, diamond or netherite pickaxe.
 *
 * <p>The blocks are listed in {@code #simple_block:requires_iron_pickaxe} and added to
 * {@code #minecraft:needs_iron_tool}, which makes wooden, stone, copper and golden pickaxes the
 * wrong tool (no drops). {@code mixin/mining/BlockBehaviourMixin} then makes their mining progress
 * zero with any wrong tool or bare hands, so the block cannot be broken at all rather than
 * breaking slowly for nothing. Crafted deepslate (cobbled, polished, bricks, tiles, slabs, stairs,
 * walls, chiseled) keeps its vanilla requirements because it is not in the tag.
 */
public final class DeepslateGate {
	public static final TagKey<Block> REQUIRES_IRON_PICKAXE =
		TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "requires_iron_pickaxe"));

	private DeepslateGate() {
	}

	/** True when this player cannot make any mining progress on this block. */
	public static boolean blocksMining(BlockState state, Player player) {
		return state.is(REQUIRES_IRON_PICKAXE) && !player.getAbilities().instabuild && !player.hasCorrectToolForDrops(state);
	}
}
