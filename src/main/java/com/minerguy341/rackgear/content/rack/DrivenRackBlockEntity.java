package com.minerguy341.rackgear.content.rack;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the rotation a passing pinion is producing. Modelled on Create's powered shaft: the speed is
 * pushed in from outside rather than computed here, the driver is remembered so that only it can
 * revoke the rotation, and a missed stop is caught by a short grace period.
 */
public class DrivenRackBlockEntity extends GeneratingKineticBlockEntity {

	/** Ticks the rotation survives without being renewed, in case a driver disappears mid-drive. */
	private static final int GRACE_TICKS = 2;

	private static final int NO_DRIVER = -1;

	private float drivenSpeed;
	private int driverId = NO_DRIVER;
	private long lastDrivenTick = Long.MIN_VALUE;

	public DrivenRackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public float getGeneratedSpeed() {
		return drivenSpeed;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || drivenSpeed == 0)
			return;
		if (level.getGameTime() - lastDrivenTick > GRACE_TICKS)
			stopDriving();
	}

	/** Called by a rolling pinion each tick it is meshed with this rack. */
	public void drive(float speed, int driverId, long gameTime) {
		this.driverId = driverId;
		this.lastDrivenTick = gameTime;
		if (speed == drivenSpeed)
			return;
		drivenSpeed = speed;
		updateGeneratedRotation();
	}

	/** Called by a pinion that has moved on, so the rotation stops in the same tick the next one starts. */
	public void stopDriving(int driverId) {
		if (this.driverId == driverId)
			stopDriving();
	}

	private void stopDriving() {
		driverId = NO_DRIVER;
		if (drivenSpeed == 0)
			return;
		drivenSpeed = 0;
		updateGeneratedRotation();
	}
}
