package com.minerguy341.rackgear.content.pinion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the pinion spinning at its current network speed.
 *
 * <p>{@link KineticBlockEntityRenderer} bails out while Flywheel is active because Create's own
 * kinetic blocks provide a Flywheel visual instead. This block has none, so the rotation is drawn
 * here on every backend.
 */
public class RackPinionRenderer extends KineticBlockEntityRenderer<RackPinionBlockEntity> {

	public RackPinionRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RackPinionBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		renderRotatingBuffer(be, CachedBuffers.block(KINETIC_BLOCK, state), ms,
			buffer.getBuffer(getRenderType(be, state)), light);
	}
}
