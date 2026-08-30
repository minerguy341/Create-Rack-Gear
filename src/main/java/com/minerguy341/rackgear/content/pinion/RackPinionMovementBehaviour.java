package com.minerguy341.rackgear.content.pinion;

import org.jetbrains.annotations.Nullable;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.rack.DrivenRackBlock;
import com.minerguy341.rackgear.content.rack.DrivenRackBlockEntity;
import com.minerguy341.rackgear.content.rack.RackTeeth;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Rolls a pinion riding a contraption along racks placed in the world — the mirror image of
 * {@link RackPinionBlockEntity}, which watches racks being carried past a pinion standing in the
 * world.
 *
 * <p>As a contraption actor the pinion is ticked with its world position, motion and rotation on
 * both sides, so the rolling speed comes straight from the contraption's motion. The rotation cannot
 * stay on the contraption — Create gives contraptions no kinetic network — so it is handed to the
 * {@link DrivenRackBlockEntity} being rolled over, which is in the world and can drive a network.
 */
public class RackPinionMovementBehaviour implements MovementBehaviour {

	/** Rolling speed in RPM, kept in the actor's data so the renderers can read it. */
	public static final String SPEED_KEY = "RollingSpeed";

	/** Accumulated rotation in degrees, so the cog keeps its phase from tick to tick. */
	public static final String ANGLE_KEY = "Angle";

	/** Position of the driven rack currently being powered, so it can be released on the way out. */
	private static final String DRIVEN_RACK_KEY = "DrivenRack";

	/** The rack and speed held while a jam has the pinion locked, so the load stays on it. */
	private static final String HELD_RACK_KEY = "HeldRack";
	private static final String HELD_SPEED_KEY = "HeldSpeed";

	/** Create animates a kinetic block at {@code speed * 3/10} degrees per tick. */
	private static final float DEGREES_PER_TICK_PER_RPM = 3 / 10f;

