package com.minerguy341.rackgear.content.rack;

import com.minerguy341.rackgear.content.RackMeshing;
import com.minerguy341.rackgear.content.pinion.RackPinionBlock;
import com.minerguy341.rackgear.content.pinion.RackPinionBlockEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Locks a contraption whose rack is meshed with a pinion that cannot turn.
 *
 * <p>A rack is otherwise a passive block, and this is the one thing it needs a tick of its own for:
 * only an actor can stall the contraption carrying it. Teeth that cannot turn the gear they are
 * pressed against do not slip past it, so an overstressed network stops the piston pushing the rack
 * rather than quietly stopping itself.
 */
public class RackMovementBehaviour implements MovementBehaviour {

	@Override
	public void tick(MovementContext context) {
		Level level = context.world;
		if (level == null || level.isClientSide)
			return;
		context.stall = meshedWithJammedPinion(context, level);
	}

	@Override
	public void stopMoving(MovementContext context) {
		context.stall = false;
	}

	/** Whether a pinion this rack is meshed with is currently unable to turn. */
	private static boolean meshedWithJammedPinion(MovementContext context, Level level) {
		Axis barAxis = worldBarAxis(context);
		if (barAxis == null)
			return false;

		BlockPos pos = BlockPos.containing(context.position);
		for (Direction toPinion : Direction.values()) {
			if (toPinion.getAxis() == barAxis)
				continue;

			// The pinion's shaft is the axis across both the bar and the way we look for it.
			Axis pinionAxis = RackMeshing.thirdAxis(barAxis, toPinion.getAxis());
			BlockPos pinionPos = pos.relative(toPinion);
			BlockState pinion = level.getBlockState(pinionPos);
			if (!(pinion.getBlock() instanceof RackPinionBlock) || pinion.getValue(RackPinionBlock.AXIS) != pinionAxis)
				continue;
			if (level.getBlockEntity(pinionPos) instanceof RackPinionBlockEntity be && be.isJammed())
				return true;
		}
		return false;
	}

	/** The bar's axis in world space, or null while a rotating contraption holds the rack askew. */
	private static Axis worldBarAxis(MovementContext context) {
		Axis local = RackTeeth.barAxisOf(context.state);
		if (local == null)
			return null;

		Vec3 bar = context.rotation.apply(RackMeshing.unit(local));
		for (Axis candidate : Axis.values())
			if (Math.abs(bar.dot(RackMeshing.unit(candidate))) > 0.9)
				return candidate;
		return null;
	}
}
