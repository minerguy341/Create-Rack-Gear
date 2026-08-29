package com.minerguy341.rackgear.registry;

import com.minerguy341.rackgear.CreateRackGear;
import com.minerguy341.rackgear.content.pinion.RackPinionBlockEntity;
import com.minerguy341.rackgear.content.pinion.RackPinionRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class RackGearBlockEntities {

	private static final CreateRegistrate REGISTRATE = CreateRackGear.registrate();

	public static final BlockEntityEntry<RackPinionBlockEntity> RACK_PINION = REGISTRATE
		.blockEntity("rack_pinion", RackPinionBlockEntity::new)
		.validBlocks(RackGearBlocks.RACK_PINION)
		.renderer(() -> RackPinionRenderer::new)
		.register();

	/** Called from the mod constructor to force class loading, which runs the registrations above. */
	public static void register() {
	}
}
