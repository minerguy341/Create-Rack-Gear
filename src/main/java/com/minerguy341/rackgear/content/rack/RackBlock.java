package com.minerguy341.rackgear.content.rack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A toothed bar. It has no kinetic behaviour of its own: a {@code RackPinionBlock} watches for racks
 * moving past it and converts their motion into rotation, so a rack is only ever a passive tooth
 * strip, whether it sits in the world or rides a contraption.
 *
 * <p>Teeth run along all four sides of the bar, so a pinion meshes with it from any direction
 * perpendicular to both the bar and the pinion's shaft.
 */
public class RackBlock extends RotatedPillarBlock {

	private static final VoxelShape SHAPE_X = Block.box(0, 1, 1, 16, 15, 15);
	private static final VoxelShape SHAPE_Y = Block.box(1, 0, 1, 15, 16, 15);
	private static final VoxelShape SHAPE_Z = Block.box(1, 1, 0, 15, 15, 16);

	public RackBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> SHAPE_X;
			case Y -> SHAPE_Y;
			case Z -> SHAPE_Z;
		};
	}
}
