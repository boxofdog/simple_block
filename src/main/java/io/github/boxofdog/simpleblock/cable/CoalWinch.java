package io.github.boxofdog.simpleblock.cable;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M06 coal-powered winch: the same cable drum, with a vanilla furnace fitted.
 *
 * <ul>
 *   <li><b>Fitting:</b> use a furnace on a drum (anywhere but its crank). It mounts on the drum's free
 *       side and the crank becomes a lever; the cable, route and loop stay as they are. Breaking the
 *       drum gives the furnace back, with its unburnt coal.</li>
 *   <li><b>Lever</b> (where the crank was; owner's controls, 2026-10-13): right-click sets PULL,
 *       left-click sets FEED, and the same click again goes back to NEUTRAL (the other click switches
 *       straight over). It stays where it is set: PULL (leaning back) hauls the cable towards the drum,
 *       FEED (leaning forward) moves it away, NEUTRAL (upright) stops and holds. Every physical stop
 *       puts it back to NEUTRAL ({@link CableDrive}); since each way has its own click, the way out of
 *       an end can always be chosen (BUG-002).</li>
 *   <li><b>Coal:</b> use coal on the drum's body to put one in (up to {@code winch.coal_capacity}).
 *       Regular coal only. With an empty hand, using the body tells you how much coal is in it, how
 *       long the coal alight has left, and where the lever is: it never moves the lever.</li>
 * </ul>
 * The coal, the burn time left and the lever are kept in the drum's line ({@link CableLines}).
 */
public final class CoalWinch {
	/** Stops caused by something in the direction of travel only (BUG-002); the opposite way stays open. */
	private static final Set<String> DIRECTIONAL_STOPS = Set.of("end", "track_end", "blocked", "unloaded", "derailed");

	private CoalWinch() {
	}

	private static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.winch." + key, fallback, args);
	}

	private static Component leverName(CableLines.Lever lever) {
		return Component.translatableWithFallback("simple_block.winch.lever." + lever.getSerializedName(),
			switch (lever) {
				case NEUTRAL -> "neutral";
				case PULL -> "pull";
				case FEED -> "feed";
			});
	}

	/** A furnace used on a drum without one: the drum becomes a coal winch. */
	static InteractionResult fitFurnace(Level level, BlockPos pos, Player player, ItemStack furnace) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && !drum.powered()) {
			CableLines.Line line = CableLines.get(serverLevel).getOrCreate(pos);
			line.powered = true;
			line.lever = CableLines.Lever.NEUTRAL;
			line.crankTicks = 0;
			line.speed = 0.0F;
			CableLines.get(serverLevel).changed();
			drum.copyDrive(line);
			// M07: the frame shows the furnace motor in place of its motor-side stand.
			BlockState state = level.getBlockState(pos);
			if (state.hasProperty(CableDrumBlock.POWERED)) {
				level.setBlock(pos, state.setValue(CableDrumBlock.POWERED, true), Block.UPDATE_ALL);
			}
			furnace.consume(1, player);
			level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
			CableRoutes.tell(player, message("fitted", "Coal winch: put coal in, then right-click the lever to pull, left-click to feed"));
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * A click on the lever: right-click asks for PULL, left-click ({@link CableDrive#feed}) for FEED;
	 * asking for the setting it is already at puts it back to NEUTRAL.
	 */
	static InteractionResult setLever(Level level, BlockPos pos, Player player, CableLines.Lever wanted) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered()) {
			setLever(serverLevel, pos, CableLines.get(serverLevel).getOrCreate(pos), pos, player, wanted, false);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * The same click on the line of the winch at {@code home}, made at {@code at}: the winch itself, or
	 * (M07 playtest change 2, {@code far}) its loop's far drum, whose lever {@code wanted} is already turned
	 * into the winch's own terms. One line, one lever: the last click from either end wins. The winch need
	 * not be loaded.
	 */
	static void setLever(ServerLevel level, BlockPos home, CableLines.Line line, BlockPos at, Player player, CableLines.Lever wanted, boolean far) {
		CableLines.Lever next = line.lever == wanted ? CableLines.Lever.NEUTRAL : wanted;
		if (next != CableLines.Lever.NEUTRAL && !line.drivable()) {
			CableRoutes.tell(player, CableRoutes.message("crank_locked", "Close the loop, or hang a skip on the cable end, to haul"));
			level.playSound(null, at, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 0.6F);
			return;
		}
		line.lever = next;
		line.idleTicks = 0;
		line.stallTicks = 0;
		if (next == CableLines.Lever.NEUTRAL) {
			line.speed = 0.0F;
		}
		CableLines.get(level).changed();
		if (level.isLoaded(home) && level.getBlockEntity(home) instanceof CableDrumBlockEntity drum) {
			drum.copyDrive(line);
		}
		FarDrums.show(level, line);
		level.playSound(null, at, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, next == CableLines.Lever.NEUTRAL ? 0.5F : 0.6F);
		CableRoutes.tell(player, message("lever", "Lever: %s", leverName(far ? next.mirrored() : next)));
	}

	/** Coal used on a coal winch's body: one goes in. */
	static InteractionResult addCoal(Level level, BlockPos pos, Player player, ItemStack coal) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered()) {
			CableLines lines = CableLines.get(serverLevel);
			CableLines.Line line = lines.getOrCreate(pos);
			int capacity = Tuning.getInt("winch.coal_capacity");
			if (line.coal >= capacity) {
				CableRoutes.tell(player, message("full", "The winch holds %s coal", capacity));
			} else {
				line.coal++;
				coal.consume(1, player);
				lines.changed();
				drum.copyDrive(line);
				level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F);
				CableRoutes.tell(player, message("coal", "Coal: %s", line.coal));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty-hand use of a coal winch's body: how much coal it has (never moves the lever). */
	static InteractionResult inspect(Level level, BlockPos pos, Player player) {
		if (level instanceof ServerLevel serverLevel) {
			CableLines.Line line = CableLines.line(serverLevel, pos);
			if (line != null) {
				CableRoutes.tell(player, message("status", "Coal winch: %s coal, %s s alight, lever %s", line.coal, (line.burn + 19) / 20,
					leverName(line.lever)));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Whether {@code stack} is fuel for a coal winch (regular coal only). */
	static boolean isCoal(ItemStack stack) {
		return stack.is(Items.COAL);
	}

	/** A physical stop: the lever goes back to NEUTRAL and the drive stops; nothing restarts it but a player. */
	static void toNeutral(ServerLevel level, BlockPos pos, CableLines.Line line, String reason) {
		// A stop against something in the direction of travel only: the lever's next click skips that way.
		if (DIRECTIONAL_STOPS.contains(reason)) {
			line.blockedSign = line.lever.sign;
			line.blockedCarts = Set.copyOf(line.lastAlongs.keySet());
		}
		line.lever = CableLines.Lever.NEUTRAL;
		line.speed = 0.0F;
		line.idleTicks = 0;
		line.stallTicks = 0;
		line.lastStop = reason;
		// M07 playtest change 2: a second drum's lever goes back with it.
		FarDrums.show(level, line);
		if (level.isLoaded(pos)) {
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.5F);
		}
	}
}
