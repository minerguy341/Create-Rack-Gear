package com.minerguy341.rackgear.content.sublevel;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.rack.RackTeeth;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Sable half of {@link RackSubLevels}. Loaded only when Sable is present.
 *
 * <p>A sub-level's blocks are real blocks standing elsewhere in the same level; its pose maps them to
 * where they appear. So meshing across the boundary is a matter of moving one position through that
 * pose, and the pose from the previous tick gives how fast the spot is sweeping — rotation included,
 * which is why this path handles a rolling, pitching ship that the contraption path cannot.
 */
public class SableRackSubLevels implements RackSubLevels {

	/** How far around a pinion to look for sub-levels; teeth only ever mesh one block away. */
	private static final double SEARCH_RADIUS = 2;

	/**
	 * How much of the momentum along the teeth a jammed pinion takes out of a sub-level each tick.
	 * Well short of a dead stop, so a heavy ship is dragged down over a moment rather than being
	 * snapped to a halt, and so the impulse cannot overshoot into a bounce.
	 */
	private static final double BRAKE_STRENGTH = 0.5;

	@Override
	public float meshWithSubLevels(BlockEntity pinion, Axis pinionAxis, boolean jammed) {
		Level level = pinion.getLevel();
		if (level == null)
			return 0;

		float carried = fromCarriedRacks(level, pinion.getBlockPos(), pinionAxis, jammed);
		return carried != 0 ? carried : fromWorldRacks(level, pinion, pinionAxis, jammed);
	}

	/** A pinion planted in the world, with a sub-level sweeping one of its racks past the teeth. */
	private static float fromCarriedRacks(Level level, BlockPos pinionPos, Axis pinionAxis, boolean jammed) {
		BoundingBox3d range = new BoundingBox3d(new AABB(pinionPos).inflate(SEARCH_RADIUS));

		float fastest = 0;
		for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, range)) {
			Pose3dc pose = subLevel.logicalPose();
			Pose3dc before = subLevel.lastPose();

			for (Direction toRack : Direction.values()) {
				if (!RackMeshing.canMesh(pinionAxis, toRack))
					continue;

				Axis rackAxis = RackMeshing.meshedRackAxis(pinionAxis, toRack);
				Vec3 meshCentre = Vec3.atCenterOf(pinionPos.relative(toRack));
				Vec3 aboard = pose.transformPositionInverse(meshCentre);

				BlockState rack = level.getBlockState(BlockPos.containing(aboard));
				Axis barAxis = RackTeeth.barAxisOf(rack);
				if (barAxis == null)
					continue;
				if (RackMeshing.nearestAxis(pose.transformNormal(RackMeshing.unit(barAxis))) != rackAxis)
					continue;

				// Where that spot was a tick ago gives how fast it is sweeping past, rotation included.
				double along = meshCentre.subtract(before.transformPosition(aboard))
					.dot(RackMeshing.unit(rackAxis));
				if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
					continue;

				// The rack is the part riding, so its motion along the teeth is the carrier's own.
				if (jammed)
					brake(subLevel, meshCentre, RackMeshing.unit(rackAxis), along);

				float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
				if (Math.abs(speed) > Math.abs(fastest))
					fastest = speed;
			}
		}
		return fastest;
	}

	/** A pinion riding a sub-level, rolling along racks laid in the world. */
	private static float fromWorldRacks(Level level, BlockEntity pinion, Axis axisAboard, boolean jammed) {
		SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(pinion);
		if (subLevel == null)
			return 0;

		Pose3dc pose = subLevel.logicalPose();
		Vec3 aboard = Vec3.atCenterOf(pinion.getBlockPos());
		Vec3 here = pose.transformPosition(aboard);
		Vec3 travelled = here.subtract(subLevel.lastPose()
			.transformPosition(aboard));
		if (travelled.length() < RackMeshing.MOTION_EPSILON)
			return 0;

		Axis pinionAxis = RackMeshing.nearestAxis(pose.transformNormal(RackMeshing.unit(axisAboard)));
		if (pinionAxis == null)
			return 0;

		BlockPos worldPos = BlockPos.containing(here);
		float fastest = 0;
		for (Direction toRack : Direction.values()) {
			if (!RackMeshing.canMesh(pinionAxis, toRack))
				continue;

			Axis rackAxis = RackMeshing.meshedRackAxis(pinionAxis, toRack);
			if (!RackTeeth.runsAlong(level.getBlockState(worldPos.relative(toRack)), rackAxis))
				continue;

			// The rack is the one standing still, so it runs backwards past the pinion riding by.
			double carrierAlong = travelled.dot(RackMeshing.unit(rackAxis));
			double along = -carrierAlong;
			if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
				continue;

			if (jammed)
				brake(subLevel, here, RackMeshing.unit(rackAxis), carrierAlong);

			float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
			if (Math.abs(speed) > Math.abs(fastest))
				fastest = speed;
		}
		return fastest;
	}

	/**
	 * Holds a sub-level back where the teeth meet, the physical stand-in for stalling a contraption.
	 * The impulse lands at the meshing point rather than at the centre of mass, so a ship caught by
	 * one corner is turned as well as slowed, the way a gear biting at one point really would.
	 *
	 * <p>Total mass understates the mass that resists an impulse off the centre of mass, so this
	 * brakes a little softer than asked and closes the gap over the following ticks.
	 */
	private static void brake(SubLevelAccess subLevel, Vec3 point, Vec3 axis, double carrierSpeedAlongAxis) {
		if (!(subLevel instanceof ServerSubLevel ship))
			return;

		MassData mass = ship.getMassTracker();
		if (mass == null || mass.isInvalid())
			return;

		double impulse = -carrierSpeedAlongAxis * mass.getMass() * BRAKE_STRENGTH;
		RigidBodyHandle body = RigidBodyHandle.of(ship);
		if (body.isValid())
			body.applyImpulseAtPoint(point, axis.scale(impulse));
	}
}
