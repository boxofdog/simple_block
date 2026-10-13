package io.github.boxofdog.simpleblock.train;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M05 rail physics shared by cable haulage and the Cobblestone Engine: how heavy a cart is and which
 * way the rail under it climbs. Masses are in empty-minecart units (tuning {@code rail.*},
 * {@code train.engine_mass}): an empty minecart is already heavy, cargo makes it heavier (a full
 * load of 256 adds {@code rail.cargo_mass}), and riders add a little. Nothing here depends on what the
 * cargo is or whether a cart carries passengers.
 */
public final class TrainPhysics {
	private TrainPhysics() {
	}

	public static double mass(AbstractMinecart cart) {
		double mass = cart instanceof MinecartFurnace ? Tuning.getDouble("train.engine_mass") : Tuning.getDouble("rail.cart_mass");
		if (cart instanceof Minecart minecart) {
			mass += Tuning.getDouble("rail.cargo_mass") * MinecartCargo.cart(minecart).simpleBlock$count() / MinecartCargo.CAPACITY;
		}
		for (Entity passenger : cart.getPassengers()) {
			mass += Tuning.getDouble("rail.passenger_mass");
		}
		return mass;
	}

	private static @Nullable RailShape railShape(AbstractMinecart cart) {
		BlockPos pos = cart.getCurrentBlockPosOrRailBelow();
		BlockState state = cart.level().getBlockState(pos);
		return state.getBlock() instanceof BaseRailBlock rail ? state.getValue(rail.getShapeProperty()) : null;
	}

	/** Whether the cart is on a rail (not derailed). */
	public static boolean onRail(AbstractMinecart cart) {
		return railShape(cart) != null;
	}

	/** +1 if the rail under the cart climbs in {@code direction} (horizontal), -1 if it falls, 0 if level or no rail. */
	public static int gradeSign(AbstractMinecart cart, Vec3 direction) {
		RailShape shape = railShape(cart);
		if (shape == null || !shape.isSlope()) {
			return 0;
		}
		Vec3 uphill = switch (shape) {
			case ASCENDING_EAST -> new Vec3(1, 0, 0);
			case ASCENDING_WEST -> new Vec3(-1, 0, 0);
			case ASCENDING_SOUTH -> new Vec3(0, 0, 1);
			default -> new Vec3(0, 0, -1);
		};
		double along = uphill.dot(direction);
		return along > 1.0E-3 ? 1 : along < -1.0E-3 ? -1 : 0;
	}

	/** The horizontal axis of a straight (or sloping) rail under the cart, or null on a curve or off the rails. */
	public static @Nullable Vec3 railAxis(AbstractMinecart cart) {
		RailShape shape = railShape(cart);
		if (shape == null) {
			return null;
		}
		return switch (shape) {
			case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Vec3(0, 0, 1);
			case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Vec3(1, 0, 0);
			default -> null;
		};
	}
}
