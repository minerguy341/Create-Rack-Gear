package com.minerguy341.rackgear.content.pinion;

import org.jetbrains.annotations.Nullable;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.rack.RackBlock;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
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
 * Makes a pinion riding a contraption roll along racks placed in the world — the mirror image of
 * {@link RackPinionBlockEntity}, which watches racks being carried past a pinion standing in the
 * world.
 *
 * <p>As a contraption actor the pinion is ticked with its world position and motion on both sides,
 * so the rolling speed is read straight from the contraption's motion rather than reconstructed. The
 * rotation it produces stays on the contraption: contraptions have no kinetic network, so it drives
 * the animation and nothing else.
 */
public class RackPinionMovementBehaviour implements MovementBehaviour {

	/** Rolling speed in RPM, kept in the actor's data so the renderers can read it. */
	public static final String SPEED_KEY = "RollingSpeed";

	/** Accumulated rotation in degrees, so the cog keeps its phase from tick to tick. */
	public static final String ANGLE_KEY = "Angle";

	/** Create animates a kinetic block at {@code speed * 3/10} degrees per tick. */
	private static final float DEGREES_PER_TICK_PER_RPM = 3 / 10f;

	/** The actor draws the cog itself, so the block entity copy in the contraption must stay hidden. */
	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}

	@Override
	public void tick(MovementContext context) {
		float speed = rollingSpeed(context);
		context.data.putFloat(SPEED_KEY, speed);
		context.data.putFloat(ANGLE_KEY,
			(context.data.getFloat(ANGLE_KEY) + speed * DEGREES_PER_TICK_PER_RPM) % 360);
	}

	@Override
	public void stopMoving(MovementContext context) {
		context.data.putFloat(SPEED_KEY, 0);
	}

	/** The speed the pinion is currently being rolled at by the rack underneath it, in RPM. */
	public static float rollingSpeed(MovementContext context) {
		Level level = context.world;
		if (level == null || context.disabled || context.contraption.stalled)
			return 0;

		Vec3 motion = context.motion;
		if (motion.length() < RackMeshing.MOTION_EPSILON)
			return 0;

		Axis pinionAxis = worldAxis(context);
		if (pinionAxis == null)
			return 0;

		BlockPos pos = BlockPos.containing(context.position);
		float fastest = 0;
		for (Direction toRack : Direction.values()) {
			if (!RackMeshing.canMesh(pinionAxis, toRack))
				continue;

			Axis rackAxis = RackMeshing.meshedRackAxis(pinionAxis, toRack);
			double along = motion.dot(RackMeshing.unit(rackAxis));
			if (Math.abs(along) < RackMeshing.MOTION_EPSILON)
				continue;

			BlockState rack = level.getBlockState(pos.relative(toRack));
			if (!(rack.getBlock() instanceof RackBlock) || rack.getValue(RackBlock.AXIS) != rackAxis)
				continue;

			float speed = RackMeshing.toRotationSpeed(along, RackMeshing.meshDirection(pinionAxis, toRack));
			if (Math.abs(speed) > Math.abs(fastest))
				fastest = speed;
		}
		return fastest;
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
}
