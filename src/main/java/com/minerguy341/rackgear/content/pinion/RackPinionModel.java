package com.minerguy341.rackgear.content.pinion;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Keeps the pinion out of the baked mesh so it is never drawn twice. Every place the block appears,
 * something else draws it turning: {@link RackPinionRenderer} in the world, and
 * {@link RackPinionActorVisual} or {@link RackPinionActorRenderer} while it rides a contraption.
 */
public class RackPinionModel extends BakedModelWrapper<BakedModel> {

	public RackPinionModel(BakedModel template) {
		super(template);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
		RenderType renderType) {
		return Collections.emptyList();
	}
}
