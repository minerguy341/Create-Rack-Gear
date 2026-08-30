package com.minerguy341.rackgear.content.rack;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
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

	/** Ticks a jammed rack is driven before its teeth give way, matching the pinion. */
	private static final int STRAIN_TICKS = 60;

	private float drivenSpeed;
	private int strainTicks;
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

		if (level.getGameTime() - lastDrivenTick > GRACE_TICKS) {
			stopDriving();
			strainTicks = 0;
			return;
		}

		// A pinion still rolling into a network that will not turn strips the rack it is riding.
		if (!isJammed()) {
			strainTicks = 0;
			return;
		}
		if (++strainTicks > STRAIN_TICKS)
			stripTeeth();
	}

	/** Breaks the rack once a pinion has been driving it against a jammed network for too long. */
	private void stripTeeth() {
		level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 0.6f);
		level.destroyBlock(worldPosition, true);
	}


	/**
	 * The generated speed has to reach the client: the goggle tooltip is built there, and it scales
	 * the capacity a generator provides by {@code getGeneratedSpeed() / getTheoreticalSpeed()}. Left
	 * unsynced, that ratio is zero on the client and the goggles read 0 su while the stressometer,
	 * which reports the network as the server computed it, reads the real figure.
	 */
	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putFloat("DrivenSpeed", drivenSpeed);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		drivenSpeed = compound.getFloat("DrivenSpeed");
	}

	/** Whether the network this rack drives is currently unable to turn, so a pinion cannot roll on. */
	public boolean isJammed() {
		return overStressed;
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
