package io.github.boxofdog.simpleblock.cable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.feature.Feedback;

/**
 * Laying out cable. A drum is fitted with one cable coil (M06: a reusable installation, not a length of
 * cable) and has a route: the anchors the cable runs through, in order. The cable's loose end and hook
 * ({@link CableHook}) hang at the last support.
 *
 * <ul>
 *   <li><b>Fitting the coil</b>: use a cable coil on a drum. One is all a drum takes or needs.</li>
 *   <li><b>Picking up the end</b>: use the hook. It follows the hand like a lead, and slips out of it if
 *       the player walks further than a span could reach ({@code cable.max_span}, plus a player's
 *       reach).</li>
 *   <li><b>Routing</b>: holding the end, use an anchor. The span from the last support to that anchor
 *       must be at most {@code cable.max_span} blocks and clear of solid blocks; there is no limit to
 *       the number of anchors or the route's length, and the length costs nothing. The end stays in
 *       the hand for the next anchor; using the drum or the last anchor puts it down. An anchor carries
 *       one cable.</li>
 *   <li><b>Disconnecting</b>: sneak and use an anchor on the route (empty hands): the cable from there
 *       on comes off its anchors and winds back onto the drum. Breaking an anchor does the same; if the
 *       drum is unloaded, its line is cut at once and the drum winds back when it loads.</li>
 *   <li><b>Dismantling</b>: sneak and use the drum: the first time the whole route winds back in, the
 *       next the coil comes off. Breaking the drum drops the coil.</li>
 * </ul>
 *
 * The coil is never lost or made: one coil in, the same one out, whatever the route's length.
 */
public final class CableRoutes {
	/** Where the cable leaves a drum: the top of the barrel, over the middle. */
	static final double DRUM_EXIT_Y = 14.0 / 16.0;
	/** Where the cable rests on an anchor's roller. */
	static final double ROLLER_Y = 12.0 / 16.0;
	/** How far beyond its reach a held end can be before it slips out of the hand: about a player's reach. */
	static final double HOLD_SLACK = 3.0;

	private CableRoutes() {
	}

	public static int maxSpan() {
		return Tuning.getInt("cable.max_span");
	}

	/** Where the cable leaves a drum hung under a beam (upside down): about the anchors' roller height. */
	static final double HANGING_EXIT_Y = 11.0 / 16.0;

