package com.minerguy341.rackgear.content.pinion;

import java.util.Collections;
import java.util.List;

import net.createmod.ponder.render.VirtualRenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Keeps the pinion out of the chunk mesh so it isn't drawn twice: in the world the block entity
 * renderer draws the rotating copy, while virtual renders — contraptions and Ponder scenes, where no
 * block entity exists — still get the static model. This mirrors how Create hides its own cogwheels.
 */
public class RackPinionModel extends BakedModelWrapper<BakedModel> {

	public RackPinionModel(BakedModel template) {
		super(template);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
		RenderType renderType) {
		if (VirtualRenderHelper.isVirtual(data))
			return super.getQuads(state, side, rand, data, renderType);
		return Collections.emptyList();
	}
}
