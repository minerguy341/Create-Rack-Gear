package com.minerguy341.rackgear.content.pinion;

import java.util.List;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.rack.RackTeeth;
import com.minerguy341.rackgear.content.sublevel.RackSubLevels;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
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

	/**
	 * Ticks a locked pinion holds a contraption before its teeth give way. Long enough to notice the
	 * machine straining and cut the load, short enough that a jam resolves itself if you don't.
	 */
	private static final int STRAIN_TICKS = 60;

	private float generatedSpeed;
	private int strainTicks;

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

		if (!(getBlockState().getBlock() instanceof RackPinionBlock))
			return;
		Axis axis = getBlockState().getValue(RackPinionBlock.AXIS);

		Scan scan = scanForPassingRacks(axis);
		// A contraption we have locked is standing still, but it is still pressing its rack into our
		// teeth: keeping the load on is what keeps the network overstressed, and so keeps it locked.
		// This is gated on the jam being ours, so a contraption stalled for its own reasons — a drill
		// on bedrock, say — parks its rack against a free pinion and generates nothing.
		boolean holdLoad = scan.speed() == 0 && scan.holdsLockedRack() && isJammed();

		float speed = holdLoad ? generatedSpeed : scan.speed();
		if (speed == 0) {
			// Sable sub-levels carry their blocks as real blocks rather than as contraption data, so
			// they are searched separately: one for a ship sweeping a rack past a pinion planted in
			// the world, one for this pinion riding a ship over racks laid in the world.
			speed = RackSubLevels.INSTANCE.speedFromCarriedRacks(level, worldPosition, axis);
			if (speed == 0)
				speed = RackSubLevels.INSTANCE.speedFromWorldRacks(this, axis);
		}

		// Something keeps driving the teeth and the network will not turn: something has to give.
		if (isJammed() && speed != 0) {
			if (++strainTicks > STRAIN_TICKS) {
				stripTeeth();
				return;
			}
		} else {
			strainTicks = 0;
		}

		if (speed == generatedSpeed)
			return;

		generatedSpeed = speed;
		updateGeneratedRotation();
	}


	/**
	 * The generated speed has to reach the client: the goggle tooltip is built there, and it scales
	 * the capacity a generator provides by {@code getGeneratedSpeed() / getTheoreticalSpeed()}. Left
	 * unsynced, that ratio is zero on the client and the goggles read 0 su while the stressometer,
	 * which reports the network as the server computed it, reads the real figure.
	 */
	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putFloat("GeneratedSpeed", generatedSpeed);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		generatedSpeed = compound.getFloat("GeneratedSpeed");
	}

	/** Breaks the pinion once it has been held against a network that will not turn for too long. */
	private void stripTeeth() {
		level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 0.6f);
		level.destroyBlock(worldPosition, true);
	}

	/** Whether the network this pinion drives is currently unable to turn, so racks cannot slip past it. */
	public boolean isJammed() {
		return overStressed;
	}

	/**
	 * The strongest rotation any passing rack is currently imparting, or 0 when nothing is moving
	 * against the pinion's teeth.
	 */
	private Scan scanForPassingRacks(Axis pinionAxis) {
		List<AbstractContraptionEntity> nearby =
			level.getEntitiesOfClass(AbstractContraptionEntity.class, new AABB(worldPosition).inflate(1));

		float fastest = 0;
		boolean holdsLockedRack = false;
		for (AbstractContraptionEntity contraption : nearby) {
			Vec3 velocity = velocityOf(contraption);
			boolean stalled = contraption.getContraption() != null && contraption.getContraption().stalled;
			if (velocity.length() < RackMeshing.MOTION_EPSILON && !stalled)
				continue;

			for (Direction toRack : Direction.values()) {
				if (!RackMeshing.canMesh(pinionAxis, toRack))
					continue;

				Axis rackAxis = RackMeshing.meshedRackAxis(pinionAxis, toRack);
				double along = velocity.dot(RackMeshing.unit(rackAxis));
				if (!carriesRackAt(contraption, worldPosition.relative(toRack), rackAxis))
					continue;
				if (Math.abs(along) < RackMeshing.MOTION_EPSILON) {
					holdsLockedRack |= stalled;
					continue;
				}

				float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
				if (Math.abs(speed) > Math.abs(fastest))
					fastest = speed;
			}
		}
		return new Scan(fastest, holdsLockedRack);
	}

	/** What a sweep for passing racks found: how fast they turn us, and whether one is held against us. */
	private record Scan(float speed, boolean holdsLockedRack) {
		private static final Scan NOTHING = new Scan(0, false);
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
		if (info == null)
			return false;

		Axis barAxis = RackTeeth.barAxisOf(info.state());
		if (barAxis == null)
			return false;

		// Contraptions can be rotated, so the rack's axis is compared in world space.
		Vec3 worldAxis = entity.applyRotation(RackMeshing.unit(barAxis), 1);
		return Math.abs(worldAxis.dot(RackMeshing.unit(requiredAxis))) > 0.9;
	}
}
