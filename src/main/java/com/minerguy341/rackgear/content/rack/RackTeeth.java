package com.minerguy341.rackgear.content.rack;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block a pinion can roll along. Implemented by the plain {@link RackBlock} and by
 * {@link DrivenRackBlock}, so a rack line can be built from either and a pinion meshes with both.
 */
public interface RackTeeth {

	VoxelShape SHAPE_X = Block.box(0, 1, 1, 16, 15, 15);
	VoxelShape SHAPE_Y = Block.box(1, 0, 1, 15, 16, 15);
	VoxelShape SHAPE_Z = Block.box(1, 1, 0, 15, 15, 16);

	/** The axis the toothed bar runs along, which is the direction a pinion travels over it. */
	Axis getBarAxis(BlockState state);

	static VoxelShape shapeFor(Axis barAxis) {
		return switch (barAxis) {
			case X -> SHAPE_X;
			case Y -> SHAPE_Y;
			case Z -> SHAPE_Z;
		};
	}

	/** The bar axis of the rack at the given state, or null when the block is not a rack. */
	static Axis barAxisOf(BlockState state) {
		return state.getBlock() instanceof RackTeeth rack ? rack.getBarAxis(state) : null;
	}

	/** Convenience for the common "is this a rack running along the axis I need" test. */
	static boolean runsAlong(BlockState state, Axis axis) {
		return barAxisOf(state) == axis;
	}
}
