package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * Laying out cable. A drum holds cable wound on it ({@link CableDrumBlockEntity#stored}, in blocks) and
 * a route: the anchors the laid-out cable runs through, in order. The cable's loose end and hook
 * ({@link CableHook}) hang at the last support.
 *
 * <ul>
 *   <li><b>Winding on</b>: use a cable coil on a drum ({@value CableContent#BLOCKS_PER_COIL} blocks, up to
 *       {@code cable.drum_capacity} in all).</li>
 *   <li><b>Picking up the end</b>: use the hook. It follows the hand like a lead, and slips out of it if
 *       the player walks further than the cable could reach (the longest span or the cable left on the
 *       drum, plus a player's reach).</li>
 *   <li><b>Routing</b>: holding the end, use an anchor. The span from the last support to that anchor
 *       must be at most {@code cable.max_span} blocks, clear of solid blocks, and its length (rounded up
 *       to whole blocks) comes off the drum. The end stays in the hand for the next anchor; using the
 *       drum or the last anchor puts it down. An anchor carries one cable.</li>
 *   <li><b>Disconnecting</b>: sneak and use an anchor on the route (empty hands): the cable from there
 *       on comes off its anchors and winds back onto the drum. Breaking an anchor does the same.</li>
 *   <li><b>Dismantling</b>: sneak and use the drum: the first time the whole route winds back in, the
 *       next the cable comes off as coils. Breaking the drum drops all its cable as coils.</li>
 * </ul>
 *
 * Cable is never lost or made: the drum's cable (wound plus laid out) is always the coils wound on.
 * Nothing moves the cable yet (no winch power, no traction: later milestones).
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

	public static int capacity() {
		return Tuning.getInt("cable.drum_capacity");
	}

	public static int maxSpan() {
		return Tuning.getInt("cable.max_span");
	}

	/** Where the cable runs at a support: the drum's exit or an anchor's roller. */
	public static Vec3 point(BlockPos pos, boolean drum) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + (drum ? DRUM_EXIT_Y : ROLLER_Y), pos.getZ() + 0.5);
	}

	/** Cable a span uses, in whole blocks (rounded up). */
	public static int spanLength(BlockPos from, boolean fromDrum, BlockPos to) {
		return Mth.ceil(point(from, fromDrum).distanceTo(point(to, false)));
	}

	static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.cable." + key, fallback, args);
	}

	static void tell(Player player, Component message) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(message);
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
			if (drum.stored() + drum.laidOut() + CableContent.BLOCKS_PER_COIL > capacity()) {
				tell(player, message("drum_full", "The drum is full: it holds %s blocks of cable", capacity()));
			} else {
				coil.consume(1, player);
				drum.setStored(drum.stored() + CableContent.BLOCKS_PER_COIL);
				level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
				tell(player, message("wound", "Coil wound on: %s blocks of cable on the drum", drum.stored()));
			}
		}
		return InteractionResult.SUCCESS;
	}

	static InteractionResult useDrum(Level level, BlockPos pos, Player player) {
		if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum)) {
			return InteractionResult.SUCCESS;
		}
		CableHook held = heldHook(level, player);
		if (player.isSecondaryUseActive()) {
			tell(player, dismantle(serverLevel, drum, player));
		} else if (held != null && held.drumPos().equals(pos)) {
			held.release();
			tell(player, message("put_down", "Cable end put down"));
		} else {
			tell(player, status(drum));
		}
		return InteractionResult.SUCCESS;
	}

	static Component status(CableDrumBlockEntity drum) {
		if (!drum.hasCable()) {
			return message("drum_empty", "Empty cable drum: use a cable coil on it");
		}
		return message("drum_status", "Cable drum: %s blocks wound on, %s laid out through %s anchors", drum.stored(), drum.laidOut(), drum.route().size());
	}

	/** One dismantling step: wind the route back in, or take the cable off as coils. */
	private static Component dismantle(ServerLevel level, CableDrumBlockEntity drum, Player player) {
		if (!drum.route().isEmpty()) {
			disconnect(level, drum, 0);
			level.playSound(null, drum.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			return message("rewound", "Cable wound back in: %s blocks on the drum", drum.stored());
		}
		if (drum.stored() > 0) {
			int blocks = drum.stored();
			drum.setStored(0);
			for (ItemStack stack : asItems(blocks)) {
				player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
			}
			level.playSound(null, drum.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			return message("taken_off", "Cable taken off the drum: %s blocks", blocks);
		}
		return status(drum);
	}

	/** Cable as items: whole coils, then single cables for the rest (rounded down). */
	static List<ItemStack> asItems(int blocks) {
		List<ItemStack> stacks = new ArrayList<>();
		int coils = blocks / CableContent.BLOCKS_PER_COIL;
		int cables = blocks % CableContent.BLOCKS_PER_COIL / CableContent.BLOCKS_PER_CABLE;
		for (int left = coils; left > 0; left -= CableContent.CABLE_COIL.getDefaultMaxStackSize()) {
			stacks.add(new ItemStack(CableContent.CABLE_COIL, Math.min(left, CableContent.CABLE_COIL.getDefaultMaxStackSize())));
		}
		if (cables > 0) {
			stacks.add(new ItemStack(CableContent.CABLE, cables));
		}
		return stacks;
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
			} else {
				int rewound = disconnect(serverLevel, owner, owner.route().indexOf(pos));
				level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
				tell(player, message("disconnected", "Cable disconnected here: %s blocks wound back onto the drum", rewound));
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
			return message("already_routed", "The cable already runs through this anchor");
		}
		if (anchor.drum() != null) {
			return message("anchor_taken", "This anchor already carries a cable");
		}
		BlockPos from = drum.routeEnd();
		boolean fromDrum = drum.route().isEmpty();
		double distance = point(from, fromDrum).distanceTo(point(pos, false));
		int length = Mth.ceil(distance);
		if (distance > maxSpan()) {
			return message("too_far", "Too far: a span can be at most %s blocks, this one would be %s", maxSpan(), length);
		}
		if (length > drum.stored()) {
			return message("too_short", "Not enough cable: this span needs %s blocks, the drum has %s", length, drum.stored());
		}
		BlockPos obstacle = obstacle(level, from, fromDrum, pos);
		if (obstacle != null) {
			return message("blocked", "The cable would run through %s at %s", level.getBlockState(obstacle).getBlock().getName(), obstacle.toShortString());
		}
		anchor.setDrum(drum.getBlockPos());
		drum.addAnchor(pos);
		drum.setStored(drum.stored() - length);
		level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
		if (drum.stored() == 0) {
			hook.release();
			return message("routed_last", "Cable run through the anchor (%s blocks): that was the last of it", length);
		}
		return message("routed", "Cable run through the anchor: %s blocks used, %s left on the drum", length, drum.stored());
	}

	/**
	 * The first block with a collision shape that the straight span from {@code from} to {@code to}
	 * passes through, or null if it is clear. The two supports are ignored, and so are the wooden blocks
	 * the anchors hang from: the cable runs on the roller just under them, so a steep span would
	 * otherwise catch the beam's edge.
	 */
	public static @Nullable BlockPos obstacle(BlockGetter level, BlockPos from, boolean fromDrum, BlockPos to) {
		Vec3 start = point(from, fromDrum);
		Vec3 end = point(to, false);
		return BlockGetter.traverseBlocks(start, end, level, (getter, pos) -> {
			if (pos.equals(from) || pos.equals(to) || pos.equals(to.above()) || !fromDrum && pos.equals(from.above())) {
				return null;
			}
			VoxelShape shape = getter.getBlockState(pos).getCollisionShape(getter, pos);
			return !shape.isEmpty() && shape.clip(start, end, pos) != null ? pos.immutable() : null;
		}, getter -> null);
	}

	/**
	 * Cuts the route at {@code index}: the cable comes off that anchor and every one after it and winds
	 * back onto the drum. A held end is put down. Returns the blocks of cable wound back.
	 */
	static int disconnect(ServerLevel level, CableDrumBlockEntity drum, int index) {
		int before = drum.laidOut();
		for (BlockPos anchor : drum.cutFrom(index)) {
			if (level.isLoaded(anchor) && level.getBlockEntity(anchor) instanceof CableAnchorBlockEntity freed && drum.getBlockPos().equals(freed.drum())) {
				freed.setDrum(null);
			}
		}
		int rewound = before - drum.laidOut();
		drum.setStored(drum.stored() + rewound);
		CableHook hook = hook(level, drum);
		if (hook != null) {
			hook.release();
		}
		return rewound;
	}

	// ---- The world changing ----

	static void anchorRemoved(ServerLevel level, BlockPos drumPos, BlockPos anchor) {
		if (level.isLoaded(drumPos) && level.getBlockEntity(drumPos) instanceof CableDrumBlockEntity drum) {
			int index = drum.route().indexOf(anchor);
			if (index >= 0) {
				disconnect(level, drum, index);
			}
		}
	}

	static void drumRemoved(ServerLevel level, CableDrumBlockEntity drum) {
		int blocks = drum.stored() + drum.laidOut();
		for (BlockPos anchor : drum.route()) {
			if (level.isLoaded(anchor) && level.getBlockEntity(anchor) instanceof CableAnchorBlockEntity freed && drum.getBlockPos().equals(freed.drum())) {
				freed.setDrum(null);
			}
		}
		CableHook hook = hook(level, drum);
		if (hook != null) {
			hook.discard();
		}
		for (ItemStack stack : asItems(blocks)) {
			Block.popResource(level, drum.getBlockPos(), stack);
		}
	}

	/**
	 * Every tick: checks a loaded route against the world (once, when its anchors are loaded) and keeps
	 * the loose end in the world while there is cable.
	 */
	static void tickDrum(ServerLevel level, CableDrumBlockEntity drum) {
		if (!drum.validated && (!drum.indexed || level.getGameTime() % 20 == 0)) {
			validate(level, drum);
		}
		CableHook hook = hook(level, drum);
		if (!drum.hasCable()) {
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
		drum.indexed = true;
		CableIndex.update(level, drum);
	}
}
