package com.minerguy341.rackgear.content.sublevel;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.rack.RackTeeth;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;

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

	@Override
	public float speedFromCarriedRacks(Level level, BlockPos pinionPos, Axis pinionAxis) {
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

				double along = meshCentre.subtract(before.transformPosition(aboard))
					.dot(RackMeshing.unit(rackAxis));
				if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
					continue;

				float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
				if (Math.abs(speed) > Math.abs(fastest))
					fastest = speed;
			}
		}
		return fastest;
	}

	@Override
	public float speedFromWorldRacks(BlockEntity pinion, Axis axisAboard) {
		Level level = pinion.getLevel();
		if (level == null)
			return 0;

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
			double along = -travelled.dot(RackMeshing.unit(rackAxis));
			if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
				continue;

			float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
			if (Math.abs(speed) > Math.abs(fastest))
				fastest = speed;
		}
		return fastest;
	}
}
