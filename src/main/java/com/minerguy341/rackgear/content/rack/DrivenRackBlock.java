package com.minerguy341.rackgear.content.rack;

import com.minerguy341.rackgear.registry.RackGearBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A rack segment that takes power off the line: a pinion rolling over it drives its shaft, and the
 * shaft turns whatever it is connected to. This is the world-side generator for a pinion that rides
 * a contraption — rotation cannot leave a contraption, so it is produced here instead.
 *
 * <p>Two axes matter and they are always perpendicular: {@link #AXIS} is the bar, matching the rack
 * line it sits in, while the shaft leaves along one of the two axes across it, picked by
 * {@link #SHAFT_ALONG_FIRST} and cycled with a wrench.
 */
public class DrivenRackBlock extends RotatedPillarKineticBlock implements RackTeeth, IBE<DrivenRackBlockEntity> {

	/** Of the two axes across the bar, whether the shaft uses the first in X, Y, Z order. */
	public static final BooleanProperty SHAFT_ALONG_FIRST = BooleanProperty.create("shaft_along_first");

	public DrivenRackBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(SHAFT_ALONG_FIRST, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(SHAFT_ALONG_FIRST));
	}

	@Override
	public Axis getBarAxis(BlockState state) {
		return state.getValue(AXIS);
	}

	/** The axis the shaft leaves along, which is also the axis a meshing pinion must rotate about. */
	@Override
	public Axis getRotationAxis(BlockState state) {
		return shaftAxis(state.getValue(AXIS), state.getValue(SHAFT_ALONG_FIRST));
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == getRotationAxis(state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		Axis bar = state.getValue(AXIS);
		// Aim the shaft across the player's line of sight, which is where a shaft is easiest to reach.
		Axis facing = context.getHorizontalDirection()
			.getAxis();
		boolean alongFirst = facing == bar || facing == shaftAxis(bar, true);
		return state.setValue(SHAFT_ALONG_FIRST, alongFirst);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!level.isClientSide)
			level.setBlockAndUpdate(pos, state.cycle(SHAFT_ALONG_FIRST));
		return InteractionResult.SUCCESS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return RackTeeth.shapeFor(state.getValue(AXIS));
	}

	@Override
	public Class<DrivenRackBlockEntity> getBlockEntityClass() {
		return DrivenRackBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DrivenRackBlockEntity> getBlockEntityType() {
		return RackGearBlockEntities.DRIVEN_RACK.get();
	}

	/** Of the two axes across {@code bar}, the first or the second in X, Y, Z order. */
	public static Axis shaftAxis(Axis bar, boolean alongFirst) {
		return switch (bar) {
			case X -> alongFirst ? Axis.Y : Axis.Z;
			case Y -> alongFirst ? Axis.X : Axis.Z;
			case Z -> alongFirst ? Axis.X : Axis.Y;
		};
	}
}