	/** The actor draws the cog itself, so the block entity copy in the contraption must stay hidden. */
	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}

	@Override
	public void tick(MovementContext context) {
		Mesh mesh = findMesh(context);
		float speed = mesh == null ? 0 : mesh.speed();

		// A locked pinion is not turning, and a locked contraption is not moving, so the animation
		// stops on both sides without any special handling.
		context.data.putFloat(SPEED_KEY, speed);
		context.data.putFloat(ANGLE_KEY,
			(context.data.getFloat(ANGLE_KEY) + speed * DEGREES_PER_TICK_PER_RPM) % 360);

		Level level = context.world;
		if (level == null || level.isClientSide)
			return;

		// Teeth that cannot turn the rack they are pressed into do not slip past it: a jammed rack
		// locks the contraption. The load is held on while locked, because letting it go would
		// un-jam the network, clear the stall, and start the whole thing chattering once a tick.
		Mesh candidate = mesh != null ? mesh : heldMesh(context);
		boolean locked = candidate != null && isJammed(level, candidate.rackPos());
		context.stall = locked;
		setHeld(context, locked ? candidate : null);

		updateDrivenRack(context, level, locked ? candidate : mesh);
	}

	@Override
	public void stopMoving(MovementContext context) {
		context.data.putFloat(SPEED_KEY, 0);
		context.stall = false;
		setHeld(context, null);
		Level level = context.world;
		if (level != null && !level.isClientSide)
			releaseDrivenRack(context, level);
	}

	/** Whether the rack at this position is currently unable to turn. */
	private static boolean isJammed(Level level, BlockPos rackPos) {
		return level.getBlockEntity(rackPos) instanceof DrivenRackBlockEntity rack && rack.isJammed();
	}

	@Nullable
	private static Mesh heldMesh(MovementContext context) {
		if (!context.data.contains(HELD_RACK_KEY))
			return null;
		return new Mesh(BlockPos.of(context.data.getLong(HELD_RACK_KEY)), context.data.getFloat(HELD_SPEED_KEY));
	}

	private static void setHeld(MovementContext context, @Nullable Mesh mesh) {
		if (mesh == null) {
			context.data.remove(HELD_RACK_KEY);
			context.data.remove(HELD_SPEED_KEY);
			return;
		}
		context.data.putLong(HELD_RACK_KEY, mesh.rackPos()
			.asLong());
		context.data.putFloat(HELD_SPEED_KEY, mesh.speed());
	}

	/**
	 * Hands the rotation to the rack being rolled over. The rack left behind is released first, in
	 * the same tick, so two racks never claim to power the same network at once.
	 */
	private static void updateDrivenRack(MovementContext context, Level level, @Nullable Mesh mesh) {
		BlockPos target = mesh == null ? null : mesh.rackPos();
		BlockPos previous = readDrivenRack(context);
		if (previous != null && !previous.equals(target))
			releaseDrivenRack(context, level);

		if (mesh == null)
			return;
		if (level.getBlockEntity(target) instanceof DrivenRackBlockEntity rack) {
			rack.drive(mesh.speed(), driverId(context), level.getGameTime());
			context.data.putLong(DRIVEN_RACK_KEY, target.asLong());
		}
	}

	private static void releaseDrivenRack(MovementContext context, Level level) {
		BlockPos previous = readDrivenRack(context);
		if (previous == null)
			return;
		context.data.remove(DRIVEN_RACK_KEY);
		if (level.getBlockEntity(previous) instanceof DrivenRackBlockEntity rack)
			rack.stopDriving(driverId(context));
	}

	@Nullable
	private static BlockPos readDrivenRack(MovementContext context) {
		return context.data.contains(DRIVEN_RACK_KEY) ? BlockPos.of(context.data.getLong(DRIVEN_RACK_KEY)) : null;
	}

	/** Identifies the contraption driving a rack, so only it can revoke the rotation again. */
	private static int driverId(MovementContext context) {
		AbstractContraptionEntity entity = context.contraption.entity;
		return entity == null ? 0 : entity.getId();
	}

	/** The rack the pinion is currently rolling along, and how fast that turns it. */
	@Nullable
	private static Mesh findMesh(MovementContext context) {
		Level level = context.world;
		if (level == null || context.disabled || context.contraption.stalled)
			return null;

		Vec3 motion = context.motion;
		if (motion.length() < RackMeshing.MOTION_EPSILON)
			return null;

		Axis pinionAxis = worldAxis(context);
		if (pinionAxis == null)
			return null;

		BlockPos pos = BlockPos.containing(context.position);
		Mesh best = null;
		for (Direction toRack : Direction.values()) {
			if (!RackMeshing.canMesh(pinionAxis, toRack))
				continue;

			Axis rackAxis = RackMeshing.meshedRackAxis(pinionAxis, toRack);
			double along = motion.dot(RackMeshing.unit(rackAxis));
			if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
				continue;

			BlockPos rackPos = pos.relative(toRack);
			BlockState rack = level.getBlockState(rackPos);
			if (!RackTeeth.runsAlong(rack, rackAxis))
				continue;
			// A driven rack only takes power off along its own shaft.
			if (rack.getBlock() instanceof DrivenRackBlock driven && driven.getRotationAxis(rack) != pinionAxis)
				continue;

			float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
			if (best == null || Math.abs(speed) > Math.abs(best.speed()))
				best = new Mesh(rackPos, speed);
		}
		return best;
	}

	/** The pinion's rotation axis in world space, or null while a rotating contraption holds it askew. */
	@Nullable
	public static Axis worldAxis(MovementContext context) {
		Vec3 axis = context.rotation.apply(RackMeshing.unit(context.state.getValue(RackPinionBlock.AXIS)));
		for (Axis candidate : Axis.values())
			if (Math.abs(axis.dot(RackMeshing.unit(candidate))) > 0.9)
				return candidate;
		return null;
	}

	@Override
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		if (!VisualizationManager.supportsVisualization(context.world))
			RackPinionActorRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
		MovementContext movementContext) {
		return new RackPinionActorVisual(visualizationContext, simulationWorld, movementContext);
	}

	private record Mesh(BlockPos rackPos, float speed) {
	}
}
