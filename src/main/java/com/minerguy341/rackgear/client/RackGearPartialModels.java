package com.minerguy341.rackgear.client;

import com.minerguy341.rackgear.CreateRackGear;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Models rendered outside the baked chunk mesh.
 *
 * <p>A block whose model is drawn turning cannot also sit in the chunk mesh, so
 * {@code RackPinionModel} drops its quads and every renderer draws this partial instead — which is
 * how Create renders its own cogwheels.
 */
@EventBusSubscriber(modid = CreateRackGear.ID, value = Dist.CLIENT)
public class RackGearPartialModels {

	/** The pinion's cogwheel, authored along Y like Create's cog models. */
	public static final PartialModel RACK_PINION = PartialModel.of(CreateRackGear.asResource("block/rack_pinion"));

	@SubscribeEvent
	static void loadPartialModels(FMLClientSetupEvent event) {
		// Empty on purpose: the listener exists so this class, and with it the partial models above,
		// is loaded while Flywheel is still collecting the models it has to bake.
	}
}
