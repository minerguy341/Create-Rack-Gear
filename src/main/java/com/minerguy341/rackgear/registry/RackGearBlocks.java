package com.minerguy341.rackgear.registry;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import com.minerguy341.rackgear.CreateRackGear;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

/**
 * Block registry. Registrate generates the blockstate, models, item model, loot table and
 * en_us entry for everything declared here when {@code gradlew runData} is run.
 */
public class RackGearBlocks {

	private static final CreateRegistrate REGISTRATE = CreateRackGear.registrate();

	/**
	 * Placeholder for the mod's namesake block. Swap {@code Block::new} for a kinetic block
	 * (e.g. a subclass of {@code RotatedPillarKineticBlock}) and give it a
	 * {@code KineticBlockEntity} once the mechanics are in place.
	 */
	public static final BlockEntry<Block> RACK_GEAR = REGISTRATE
		.block("rack_gear", Block::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.PODZOL))
		.transform(pickaxeOnly())
		.simpleItem()
		.register();

	/** Called from the mod constructor to force class loading, which runs the registrations above. */
	public static void register() {
	}
}
