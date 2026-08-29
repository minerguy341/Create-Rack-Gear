package com.minerguy341.rackgear.content.pinion;

import com.minerguy341.rackgear.client.RackGearPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;

/** Draws the rolling pinion on a contraption when Flywheel is not instancing it. */
public class RackPinionActorRenderer {

	private RackPinionActorRenderer() {
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		// The caller has already translated to the block's position within the contraption.
		float angle = context.data.getFloat(RackPinionMovementBehaviour.ANGLE_KEY);
		Direction facing = Direction.fromAxisAndDirection(context.state.getValue(RackPinionBlock.AXIS),
			AxisDirection.POSITIVE);

		CachedBuffers.partialFacingVertical(RackGearPartialModels.RACK_PINION, context.state, facing)
			.transform(matrices.getModel())
			.rotateCentered(angle * Mth.DEG_TO_RAD, facing)
			.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
			.useLevelLight(context.world, matrices.getWorld())
			.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.solid()));
	}
}
