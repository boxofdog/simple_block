package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * M05 closed cable loops. An open M04 route (drum, anchors, loose end at the last anchor) becomes a
 * loop when a copper anchor is used on its last anchor: that anchor gets a return wheel (the anchor
 * used is fitted into it), and a return strand runs from the wheel back along the route to the drum,
 * beside the working strand on the anchors' arm side ({@link #RETURN_OFFSET} away, same height), over
 * small return guides on the anchors in between. The drum then works as a capstan ({@link CableDrive}):
 * turning it moves the working strand, which the carts' rods grip, out or back. The loose end and its
 * hook are spliced into the loop and come back when it is opened.
 *
 * <p>Rules for closing:
 * <ul>
 *   <li>the route must be straight: drum and anchors in one line along a world axis, climbing or
 *       falling as it likes, but not turning (refused, naming the anchor where it turns);</li>
 *   <li>the return strand must be clear of solid blocks, like any span;</li>
 *   <li>the drum must still hold enough cable for it: the return spans plus {@value #TURN_CABLE} blocks
 *       for the turns round the wheel and the drum. The cable comes off the drum, and goes back on when
 *       the loop opens.</li>
 * </ul>
 * Closing lines the drum and the anchors up with the route, so their arms and wheel face the right way.
 *
 * <p>Opening: sneak-use any anchor of the loop, or the drum, with empty hands (the anchor in the
 * wheel comes back), or break an anchor (the wheel's anchor drops with it) or the drum. Opening never
 * dismantles the route; only a further sneak-use does, as in M04.
 */
public final class CableLoops {
	/** Distance from the working strand to the return strand, in blocks (4.75 model pixels). */
	public static final double RETURN_OFFSET = 4.75 / 16.0;
	/** How far beyond the last anchor's roller the return wheel turns the cable. */
	public static final double WHEEL_OFFSET = 5.0 / 16.0;
	/** Cable round the return wheel and the drum, in blocks. */
	public static final int TURN_CABLE = 3;

	private CableLoops() {
	}

	/**
	 * The direction a straight route runs from its drum along a world axis, or null if it turns (or is
	 * empty). Anchors must keep moving away from the drum.
	 */
	public static @Nullable Direction straightDirection(BlockPos drum, List<BlockPos> route) {
		if (route.isEmpty()) {
			return null;
		}
		Direction direction = axisDirection(drum, route.getFirst());
		return direction != null && turn(drum, route, direction) == null ? direction : null;
	}

	/** The horizontal direction from {@code from} to {@code to} if they are on one line along a world axis, else null. */
	private static @Nullable Direction axisDirection(BlockPos from, BlockPos to) {
		if (to.getZ() == from.getZ() && to.getX() != from.getX()) {
			return to.getX() > from.getX() ? Direction.EAST : Direction.WEST;
		}
		if (to.getX() == from.getX() && to.getZ() != from.getZ()) {
			return to.getZ() > from.getZ() ? Direction.SOUTH : Direction.NORTH;
		}
		return null;
	}

	/** The first anchor that leaves the line from the drum in {@code direction}, or null if none does. */
	private static @Nullable BlockPos turn(BlockPos drum, List<BlockPos> route, Direction direction) {
		BlockPos previous = drum;
		for (BlockPos anchor : route) {
			boolean onLine = direction.getAxis() == Direction.Axis.X ? anchor.getZ() == drum.getZ() : anchor.getX() == drum.getX();
			int step = (anchor.getX() - previous.getX()) * direction.getStepX() + (anchor.getZ() - previous.getZ()) * direction.getStepZ();
			if (!onLine || step <= 0) {
				return anchor;
			}
			previous = anchor;
		}
		return null;
	}

	/** From the working strand to the return strand: the anchors' arm side (west of a north-south route, north of an east-west one). */
	public static Vec3 returnOffset(Direction outward) {
		return outward.getAxis() == Direction.Axis.Z ? new Vec3(-RETURN_OFFSET, 0.0, 0.0) : new Vec3(0.0, 0.0, -RETURN_OFFSET);
	}

	/** The return strand's points, drum first: the working strand's points moved to the arm side. */
	public static List<Vec3> returnPoints(CableDrumBlockEntity drum, Direction outward) {
		Vec3 offset = returnOffset(outward);
		return drum.points().stream().map(point -> point.add(offset)).toList();
	}

	/** Cable the return strand takes, in blocks. */
	static int returnCable(List<Vec3> points) {
		int total = TURN_CABLE;
		for (int i = 1; i < points.size(); i++) {
			total += (int) Math.ceil(points.get(i - 1).distanceTo(points.get(i)));
		}
		return total;
	}

	/** The drum whose open or looped route ends at {@code anchorPos} (client and server: both know routes). */
	static @Nullable CableDrumBlockEntity routeEndDrum(Level level, BlockPos anchorPos) {
		if (level.getBlockEntity(anchorPos) instanceof CableAnchorBlockEntity anchor && anchor.drum() != null && level.isLoaded(anchor.drum())
			&& level.getBlockEntity(anchor.drum()) instanceof CableDrumBlockEntity drum && !drum.route().isEmpty()
			&& drum.routeEnd().equals(anchorPos)) {
			return drum;
		}
		return null;
	}

	/** Using a copper anchor on the last anchor of a route: closes it into a loop, if it can. */
	static InteractionResult close(Level level, BlockPos pos, Player player, ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel) || !(routeEndDrum(level, pos) instanceof CableDrumBlockEntity drum)) {
			return InteractionResult.SUCCESS;
		}
		if (drum.looped()) {
			CableRoutes.tell(player, CableRoutes.message("loop_already", "This route is already a loop"));
			return InteractionResult.SUCCESS;
		}
		BlockPos drumPos = drum.getBlockPos();
		List<BlockPos> route = drum.route();
		Direction outward = straightDirection(drumPos, route);
		if (outward == null) {
			BlockPos at = route.getFirst();
			Direction first = axisDirection(drumPos, at);
			if (first != null && turn(drumPos, route, first) != null) {
				at = turn(drumPos, route, first);
			}
			CableRoutes.tell(player, CableRoutes.message("loop_not_straight", "A loop needs a straight route: the cable turns at %s", at.toShortString()));
			return InteractionResult.SUCCESS;
		}
		List<Vec3> points = returnPoints(drum, outward);
		Set<BlockPos> ignored = supports(drum, route);
		for (int i = 1; i < points.size(); i++) {
			BlockPos obstacle = CableRoutes.obstacle(serverLevel, points.get(i - 1), points.get(i), ignored);
			if (obstacle != null) {
				CableRoutes.tell(player, CableRoutes.message("loop_blocked", "The return cable would run through %s at %s",
					serverLevel.getBlockState(obstacle).getBlock().getName(), obstacle.toShortString()));
				return InteractionResult.SUCCESS;
			}
		}
		int length = returnCable(points);
		if (length > drum.stored()) {
			CableRoutes.tell(player, CableRoutes.message("loop_short", "Not enough cable for the return: it needs %s blocks, the drum has %s",
				length, drum.stored()));
			return InteractionResult.SUCCESS;
		}
		CableHook hook = CableRoutes.hook(serverLevel, drum);
		if (hook != null) {
			hook.discard();
		}
		drum.hookId = -1;
		drum.setStored(drum.stored() - length);
		drum.setLoop(true, length);
		BlockState drumState = serverLevel.getBlockState(drumPos);
		if (drumState.getValue(CableDrumBlock.FACING) != outward) {
			serverLevel.setBlock(drumPos, drumState.setValue(CableDrumBlock.FACING, outward), Block.UPDATE_ALL);
		}
		for (int i = 0; i < route.size(); i++) {
			boolean last = i == route.size() - 1;
			setReturn(serverLevel, route.get(i), outward, last ? CableAnchorBlock.Return.WHEEL : CableAnchorBlock.Return.GUIDE);
		}
		if (serverLevel.getBlockEntity(pos) instanceof CableAnchorBlockEntity terminal) {
			terminal.setWheel(true);
		}
		stack.consume(1, player);
		serverLevel.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
		CableRoutes.tell(player, CableRoutes.message("loop_closed", "Loop closed: %s blocks of return cable", length));
		return InteractionResult.SUCCESS;
	}

	/** Blocks a loop's cable may pass: the drum, the anchors and the wooden blocks they hang from. */
	private static Set<BlockPos> supports(CableDrumBlockEntity drum, List<BlockPos> route) {
		Set<BlockPos> ignored = new HashSet<>();
		ignored.add(drum.getBlockPos());
		if (drum.hanging()) {
			ignored.add(drum.getBlockPos().above());
		}
		for (BlockPos anchor : route) {
			ignored.add(anchor);
			ignored.add(anchor.above());
		}
		return ignored;
	}

	private static void setReturn(ServerLevel level, BlockPos anchor, Direction facing, CableAnchorBlock.Return value) {
		BlockState state = level.getBlockState(anchor);
		if (state.is(CableContent.CABLE_ANCHOR)) {
			level.setBlock(anchor, state.setValue(CableAnchorBlock.FACING, facing).setValue(CableAnchorBlock.RETURN, value), Block.UPDATE_ALL);
		}
	}

	/**
	 * Opens a loop: the return cable winds back onto the drum, the anchors lose their return guides and
	 * wheel, and the wheel's anchor goes to {@code player} (or drops, if null). The route stays laid out.
	 */
	static void open(ServerLevel level, CableDrumBlockEntity drum, @Nullable Player player) {
		if (!drum.looped()) {
			return;
		}
		int length = drum.returnLength();
		drum.setLoop(false, 0);
		drum.setStored(drum.stored() + length);
		List<BlockPos> route = drum.route();
		for (BlockPos anchor : route) {
			if (!level.isLoaded(anchor)) {
				continue;
			}
			BlockState state = level.getBlockState(anchor);
			if (state.is(CableContent.CABLE_ANCHOR) && state.getValue(CableAnchorBlock.RETURN) != CableAnchorBlock.Return.NONE) {
				level.setBlock(anchor, state.setValue(CableAnchorBlock.RETURN, CableAnchorBlock.Return.NONE), Block.UPDATE_ALL);
			}
			if (level.getBlockEntity(anchor) instanceof CableAnchorBlockEntity entity && entity.wheel()) {
				entity.setWheel(false);
				ItemStack wheel = new ItemStack(CableContent.CABLE_ANCHOR_ITEM);
				if (player != null) {
					player.getInventory().placeItemBackInInventory(wheel, Prediction.SERVER_ONLY);
				} else {
					Block.popResource(level, anchor, wheel);
				}
			}
		}
		level.playSound(null, drum.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
	}

	/** Whether every block the loop runs through is loaded and ticking entities (otherwise the drum will not drive it). */
	static boolean ticking(ServerLevel level, CableDrumBlockEntity drum) {
		if (!level.isPositionEntityTicking(drum.getBlockPos())) {
			return false;
		}
		for (BlockPos anchor : drum.route()) {
			if (!level.isPositionEntityTicking(anchor)) {
				return false;
			}
		}
		return true;
	}

	/** Points of the cable round the return wheel at the last anchor's roller {@code roller}: the working side first. */
	public static List<Vec3> wheelPath(Vec3 roller, Direction outward) {
		Vec3 o = new Vec3(outward.getStepX(), 0.0, outward.getStepZ());
		Vec3 a = returnOffset(outward);
		Vec3 start = roller.add(o.scale(WHEEL_OFFSET));
		Vec3 centre = start.add(a.scale(0.5));
		double radius = a.length() / 2.0;
		Vec3 u = a.normalize().scale(-1.0);
		List<Vec3> path = new ArrayList<>();
		path.add(roller);
		for (int i = 0; i <= 6; i++) {
			double angle = Math.PI * i / 6.0;
			path.add(centre.add(u.scale(radius * Math.cos(angle))).add(o.scale(radius * Math.sin(angle))));
		}
		path.add(roller.add(a));
		return path;
	}

	/** The direction a drum's straight route runs from it, or null. */
	public static @Nullable Direction outward(CableDrumBlockEntity drum) {
		return straightDirection(drum.getBlockPos(), drum.route());
	}
}
