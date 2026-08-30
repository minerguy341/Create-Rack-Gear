package com.minerguy341.rackgear.registry;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import com.minerguy341.rackgear.CreateRackGear;
import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.pinion.RackPinionBlock;
import com.minerguy341.rackgear.content.pinion.RackPinionBlockItem;
import com.minerguy341.rackgear.content.pinion.RackPinionModel;
import com.minerguy341.rackgear.content.pinion.RackPinionMovementBehaviour;
import com.minerguy341.rackgear.content.rack.DrivenRackBlock;
import com.minerguy341.rackgear.content.rack.RackMovementBehaviour;
import com.minerguy341.rackgear.content.rack.RackBlock;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

/**
 * Block registry. Registrate generates the blockstate, item model, loot table and en_us entry for
 * these when {@code gradlew runData} is run; the block models themselves are authored by hand under
 * {@code src/main/resources}.
 */
public class RackGearBlocks {

	/** Stress the pinion can support per RPM it generates, matching Create's water wheel. */
	private static final double STRESS_CAPACITY = 8;

	private static final CreateRegistrate REGISTRATE = CreateRackGear.registrate();

	public static final BlockEntry<RackBlock> RACK = REGISTRATE.block("rack", RackBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.METAL)
			.sound(SoundType.NETHERITE_BLOCK)
			.noOcclusion())
		.transform(pickaxeOnly())
		.blockstate(BlockStateGen.axisBlockProvider(false))
		.onRegister(MovementBehaviour.movementBehaviour(new RackMovementBehaviour()))
		.simpleItem()
		.register();

	public static final BlockEntry<DrivenRackBlock> DRIVEN_RACK = REGISTRATE
		.block("driven_rack", DrivenRackBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
			.sound(SoundType.NETHERITE_BLOCK)
			.noOcclusion())
		.transform(pickaxeOnly())
		.blockstate((c, p) -> p.getVariantBuilder(c.get())
			.forAllStatesExcept(state -> {
				Axis axis = state.getValue(DrivenRackBlock.AXIS);
				return ConfiguredModel.builder()
					.modelFile(p.models()
						.getExistingFile(p.modLoc("block/driven_rack")))
					.rotationX(axis == Axis.Y ? 0 : 90)
					.rotationY(axis == Axis.X ? 90 : axis == Axis.Z ? 180 : 0)
					.build();
			}, DrivenRackBlock.SHAFT_ALONG_FIRST))
		.onRegister(MovementBehaviour.movementBehaviour(new RackMovementBehaviour()))
		.onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> STRESS_CAPACITY))
		.onRegister(BlockStressValues.setGeneratorSpeed((int) RackMeshing.MAX_RPM, true))
		.simpleItem()
		.register();

	public static final BlockEntry<RackPinionBlock> RACK_PINION = REGISTRATE
		.block("rack_pinion", RackPinionBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.DIRT)
			.sound(SoundType.WOOD)
			.noOcclusion())
		.transform(axeOrPickaxe())
		.blockstate(BlockStateGen.axisBlockProvider(false))
		.onRegister(CreateRegistrate.blockModel(() -> RackPinionModel::new))
		.onRegister(MovementBehaviour.movementBehaviour(new RackPinionMovementBehaviour()))
		.onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> STRESS_CAPACITY))
		.onRegister(BlockStressValues.setGeneratorSpeed((int) RackMeshing.MAX_RPM, true))
		.item(RackPinionBlockItem::new)
		.build()
		.register();

	/** Called from the mod constructor to force class loading, which runs the registrations above. */
	public static void register() {
	}
}
