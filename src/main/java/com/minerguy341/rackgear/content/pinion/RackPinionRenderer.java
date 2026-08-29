package com.minerguy341.rackgear.content.pinion;

import com.minerguy341.rackgear.client.RackGearPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the pinion spinning when Flywheel is not instancing it.
 *
 * <p>The blockstate model is not drawn here: its quads are suppressed so the block stays out of the
 * chunk mesh, so the cog comes from a partial model, oriented onto the block's axis the same way
 * Create renders a large cogwheel.
 */
public class RackPinionRenderer extends KineticBlockEntityRenderer<RackPinionBlockEntity> {

	public RackPinionRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(RackPinionBlockEntity be, BlockState state) {
		Direction facing = Direction.fromAxisAndDirection(getRotationAxisOf(be), AxisDirection.POSITIVE);
		return CachedBuffers.partialFacingVertical(RackGearPartialModels.RACK_PINION, state, facing);
	}
}
