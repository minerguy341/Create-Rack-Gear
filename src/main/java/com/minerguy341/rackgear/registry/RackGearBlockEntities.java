package com.minerguy341.rackgear.registry;

import com.minerguy341.rackgear.CreateRackGear;
import com.minerguy341.rackgear.content.pinion.RackPinionBlockEntity;
import com.minerguy341.rackgear.content.rack.DrivenRackBlockEntity;
import com.minerguy341.rackgear.content.pinion.RackPinionRenderer;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class RackGearBlockEntities {

	private static final CreateRegistrate REGISTRATE = CreateRackGear.registrate();

	public static final BlockEntityEntry<RackPinionBlockEntity> RACK_PINION = REGISTRATE
		.blockEntity("rack_pinion", RackPinionBlockEntity::new)
		.validBlocks(RackGearBlocks.RACK_PINION)
		.renderer(() -> RackPinionRenderer::new)
		.register();

	/**
	 * The driven rack's shaft is drawn by Create's own shaft visual, so it is instanced through
	 * Flywheel like every other shaft and falls back to the block entity renderer without it.
	 */
	public static final BlockEntityEntry<DrivenRackBlockEntity> DRIVEN_RACK = REGISTRATE
		.blockEntity("driven_rack", DrivenRackBlockEntity::new)
		.visual(() -> SingleAxisRotatingVisual::shaft, false)
		.validBlocks(RackGearBlocks.DRIVEN_RACK)
		.renderer(() -> ShaftRenderer::new)
		.register();

	/** Called from the mod constructor to force class loading, which runs the registrations above. */
	public static void register() {
	}
}
