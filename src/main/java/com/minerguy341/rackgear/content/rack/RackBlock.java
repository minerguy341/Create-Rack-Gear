package com.minerguy341.rackgear.content.rack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A toothed bar with no kinetics of its own. Teeth run along all four sides, so a pinion meshes with
 * it from any direction perpendicular to both the bar and the pinion's shaft.
 *
 * <p>Rotation is produced by whichever side of the pair is standing in the world: a pinion placed in
 * the world when the rack rides a contraption, or a {@link DrivenRackBlock} in the line when the
 * pinion is the one riding.
 */
public class RackBlock extends RotatedPillarBlock implements RackTeeth {

	public RackBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Axis getBarAxis(BlockState state) {
		return state.getValue(AXIS);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return RackTeeth.shapeFor(state.getValue(AXIS));
	}
}
