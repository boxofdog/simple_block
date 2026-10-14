package io.github.boxofdog.simpleblock.rope;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.boxofdog.simpleblock.feature.Feedback;

/**
 * M10 Spelunker's Rope: an anchor fixed under a block or on its side, with a climbing rope hanging
 * straight down from it through open air ({@link RopeContent}).
 *
 * <ul>
 *   <li><b>Placing</b> a Spelunker's Rope on the bottom or a side of a solid block fixes the anchor in
 *       the open block there and lowers its own rope: 12 blocks, or to the ground.</li>
 *   <li><b>Extending:</b> use (or hold use with) Rope on any part of the line: each Rope lowers it 12
 *       blocks more. Length already paid for and lying coiled is let out first, without using a Rope.
 *       Nothing is used when the line already stands on the ground.</li>
 *   <li><b>The ground:</b> the line goes down only through open air (no fluids, no blocks); where it
 *       stops with rope to spare, the spare lies in a coil on the lowest block.</li>
 *   <li><b>Taking it down:</b> sneak-use any part with an empty hand: the whole line comes up, into the
 *       inventory, as the one Spelunker's Rope and every Rope that was used on it. Breaking any part of
 *       it, or its support going, takes the whole line down the same way, dropping them there. Nothing
 *       is ever made or lost.</li>
 * </ul>
 * Climbing it is ordinary climbing ({@code #minecraft:climbable}): Spiked Boots climb it faster. It is
 * not a cable: nothing in the M04–M07 haulage sees it.
 */
public final class SpelunkerRopes {
	/** Blocks of line one Rope (or the Spelunker's Rope itself) pays for. */
	public static final int LENGTH = 12;
	/** Set while a line is being taken down, so its parts going do not take it down again. */
	private static boolean retrieving;

	private SpelunkerRopes() {
	}

	static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.rope." + key, fallback, args);
	}

	static void tell(Player player, Component message) {
		Feedback.quiet(player, message);
	}

	/** Whether the line may go down into this block: open air only. */
	static boolean open(BlockState state) {
		return state.isAir();
	}

	static boolean isLine(BlockState state) {
		return state.is(RopeContent.ROPE_BLOCK) || state.is(RopeContent.ROPE_COIL);
	}

	/** The anchor whose line {@code pos} is part of (the anchor itself, or straight above), or null. */
	static @Nullable BlockPos anchorOf(Level level, BlockPos pos) {
		BlockPos at = pos;
		for (int i = 0; i < level.getHeight(); i++) {
			BlockState state = level.getBlockState(at);
			if (state.is(RopeContent.ROPE_ANCHOR)) {
				return at;
			}
			if (!isLine(state)) {
				return null;
			}
			at = at.above();
		}
		return null;
	}

	/** The lowest block of the line under {@code anchor} (the anchor itself if it has none). */
	static BlockPos bottom(Level level, BlockPos anchor) {
		BlockPos at = anchor;
		while (isLine(level.getBlockState(at.below()))) {
			at = at.below();
		}
		return at;
	}

	/**
	 * Lets the line out as far as its paid-for length allows (or the air below does). The bottom block is
	 * a coil if rope is left over against the ground, plain rope otherwise. Returns the blocks added.
	 */
	static int lower(ServerLevel level, BlockPos anchor, RopeAnchorBlockEntity entity) {
		BlockPos bottom = bottom(level, anchor);
		int used = anchor.getY() - bottom.getY();
		int budget = entity.paid() * LENGTH;
		if (level.getBlockState(bottom).is(RopeContent.ROPE_COIL)) {
			level.setBlock(bottom, RopeContent.ROPE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		int added = 0;
		BlockPos at = bottom;
		while (used + added < budget && level.isInWorldBounds(at.below()) && open(level.getBlockState(at.below()))) {
			at = at.below();
			level.setBlock(at, RopeContent.ROPE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
			added++;
		}
		if (used + added < budget && !at.equals(anchor)) {
			// Against the ground with rope to spare: it lies coiled on the lowest block.
			level.setBlock(at, RopeContent.ROPE_COIL.defaultBlockState(), Block.UPDATE_ALL);
		}
		return added;
	}

	/** Rope used on a line: lets out what is paid for, or pays for 12 more with one Rope. */
	static void extend(ServerLevel level, BlockPos pos, Player player, ItemStack rope) {
		BlockPos anchor = anchorOf(level, pos);
		if (anchor == null || !(level.getBlockEntity(anchor) instanceof RopeAnchorBlockEntity entity)) {
			return;
		}
		BlockPos bottom = bottom(level, anchor);
		if (!level.isInWorldBounds(bottom.below()) || !open(level.getBlockState(bottom.below()))) {
			tell(player, message("on_ground", "The rope already reaches the ground"));
			return;
		}
		int used = anchor.getY() - bottom.getY();
		boolean spare = used < entity.paid() * LENGTH;
		if (!spare) {
			entity.addPaid();
			rope.consume(1, player);
		}
		int added = lower(level, anchor, entity);
		level.playSound(null, bottom, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
		tell(player, message("lowered", "Rope let down %s blocks: %s in all", added, anchor.getY() - bottom(level, anchor).getY()));
	}

	/**
	 * Takes the whole line down: {@code part} is a block of it (or, if {@code gap} is set, the block just
	 * above {@code gap}, a part that has already gone). Every part goes; the Spelunker's Rope and the Ropes
	 * used on it go to {@code player}'s inventory, or drop at {@code dropAt}.
	 */
	static void retrieve(ServerLevel level, BlockPos part, @Nullable BlockPos gap, @Nullable Player player, BlockPos dropAt,
		@Nullable RopeAnchorBlockEntity known) {
		if (retrieving) {
			return;
		}
		retrieving = true;
		try {
			BlockPos anchor = known != null ? known.getBlockPos() : anchorOf(level, part);
			RopeAnchorBlockEntity entity = known != null ? known
				: anchor != null && level.getBlockEntity(anchor) instanceof RopeAnchorBlockEntity found ? found : null;
			int paid = entity == null ? 0 : entity.paid();
			// From the anchor (or the highest part left) down, over the gap where a part has already gone.
			BlockPos at = anchor != null ? anchor : part;
			while (level.isInWorldBounds(at)) {
				if (known != null && at.equals(known.getBlockPos())) {
					// The anchor being removed is going anyway.
					at = at.below();
					continue;
				}
				BlockState state = level.getBlockState(at);
				boolean ours = state.is(RopeContent.ROPE_ANCHOR) || isLine(state);
				if (ours) {
					level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				} else if (!at.equals(gap)) {
					break;
				}
				at = at.below();
			}
			if (paid > 0) {
				give(level, player, dropAt, new ItemStack(RopeContent.SPELUNKER_ROPE));
				if (paid > 1) {
					give(level, player, dropAt, new ItemStack(RopeContent.ROPE, paid - 1));
				}
			}
			level.playSound(null, dropAt, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
		} finally {
			retrieving = false;
		}
	}

	private static void give(ServerLevel level, @Nullable Player player, BlockPos at, ItemStack stack) {
		if (player != null) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		} else {
			Block.popResource(level, at, stack);
		}
	}

	static boolean retrieving() {
		return retrieving;
	}

	/** Whether an anchor may hang on {@code support} from its {@code face}: a sturdy face, bottom or side. */
	static boolean canHangOn(Level level, BlockPos support, Direction face) {
		return face != Direction.UP && level.getBlockState(support).isFaceSturdy(level, support, face);
	}
}
