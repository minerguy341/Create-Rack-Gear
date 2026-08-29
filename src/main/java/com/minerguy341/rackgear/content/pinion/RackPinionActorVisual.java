package com.minerguy341.rackgear.content.pinion;

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
		cog = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.block(context.state))
			.createInstance();
	}

	@Override
	public void tick() {
		previousAngle = angle;
		angle = context.data.getFloat(RackPinionMovementBehaviour.ANGLE_KEY);
	}

	@Override
	public void beginFrame() {
		float partialTicks = AnimationTickHolder.getPartialTicks();
		cog.setIdentityTransform()
			.translate(context.localPos)
			.rotateCenteredDegrees(AngleHelper.angleLerp(partialTicks, previousAngle, angle), axis)
			.setChanged();
	}

	@Override
	protected void _delete() {
		cog.delete();
	}
}
