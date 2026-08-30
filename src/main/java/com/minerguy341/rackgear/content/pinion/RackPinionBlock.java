package com.minerguy341.rackgear.content.pinion;

import com.minerguy341.rackgear.registry.RackGearBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A large cogwheel that also acts as a generator: racks moving past it drive the network it is part
 * of. Implementing {@link ICogWheel} as a large cog means Create's rotation propagator meshes it
 * with cogwheels exactly like its own large cogwheel does.
 */
public class RackPinionBlock extends AbstractShaftBlock implements ICogWheel {

	public RackPinionBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isLargeCog() {
		return true;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.LARGE_GEAR.get(state.getValue(AXIS));
	}

	/**
	 * A pinion sits where a large cogwheel could: not beside another cog whose teeth it would pass
	 * through without ever meshing. Create's own rule, applied to the same block.
	 */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return CogWheelBlock.isValidCogwheelPosition(true, level, pos, state.getValue(AXIS));
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return RackGearBlockEntities.RACK_PINION.get();
	}
}
