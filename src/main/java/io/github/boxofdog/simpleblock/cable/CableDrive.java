package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.TrainPhysics;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M05 hand-cranked cable haulage.
 *
 * <p><b>The crank.</b> Every drum has one, on its axle end. Hold the use key on the crank to turn it
 * (vanilla repeats a held use every few ticks; the crank keeps turning {@value #CRANK_HOLD} ticks after
 * each); sneak while doing so (empty hands) to turn it the other way. Uses anywhere else on the drum do
 * what they did in M04, so cranking never loads a coil or dismantles anything. An empty drum, or one
 * with all its cable wound on, just spins; a drum with an open route is locked (only a closed loop
 * moves cable).
 *
 * <p><b>The loop.</b> On a closed loop ({@link CableLoops}) the drum is a capstan: cranking moves the
 * working strand at {@code cable.crank_speed} blocks a tick, forwards (away from the drum) or back,
 * and the cable never piles up on the spool. Each cart whose rod grips the working strand (only that
 * strand) moves with it, with the carts coupled to it; when the crank stops they are held where they
 * are. A cart stops short of either end: within {@code cable.end_clearance} blocks of the drum's exit
 * or the return wheel along the track, or where the cable would leave its rod's reach (a climb the
 * cable does not follow, a curve, a low cable near the drum), and stays held there until the cable
 * runs the other way. Hauling uphill is slower the heavier the train: the speed is divided by
 * {@code 1 + cable.grade_load x} the mass of the gripping trains on a climbing rail (masses in
 * {@link TrainPhysics}); lowering downhill is not faster. A loop with any part in chunks that do not
 * tick does not move.
 */
public final class CableDrive {
	/** Ticks a crank keeps turning after one use: a held use key repeats every 4 ticks. */
	static final int CRANK_HOLD = 6;

	private CableDrive() {
	}

	public static double crankSpeed() {
		return Tuning.getDouble("cable.crank_speed");
	}

	/** A use on the crank: keeps it turning, backwards if sneaking. Client and server both swing the arm. */
	static InteractionResult crank(Level level, BlockPos pos, Player player) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum) {
			boolean starting = drum.crankTicks <= 0;
			drum.crankTicks = CRANK_HOLD;
			drum.crankDirection = player.isSecondaryUseActive() ? -1 : 1;
			if (starting && !drum.looped() && !drum.route().isEmpty()) {
				serverLevel.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 0.6F);
				CableRoutes.tell(player, CableRoutes.message("crank_locked", "Close the loop to haul"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Every tick: turns the drum from the crank, and drives a loop. */
	static void tickDrum(ServerLevel level, CableDrumBlockEntity drum) {
		int input = drum.crankTicks > 0 ? drum.crankDirection : 0;
		if (drum.crankTicks > 0) {
			drum.crankTicks--;
		}
		double speed = 0.0;
		if (drum.looped()) {
			if (input != 0 && CableLoops.ticking(level, drum)) {
				double load = input > 0 ? drum.lastLoadForward : drum.lastLoadBackward;
				speed = input * crankSpeed() / (1.0 + load);
			}
		} else if (drum.route().isEmpty()) {
			speed = input * crankSpeed();
		}
		drum.lastLoadForward = drum.loadForward;
		drum.lastLoadBackward = drum.loadBackward;
		drum.loadForward = 0.0;
		drum.loadBackward = 0.0;
		drum.setSpeed((float) speed);
		if (speed != 0.0 && level.getGameTime() % 5 == 0) {
			level.playSound(null, drum.getBlockPos(), SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.4F);
		}
	}

	/**
	 * A cart whose rod grips a working strand (server, every tick): if the strand belongs to a loop, the
	 * cart (and its train) is moved with the cable or held. Returns whether it is under a loop's control.
	 */
	static boolean driveCart(ServerLevel level, Minecart cart, CableIndex.Grip grip) {
		if (!level.isLoaded(grip.drum()) || !(level.getBlockEntity(grip.drum()) instanceof CableDrumBlockEntity drum) || !drum.looped()
			|| grip.direction().lengthSqr() < 1.0E-6) {
			return false;
		}
		Vec3 outward = grip.direction();
		double mass = Couplings.consistMass(cart);
		double gradeLoad = Tuning.getDouble("cable.grade_load") * mass;
		int grade = TrainPhysics.gradeSign(cart, outward);
		if (grade > 0) {
			drum.loadForward += gradeLoad;
		} else if (grade < 0) {
			drum.loadBackward += gradeLoad;
		}
		double speed = drum.speed();
		if (speed == 0.0 || !canMove(level, cart, grip, speed)) {
			Couplings.command(cart, null);
		} else {
			Couplings.command(cart, outward.scale(speed));
		}
		return true;
	}

	/** Whether moving {@code speed} along the strand keeps the cart clear of both ends and in its rod's reach. */
	private static boolean canMove(ServerLevel level, Minecart cart, CableIndex.Grip grip, double speed) {
		double clearance = Tuning.getDouble("cable.end_clearance");
		double next = grip.along() + speed;
		// Never further into an end zone; a cart already in one may always move out of it.
		if (next < clearance && speed < 0.0 || next > grip.length() - clearance && speed > 0.0) {
			return false;
		}
		Vec3 ahead = cart.position().add(grip.direction().scale(speed + Math.signum(speed) * 0.5));
		CableIndex.Grip still = CableIndex.grip(level, ahead, ConnectingRods.TOLERANCE, ConnectingRods.reachMin(), ConnectingRods.reachMax());
		return still != null && still.drum().equals(grip.drum());
	}
}
