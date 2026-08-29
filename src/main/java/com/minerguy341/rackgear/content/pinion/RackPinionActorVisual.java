package com.minerguy341.rackgear.content.pinion;

import com.minerguy341.rackgear.client.RackGearPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction.Axis;

/** Instanced counterpart of {@link RackPinionActorRenderer}, used while Flywheel is active. */
public class RackPinionActorVisual extends ActorVisual {

	private final TransformedInstance cog;
	private final Axis axis;

	private float angle;
	private float previousAngle;

	public RackPinionActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld contraption,
		MovementContext context) {
		super(visualizationContext, contraption, context);
		axis = context.state.getValue(RackPinionBlock.AXIS);
		cog = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(RackGearPartialModels.RACK_PINION))
			.createInstance();
	}

	@Override
	public void tick() {
		previousAngle = angle;
		angle = context.data.getFloat(RackPinionMovementBehaviour.ANGLE_KEY);
	}

	@Override
	public void beginFrame() {
		float spin = AngleHelper.angleLerp(AnimationTickHolder.getPartialTicks(), previousAngle, angle);
		cog.setIdentityTransform()
			.translate(context.localPos)
			.center()
			// The cog is authored along Y, so it is turned onto its axis with the same rotations the
			// blockstate uses, and then spun around its own axis, which is innermost.
			.rotateYDegrees(blockstateYRotation())
			.rotateXDegrees(blockstateXRotation())
			.rotateYDegrees(spin)
			.uncenter()
			.setChanged();
	}

	private float blockstateXRotation() {
		return axis == Axis.Y ? 0 : 90;
	}

	private float blockstateYRotation() {
		return switch (axis) {
			case X -> 90;
			case Y -> 0;
			case Z -> 180;
		};
	}

	@Override
	protected void _delete() {
		cog.delete();
	}
}
