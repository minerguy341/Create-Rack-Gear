package com.minerguy341.rackgear.content.pinion;

import java.util.List;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.rack.RackBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Generates rotation from racks that are dragged past the pinion by a contraption.
 *
 * <p>Blocks on a moving contraption are not in the world, so the pinion cannot see them as
 * neighbours: instead it looks for contraption entities around itself each tick and asks them what
 * block they carry at the position where teeth would mesh. The contraption's own travel over that
 * tick then gives the speed, which is why the pinion turns faster or slower exactly as the
 * contraption pushing the rack does, and reverses when the contraption reverses.
 */
public class RackPinionBlockEntity extends GeneratingKineticBlockEntity {

	private float generatedSpeed;

	public RackPinionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public float getGeneratedSpeed() {
		return generatedSpeed;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		float speed = scanForPassingRacks();
		if (speed == generatedSpeed)
			return;

		generatedSpeed = speed;
		updateGeneratedRotation();
	}

	/**
	 * The strongest rotation any passing rack is currently imparting, or 0 when nothing is moving
	 * against the pinion's teeth.
	 */
	private float scanForPassingRacks() {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof RackPinionBlock))
			return 0;

		Axis pinionAxis = state.getValue(RackPinionBlock.AXIS);
		List<AbstractContraptionEntity> nearby =
			level.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(worldPosition).inflate(1));

		float fastest = 0;
		for (AbstractContraptionEntity contraption : nearby) {
			Vec3 velocity = velocityOf(contraption);
			if (velocity.length() < RackMeshing.MOTION_EPSILON)
				continue;

			for (Direction toRack : Direction.values()) {
				if (!RackMeshing.canMesh(pinionAxis, toRack))
					continue;

				Axis rackAxis = RackMeshing.meshedRackAxis(pinionAxis, toRack);
				double along = velocity.dot(RackMeshing.unit(rackAxis));
				if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
					continue;
				if (!carriesRackAt(contraption, worldPosition.relative(toRack), rackAxis))
					continue;

				float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
				if (Math.abs(speed) > Math.abs(fastest))
					fastest = speed;
			}
		}
		return fastest;
	}

	/**
	 * How far the contraption travelled this tick. Contraption types are moved in different ways, so
	 * the position delta is preferred over the entity's motion vector, which not all of them keep up
	 * to date.
	 */
	private static Vec3 velocityOf(AbstractContraptionEntity contraption) {
		Vec3 travelled = contraption.position()
			.subtract(contraption.xo, contraption.yo, contraption.zo);
		return travelled.length() < RackMeshing.MOTION_EPSILON ? contraption.getDeltaMovement() : travelled;
	}

	/** Whether the contraption carries a rack of the given world-space axis at the meshing position. */
	private static boolean carriesRackAt(AbstractContraptionEntity entity, BlockPos meshPos, Axis requiredAxis) {
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return false;

		BlockPos localPos = BlockPos.containing(entity.toLocalVector(Vec3.atCenterOf(meshPos), 1));
		StructureBlockInfo info = contraption.getBlocks()
			.get(localPos);
		if (info == null || !(info.state()
			.getBlock() instanceof RackBlock))
			return false;

		// Contraptions can be rotated, so the rack's axis is compared in world space.
		Vec3 worldAxis = entity.applyRotation(RackMeshing.unit(info.state()
			.getValue(RackBlock.AXIS)), 1);
		return Math.abs(worldAxis.dot(RackMeshing.unit(requiredAxis))) > 0.9;
	}
}
