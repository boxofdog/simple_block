package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * M07 playtest change 2 (owner's decisions, 2026-10-14): a cable line worked from both ends. Both kinds
 * of line are kept: one drum and a closed loop round a return wheel ({@link CableLoops}), worked from the
 * drum; or two drums and one cable between them, worked from either.
 *
 * <ul>
 *   <li><b>Two drums.</b> Holding the end of a drum's cable (routed through its anchors, or not), use a
 *       second, empty drum (no cable, no furnace): the end is made fast to it. The route must be
 *       straight, drum to drum, like a loop's, and the last span at most {@code cable.max_span} blocks and
 *       clear. There is no return strand or wheel: driving pays the cable off one drum and winds it onto
 *       the other, so a gripping cart runs between them. The first drum keeps the one coil (the second
 *       needs none) and the drive; the second shows cable on its barrel and turns with it.</li>
 *   <li><b>Controls, relative to each end.</b> Hand-cranked, both drums have the same crank: held
 *       right-click pulls the cart towards that drum, held left-click sends it away. With a coal winch, the
 *       second drum has its lever, without the motor: right-click PULL (towards it), left-click FEED
 *       (away), the same click again NEUTRAL. Both ends drive one line: the last command from either end
 *       wins, NEUTRAL at either end stops it, and the two levers always show the same setting from their
 *       own ends. There is only ever one motor, so nothing can fight.</li>
 *   <li><b>Coal stays at home.</b> Only the coal winch (the first drum) takes and burns coal, whichever
 *       end commands.</li>
 *   <li><b>Everything else is the loop's</b>: a cart stops short of either drum (its end clearance) and
 *       otherwise as on a loop; Loading Rail docking and stops, the BUG-002 rule and the physical stops
 *       that put the lever back to NEUTRAL.</li>
 *   <li><b>Unhooking.</b> Sneak-use either drum (or an anchor of the route) with an empty hand: the end
 *       comes off the second drum and the line is an open route again, its end back at the last support.
 *       Breaking either drum or an anchor does the same.</li>
 * </ul>
 */
public final class FarDrums {
	private FarDrums() {
	}

	static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.far_drum." + key, fallback, args);
	}

	/**
	 * The held end of {@code home}'s cable used on {@code far}: made fast to it, if it can be. Returns what
	 * the player is told.
	 */
	static Component attach(ServerLevel level, CableHook held, CableDrumBlockEntity far) {
		BlockPos homePos = held.drumPos();
		if (!level.isLoaded(homePos) || !(level.getBlockEntity(homePos) instanceof CableDrumBlockEntity home)) {
			return CableRoutes.status(far);
		}
		if (home.looped()) {
			return CableRoutes.message("loop_already", "This route is already a loop");
		}
		if (CableHoists.hoisting(level, homePos)) {
			return message("hoisting", "A skip hangs on this cable: take it off first");
		}
		if (far.hasCoil() || far.powered() || !far.route().isEmpty() || far.farFrom() != null || far.endDrum() != null) {
			return message("not_empty", "The second drum must be empty: no cable and no furnace");
		}
		BlockPos farPos = far.getBlockPos();
		List<BlockPos> route = new ArrayList<>(home.route());
		route.add(farPos);
		Direction outward = CableLoops.straightDirection(homePos, route);
		if (outward == null) {
			return message("not_straight", "Drum to drum, the cable must run straight");
		}
		Vec3 from = home.points().getLast();
		Vec3 to = CableRoutes.drumPoint(farPos, far.hanging());
		double span = from.distanceTo(to);
		if (span > CableRoutes.maxSpan()) {
			return CableRoutes.message("too_far", "That span would be %s blocks: at most %s", (int) Math.ceil(span), CableRoutes.maxSpan());
		}
		BlockPos last = home.routeEnd();
		Set<BlockPos> ignored = new HashSet<>(List.of(last, last.above(), farPos));
		if (far.hanging()) {
			ignored.add(farPos.above());
		}
		BlockPos obstacle = CableRoutes.obstacle(level, from, to, ignored);
		if (obstacle != null) {
			return CableRoutes.message("blocked", "The cable would run through %s at %s", level.getBlockState(obstacle).getBlock().getName(),
				obstacle.toShortString());
		}
		face(level, homePos, outward);
		face(level, farPos, outward.getOpposite());
		held.discard();
		home.hookId = -1;
		home.setEndDrum(farPos, far.hanging());
		far.setFarFrom(homePos);
		CableLines.Line line = CableLines.line(level, homePos);
		if (line != null) {
			far.copyFar(line);
		}
		level.playSound(null, farPos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
		return line != null && line.powered
			? message("attached_winch", "Cable made fast to this drum: work the coal winch from either end (right-click pulls the cart towards you)")
			: message("attached", "Cable made fast to this drum: crank from either end (right-click pulls the cart towards you)");
	}

	private static void face(ServerLevel level, BlockPos pos, Direction facing) {
		BlockState state = level.getBlockState(pos);
		if (state.hasProperty(CableDrumBlock.FACING) && state.getValue(CableDrumBlock.FACING) != facing) {
			level.setBlock(pos, state.setValue(CableDrumBlock.FACING, facing), Block.UPDATE_ALL);
		}
	}

	/** The line a second drum holds the end of, or null if it no longer does. */
	private static CableLines.@Nullable Line homeLine(ServerLevel level, CableDrumBlockEntity far) {
		BlockPos home = far.farFrom();
		if (home == null) {
			return null;
		}
		CableLines.Line line = CableLines.line(level, home);
		return line != null && line.looped && far.getBlockPos().equals(line.far) ? line : null;
	}

	/** Every tick of a second drum: still holding the end? Then it shows the line's drive from its end. */
	static void tick(ServerLevel level, CableDrumBlockEntity drum) {
		if (drum.farFrom() == null) {
			return;
		}
		CableLines.Line line = homeLine(level, drum);
		if (line == null) {
			drum.setFarFrom(null);
		} else {
			drum.copyFar(line);
		}
	}

	/** The line's drive changed: its second drum, where loaded, shows it at once. */
	static void show(ServerLevel level, CableLines.Line line) {
		BlockPos far = line.far;
		if (far != null && level.isLoaded(far) && level.getBlockEntity(far) instanceof CableDrumBlockEntity drum && drum.farFrom() != null) {
			drum.copyFar(line);
		}
	}

	/**
	 * A click on a second drum's crank or lever: {@code towardsHere} (right-click) pulls the cart towards
	 * it, otherwise (left-click) sends it away. Turned into the first drum's terms (away from it is towards
	 * the second) and given to the one line, which need not have its first drum loaded.
	 */
	static InteractionResult work(ServerLevel level, CableDrumBlockEntity far, Player player, boolean towardsHere) {
		CableLines.Line line = homeLine(level, far);
		BlockPos home = far.farFrom();
		if (line == null || home == null) {
			return InteractionResult.SUCCESS;
		}
		if (line.powered) {
			CoalWinch.setLever(level, home, line, far.getBlockPos(), player, towardsHere ? CableLines.Lever.FEED : CableLines.Lever.PULL, true);
		} else {
			CableDrive.crank(level, line, far.getBlockPos(), player, towardsHere ? 1 : -1, true);
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty-hand use of a second drum's body: what it is, and (with a coal winch) its lever and the coal at the winch. */
	static InteractionResult inspect(ServerLevel level, CableDrumBlockEntity far, Player player) {
		CableLines.Line line = homeLine(level, far);
		BlockPos home = far.farFrom();
		if (line != null && home != null) {
			CableLines.Lever shown = line.lever.mirrored();
			CableRoutes.tell(player, line.powered
				? message("status_winch", "Second drum of the coal winch at %s: lever %s; coal at the winch: %s", home.toShortString(),
					Component.translatableWithFallback("simple_block.winch.lever." + shown.getSerializedName(), shown.getSerializedName()), line.coal)
				: message("status", "Second drum of the cable from %s: crank from either end", home.toShortString()));
		}
		return InteractionResult.SUCCESS;
	}

	/** Sneak-use of a second drum's body: the cable's end comes off it. */
	static InteractionResult unhook(ServerLevel level, CableDrumBlockEntity far, Player player) {
		BlockPos home = far.farFrom();
		if (home != null) {
			release(level, home, far.getBlockPos());
			far.setFarFrom(null);
			level.playSound(null, far.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
			CableRoutes.tell(player, CableLoops.openedMessage(true));
		}
		return InteractionResult.SUCCESS;
	}

	/** Things a second drum refuses: coal (it burns at the winch), a furnace, a cable coil. */
	static InteractionResult refuse(ServerLevel level, CableDrumBlockEntity far, Player player, String what) {
		BlockPos home = far.farFrom();
		String at = home == null ? "?" : home.toShortString();
		CableRoutes.tell(player, switch (what) {
			case "coal" -> message("coal_at_home", "Coal goes in the coal winch at %s", at);
			case "furnace" -> message("no_furnace", "This drum holds the end of the cable from %s: it needs no furnace", at);
			default -> message("no_coil", "This drum holds the end of the cable from %s: it needs no coil", at);
		});
		level.playSound(null, far.getBlockPos(), SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 0.6F);
		return InteractionResult.SUCCESS;
	}

	/** A second drum is going (broken): the cable's end comes off it. */
	static void removed(ServerLevel level, CableDrumBlockEntity drum) {
		BlockPos home = drum.farFrom();
		if (home != null) {
			release(level, home, drum.getBlockPos());
		}
	}

	/**
	 * The end of {@code home}'s cable comes off the second drum at {@code far}: at once if the first drum
	 * is loaded; otherwise its line is opened now and the drum follows when it loads ({@link CableLines#link}).
	 */
	private static void release(ServerLevel level, BlockPos home, BlockPos far) {
		if (level.isLoaded(home) && level.getBlockEntity(home) instanceof CableDrumBlockEntity drum && far.equals(drum.endDrum())) {
			CableLoops.open(level, drum, null);
			return;
		}
		CableLines.Line line = CableLines.line(level, home);
		if (line != null && far.equals(line.far)) {
			line.looped = false;
			line.far = null;
			line.outward = null;
			line.speed = 0.0F;
			line.lever = CableLines.Lever.NEUTRAL;
			if (!line.points.isEmpty()) {
				line.points = List.copyOf(line.points.subList(0, line.points.size() - 1));
			}
			CableLines.get(level).changed();
			CableIndex.update(level, home, line);
		}
	}
}