	/** Where the cable runs at a support: a standing drum's exit or an anchor's roller. */
	public static Vec3 point(BlockPos pos, boolean drum) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + (drum ? DRUM_EXIT_Y : ROLLER_Y), pos.getZ() + 0.5);
	}

	/** Where the cable leaves a drum, standing or hung under a beam. */
	public static Vec3 drumPoint(BlockPos pos, boolean hanging) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + (hanging ? HANGING_EXIT_Y : DRUM_EXIT_Y), pos.getZ() + 0.5);
	}

	/** Where the cable runs at a support in the world (a drum's exit depends on how it is mounted, an anchor's roller on its drop). */
	public static Vec3 point(BlockGetter level, BlockPos pos, boolean drum) {
		if (drum) {
			BlockState state = level.getBlockState(pos);
			return drumPoint(pos, state.hasProperty(CableDrumBlock.HANGING) && state.getValue(CableDrumBlock.HANGING));
		}
		return rollerPoint(level, pos);
	}

	/** M13 (BUG-021): one step of an anchor's roller drop, in blocks (2 px; at most 4 steps, half a block). */
	public static final double DROP_STEP = 2.0 / 16.0;

	/** How many {@link #DROP_STEP}s the roller at {@code pos} hangs low (0 if not an anchor). */
	public static int drop(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.hasProperty(CableAnchorBlock.DROP) ? state.getValue(CableAnchorBlock.DROP) : 0;
	}

	/** Where the cable runs over the anchor at {@code pos}: its roller, lowered by its drop. */
	public static Vec3 rollerPoint(BlockGetter level, BlockPos pos) {
		return point(pos, false).subtract(0.0, drop(level, pos) * DROP_STEP, 0.0);
	}

	static void setDrop(net.minecraft.world.level.Level level, BlockPos pos, int drop) {
		BlockState state = level.getBlockState(pos);
		if (state.hasProperty(CableAnchorBlock.DROP) && state.getValue(CableAnchorBlock.DROP) != drop) {
			level.setBlock(pos, state.setValue(CableAnchorBlock.DROP, drop), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
	}

	/** A span's length in whole blocks (rounded up). */
	public static int spanLength(BlockPos from, boolean fromDrum, BlockPos to) {
		return Mth.ceil(point(from, fromDrum).distanceTo(point(to, false)));
	}

	static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.cable." + key, fallback, args);
	}

	static void tell(Player player, Component message) {
		if (player instanceof ServerPlayer serverPlayer) {
			Feedback.quiet(serverPlayer, message);
		}
	}

	/** The cable end this player is holding, if any (client and server). */
	public static @Nullable CableHook heldHook(Level level, Player player) {
		List<CableHook> hooks = level.getEntitiesOfClass(CableHook.class, player.getBoundingBox().inflate(4.0), hook -> hook.holderId() == player.getId());
		return hooks.isEmpty() ? null : hooks.getFirst();
	}

	static @Nullable CableHook hook(ServerLevel level, CableDrumBlockEntity drum) {
		return drum.hookId >= 0 && level.getEntity(drum.hookId) instanceof CableHook hook && !hook.isRemoved() ? hook : null;
	}

	// ---- Using the drum ----

	static InteractionResult windCoil(Level level, BlockPos pos, Player player, ItemStack coil) {
		if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum) {
			if (drum.hasCoil()) {
				tell(player, message("coil_fitted", "The drum already has its cable: one coil reaches as far as you route it"));
			} else {
				coil.consume(1, player);
				drum.setCoil(true);
				level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
				tell(player, message("wound", "Cable fitted: run it through as many anchors as you like"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	static InteractionResult useDrum(Level level, BlockPos pos, Player player) {
		if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum)) {
			return InteractionResult.SUCCESS;
		}
		CableHook held = heldHook(level, player);
		if (player.isSecondaryUseActive() && drum.looped()) {
			boolean twoDrums = drum.endDrum() != null;
			CableLoops.open(serverLevel, drum, player);
			tell(player, CableLoops.openedMessage(twoDrums));
		} else if (player.isSecondaryUseActive()) {
			tell(player, dismantle(serverLevel, drum, player));
		} else if (held != null && held.drumPos().equals(pos)) {
			held.release();
			tell(player, message("put_down", "Cable end put down"));
		} else if (held != null) {
			// M07 playtest change 2: the end made fast to a second drum, to work the line from both ends.
			tell(player, FarDrums.attach(serverLevel, held, drum));
		} else {
			tell(player, status(drum));
		}
		return InteractionResult.SUCCESS;
	}

	static Component status(CableDrumBlockEntity drum) {
		if (!drum.hasCable()) {
			return message("drum_empty", "Empty cable drum: use a cable coil on it");
		}
		if (drum.looped() && drum.endDrum() != null) {
			return message("drum_two", "Cable to the drum at %s: %s blocks through %s anchors, worked from both ends", drum.endDrum().toShortString(),
				drum.laidOut(), drum.route().size());
		}
		if (drum.looped()) {
			return message("drum_loop", "Cable loop: %s blocks through %s anchors", drum.laidOut(), drum.route().size());
		}
		return message("drum_status", "Cable drum: %s blocks laid out through %s anchors", drum.laidOut(), drum.route().size());
	}

	/** One dismantling step: wind the route back in, or take the cable off as coils. */
	private static Component dismantle(ServerLevel level, CableDrumBlockEntity drum, Player player) {
		if (!drum.route().isEmpty()) {
			int anchors = disconnect(level, drum, 0);
			level.playSound(null, drum.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			return message("rewound", "Cable wound back in from %s anchors", anchors);
		}
		if (drum.hasCoil()) {
			drum.setCoil(false);
			player.getInventory().placeItemBackInInventory(new ItemStack(CableContent.CABLE_COIL), Prediction.SERVER_ONLY);
			level.playSound(null, drum.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			return message("taken_off", "Cable coil taken off the drum");
		}
		return status(drum);
	}

	// ---- Using an anchor ----

	static InteractionResult useAnchor(Level level, BlockPos pos, Player player) {
		CableHook held = heldHook(level, player);
		boolean sneaking = player.isSecondaryUseActive();
		if (held == null && !sneaking && !player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof CableAnchorBlockEntity anchor)) {
			return InteractionResult.SUCCESS;
		}
		CableDrumBlockEntity owner = owner(serverLevel, anchor);
		if (sneaking) {
			if (owner == null) {
				tell(player, message("not_routed", "No cable runs through this anchor"));
			} else if (owner.looped()) {
				boolean twoDrums = owner.endDrum() != null;
				CableLoops.open(serverLevel, owner, player);
				tell(player, CableLoops.openedMessage(twoDrums));
			} else {
				int freed = disconnect(serverLevel, owner, owner.route().indexOf(pos));
				level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
				tell(player, message("disconnected", "Cable disconnected here: wound back from %s anchors", freed));
			}
		} else if (held == null) {
			tell(player, owner != null
				? message("anchor_routed", "This anchor carries the cable from the drum at %s", owner.getBlockPos().toShortString())
				: message("anchor_free", "Free anchor: pick up a drum's cable end, then use the anchor to run the cable through it"));
		} else if (held.drum(serverLevel) instanceof CableDrumBlockEntity drum) {
			tell(player, routeThrough(serverLevel, drum, anchor, owner, held));
		}
		return InteractionResult.SUCCESS;
	}

	/** The drum whose route runs through this anchor. Clears a record left by a drum that is gone or no longer routes here. */
	private static @Nullable CableDrumBlockEntity owner(ServerLevel level, CableAnchorBlockEntity anchor) {
		BlockPos drumPos = anchor.drum();
		if (drumPos == null || !level.isLoaded(drumPos)) {
			return null;
		}
		if (level.getBlockEntity(drumPos) instanceof CableDrumBlockEntity drum && drum.route().contains(anchor.getBlockPos())) {
			return drum;
		}
		anchor.setDrum(null);
		return null;
	}

	private static Component routeThrough(ServerLevel level, CableDrumBlockEntity drum, CableAnchorBlockEntity anchor, @Nullable CableDrumBlockEntity owner,
		CableHook hook) {
		BlockPos pos = anchor.getBlockPos();
		if (owner == drum) {
			if (pos.equals(drum.routeEnd())) {
				hook.release();
				return message("put_down", "Cable end put down");
			}
			// M13 (BUG-020): the end used on an earlier anchor of its own route winds the cable back to it:
			// the anchors after it are freed, the cable comes off them onto the drum, and the end hangs here.
			int freed = disconnect(level, drum, drum.route().indexOf(pos) + 1);
			level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			return message("wound_back", "Cable wound back to this anchor from %s anchors", freed);
		}
		if (anchor.drum() != null) {
			return message("anchor_taken", "This anchor already carries a cable");
		}
		BlockPos from = drum.routeEnd();
		boolean fromDrum = drum.route().isEmpty();
		Vec3 start = fromDrum ? drum.exitPoint() : rollerPoint(level, from);
		double distance = start.distanceTo(point(pos, false));
		int length = Mth.ceil(distance);
		if (distance > maxSpan()) {
			return message("too_far", "Too far: a span can be at most %s blocks, this one would be %s", maxSpan(), length);
		}
		Set<BlockPos> ignored = fromDrum ? supports(from, drum.hanging(), pos) : supports(from, true, pos);
		BlockPos obstacle = obstacle(level, start, point(pos, false), ignored);
		int drop = 0;
		// M13 (BUG-021): an inclined span that catches something on its way may hang this anchor's roller
		// lower, a step at a time up to half a block, just enough to clear it.
		while (obstacle != null && Math.abs(start.y - point(pos, false).y) > 1.0E-6 && drop < 4) {
			drop++;
			obstacle = obstacle(level, start, point(pos, false).subtract(0.0, drop * DROP_STEP, 0.0), ignored);
		}
		if (obstacle != null) {
			return message("blocked", "The cable would run through %s at %s", level.getBlockState(obstacle).getBlock().getName(), obstacle.toShortString());
		}
		setDrop(level, pos, drop);
		anchor.setDrum(drum.getBlockPos());
		drum.addAnchor(pos);
		level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
		return message("routed", "Cable run through the anchor: a %s-block span, %s blocks of route", length, drum.laidOut());
	}

	/**
	 * The first block with a collision shape that the straight span from {@code from} to {@code to}
	 * passes through, or null if it is clear. The two supports are ignored, and so are the wooden blocks
	 * the anchors hang from: the cable runs on the roller just under them, so a steep span would
	 * otherwise catch the beam's edge.
	 */
	public static @Nullable BlockPos obstacle(BlockGetter level, BlockPos from, boolean fromDrum, BlockPos to) {
		return obstacle(level, point(level, from, fromDrum), rollerPoint(level, to), supports(from, !fromDrum, to));
	}

	/** The supports a span from {@code from} to the anchor {@code to} may pass: both ends and the beam over the anchor (and over {@code from}, if it hangs). */
	private static Set<BlockPos> supports(BlockPos from, boolean fromHangs, BlockPos to) {
		Set<BlockPos> ignored = new HashSet<>(List.of(from, to, to.above()));
		if (fromHangs) {
			ignored.add(from.above());
		}
		return ignored;
	}

	/** The first block with a collision shape that the straight line from {@code start} to {@code end} passes through, apart from {@code ignored}. */
	public static @Nullable BlockPos obstacle(BlockGetter level, Vec3 start, Vec3 end, Set<BlockPos> ignored) {
		return BlockGetter.traverseBlocks(start, end, level, (getter, pos) -> {
			if (ignored.contains(pos)) {
				return null;
			}
			VoxelShape shape = getter.getBlockState(pos).getCollisionShape(getter, pos);
			return !shape.isEmpty() && shape.clip(start, end, pos) != null ? pos.immutable() : null;
		}, getter -> null);
	}

	/**
	 * Cuts the route at {@code index}: the cable comes off that anchor and every one after it and winds
	 * back onto the drum. A held end is put down. Returns the number of anchors freed.
	 */
	static int disconnect(ServerLevel level, CableDrumBlockEntity drum, int index) {
		CableLoops.open(level, drum, null);
		// M07: the cable end comes off any skip hung on it.
		CableHoists.detach(level, drum.getBlockPos());
		List<BlockPos> cut = drum.cutFrom(index);
		for (BlockPos anchor : cut) {
			if (level.isLoaded(anchor) && level.getBlockEntity(anchor) instanceof CableAnchorBlockEntity freed && drum.getBlockPos().equals(freed.drum())) {
				freed.setDrum(null);
			}
		}
		CableHook hook = hook(level, drum);
		if (hook != null) {
			hook.release();
		}
		return cut.size();
	}

	// ---- The world changing ----

	static void anchorRemoved(ServerLevel level, BlockPos drumPos, BlockPos anchor) {
		if (level.isLoaded(drumPos) && level.getBlockEntity(drumPos) instanceof CableDrumBlockEntity drum) {
			int index = drum.route().indexOf(anchor);
			if (index >= 0) {
				disconnect(level, drum, index);
			}
		} else {
			// M06: the drum is far away and unloaded. Stop its line here now; the drum winds back when it loads.
			CableLines.cut(level, drumPos, anchor);
		}
	}

	static void drumRemoved(ServerLevel level, CableDrumBlockEntity drum) {
		CableLines.Line line = CableLines.line(level, drum.getBlockPos());
		// A loop opens: the anchors lose their return parts and the wheel's anchor drops.
		CableLoops.open(level, drum, null);
		for (BlockPos anchor : drum.route()) {
			if (level.isLoaded(anchor) && level.getBlockEntity(anchor) instanceof CableAnchorBlockEntity freed && drum.getBlockPos().equals(freed.drum())) {
				freed.setDrum(null);
			}
		}
		CableHook hook = hook(level, drum);
		if (hook != null) {
			hook.discard();
		}
		if (drum.hasCoil()) {
			Block.popResource(level, drum.getBlockPos(), new ItemStack(CableContent.CABLE_COIL));
		}
		// M06 coal winch: the furnace and the unburnt coal come back too (the coal alight is spent).
		if (line != null && line.powered) {
			Block.popResource(level, drum.getBlockPos(), new ItemStack(Items.FURNACE));
			for (int left = line.coal; left > 0; left -= 64) {
				Block.popResource(level, drum.getBlockPos(), new ItemStack(Items.COAL, Math.min(64, left)));
			}
		}
		CableLines.remove(level, drum.getBlockPos());
	}

	/**
	 * Every tick: checks a loaded route against the world (once, when its anchors are loaded) and keeps
	 * the loose end in the world while there is cable.
	 */
	static void tickDrum(ServerLevel level, CableDrumBlockEntity drum) {
		if (!drum.validated && (!drum.checked || level.getGameTime() % 20 == 0)) {
			validate(level, drum);
		}
		CableHook hook = hook(level, drum);
		// M07: while a skip hangs on the cable end, the end (and its hook) is made fast to the skip's bridle.
		if (!drum.hasCable() || drum.looped() || CableHoists.hoisting(level, drum.getBlockPos())) {
			if (hook != null) {
				hook.discard();
			}
			drum.hookId = -1;
		} else if (hook == null) {
			CableHook fresh = new CableHook(level, drum);
			if (level.isPositionEntityTicking(fresh.blockPosition()) && level.addFreshEntity(fresh)) {
				drum.hookId = fresh.getId();
			}
		}
	}

	/** Winds the cable back from the first anchor that is missing or not on this route any more. */
	private static void validate(ServerLevel level, CableDrumBlockEntity drum) {
		List<BlockPos> route = drum.route();
		boolean complete = true;
		for (int i = 0; i < route.size(); i++) {
			BlockPos pos = route.get(i);
			if (!level.isLoaded(pos)) {
				complete = false;
				break;
			}
			if (!(level.getBlockEntity(pos) instanceof CableAnchorBlockEntity anchor) || !drum.getBlockPos().equals(anchor.drum())) {
				disconnect(level, drum, i);
				break;
			}
		}
		drum.validated = complete;
		drum.checked = true;
	}
}
