package com.minerguy341.rackgear;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import com.minerguy341.rackgear.registry.RackGearBlockEntities;
import com.minerguy341.rackgear.registry.RackGearBlocks;
import com.minerguy341.rackgear.registry.RackGearCreativeTab;

/**
 * Entry point of Create: Rack Gear.
 *
 * <p>Registration goes through a {@link CreateRegistrate}, the same builder Create itself uses, so
 * blocks, items and their data generators are declared in one place (see {@link RackGearBlocks}).
 */
@Mod(CreateRackGear.ID)
public class CreateRackGear {

	public static final String ID = "create_rack_gear";
	public static final String NAME = "Create: Rack Gear";
	public static final Logger LOGGER = LogUtils.getLogger();

	private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);

	public CreateRackGear(IEventBus modEventBus, ModContainer container) {
		REGISTRATE.registerEventListeners(modEventBus);

		RackGearCreativeTab.register(modEventBus);
		// Entries built after this call land in our creative tab automatically.
		REGISTRATE.setCreativeTab(RackGearCreativeTab.MAIN);

		RackGearBlocks.register();
		RackGearBlockEntities.register();

		REGISTRATE.addDataGenerator(ProviderType.LANG,
			provider -> provider.add("itemGroup." + ID, NAME));

		LOGGER.info("{} loaded", NAME);
	}

	public static CreateRegistrate registrate() {
		return REGISTRATE;
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}
}
