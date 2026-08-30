package com.minerguy341.rackgear.content;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Geometry and speed conversions shared by the rack and the pinion. */
public final class RackMeshing {

	/**
	 * Create's linear actuators move at {@code speed / 512} blocks per tick, see
	 * {@code KineticBlockEntity#convertToLinear}. Using the inverse here means a rack pushed by a
	 * piston running at n RPM turns a pinion at n RPM.
	 */
	public static final float BLOCKS_PER_TICK_TO_RPM = 512;

	/** Create's default maximum rotation speed; faster networks stop working, so generation is capped. */
	public static final float MAX_RPM = 256;

	/** Movement slower than this (in blocks per tick) counts as standing still. */
	public static final double MOTION_EPSILON = 1 / 4096d;

	private RackMeshing() {
	}

	public static Vec3 unit(Axis axis) {
		return new Vec3(axis == Axis.X ? 1 : 0, axis == Axis.Y ? 1 : 0, axis == Axis.Z ? 1 : 0);
	}

	/** The axis that is neither of the two given ones, which must differ. */
	public static Axis thirdAxis(Axis first, Axis second) {
		for (Axis axis : Axis.values())
			if (axis != first && axis != second)
				return axis;
		throw new IllegalArgumentException("No third axis for " + first + " and " + second);
	}

	/**
	 * The axis a direction lies along, or null when it points somewhere between two of them — which a
	 * freely rotating carrier can do, and where teeth would not line up anyway.
	 */
	@Nullable
	public static Axis nearestAxis(Vec3 direction) {
		for (Axis axis : Axis.values())
			if (Math.abs(direction.dot(unit(axis))) > 0.9)
				return axis;
		return null;
	}

	/** Teeth engage on the four sides around a pinion, never on its two shaft faces. */
	public static boolean canMesh(Axis pinionAxis, Direction toRack) {
		return toRack.getAxis() != pinionAxis;
	}

	/** The axis a rack must run along to mesh with a pinion from the given side. */
	public static Axis meshedRackAxis(Axis pinionAxis, Direction toRack) {
		return thirdAxis(pinionAxis, toRack.getAxis());
	}

	/**
	 * Which way a rack turns the pinion it passes: {@code +1} when movement along the positive
	 * direction of the rack's axis drives a positive rotation, {@code -1} when it drives a negative one.
	 *
	 * <p>Rolling contact means the pinion's surface velocity where the teeth meet equals the rack's
	 * velocity, {@code omega * r * (A x d) = v * R}, leaving {@code R . (A x d)} as the sign — with A
	 * the pinion's rotation axis, d pointing from the pinion to the rack, and R the rack's axis. That
	 * is the same right-handed convention Create renders positive speed with, see
	 * {@code KineticBlockEntityRenderer#kineticRotationTransform}.
	 */
	public static double meshDirection(Axis pinionAxis, Direction toRack) {
		Vec3 contact = unit(pinionAxis).cross(Vec3.atLowerCornerOf(toRack.getNormal()));
		return contact.dot(unit(meshedRackAxis(pinionAxis, toRack)));
	}

	/**
	 * Turns the rack's velocity along its own axis into a rotation speed for the pinion, rounded to
	 * whole RPM so that minute changes in contraption speed don't churn the kinetic network.
	 */
	public static float toRotationSpeed(double blocksPerTick, double meshDirection) {
		double rpm = blocksPerTick * meshDirection * BLOCKS_PER_TICK_TO_RPM;
		return (float) Mth.clamp(Math.round(rpm), -MAX_RPM, MAX_RPM);
	}
}
