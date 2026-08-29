package com.minerguy341.rackgear.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.minerguy341.rackgear.content.rack.DrivenRackBlock;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

class RackMeshingTest {

	@Test
	void teethOnlyEngageAroundTheShaft() {
		assertFalse(RackMeshing.canMesh(Axis.Z, Direction.NORTH), "shaft faces cannot mesh");
		assertFalse(RackMeshing.canMesh(Axis.Z, Direction.SOUTH), "shaft faces cannot mesh");
		for (Direction side : new Direction[] { Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST })
			assertTrue(RackMeshing.canMesh(Axis.Z, side), side + " should mesh with a Z-axis pinion");
	}

	@Test
	void aMeshedRackRunsPerpendicularToBothTheShaftAndTheContact() {
		assertEquals(Axis.X, RackMeshing.meshedRackAxis(Axis.Z, Direction.DOWN));
		assertEquals(Axis.Y, RackMeshing.meshedRackAxis(Axis.Z, Direction.EAST));
		assertEquals(Axis.Z, RackMeshing.meshedRackAxis(Axis.Y, Direction.EAST));
	}

	@Test
	void oppositeSidesTurnThePinionOppositeWays() {
		double below = RackMeshing.meshDirection(Axis.Z, Direction.DOWN);
		double above = RackMeshing.meshDirection(Axis.Z, Direction.UP);
		assertEquals(1, below, "a rack below a Z pinion moving along +X drives a right-handed turn");
		assertEquals(-below, above, "the same rack above the pinion drives the opposite turn");
		assertEquals(-RackMeshing.meshDirection(Axis.Z, Direction.EAST),
			RackMeshing.meshDirection(Axis.Z, Direction.WEST));
	}

	@Test
	void everyMeshingSideGivesAWholeTurnDirection() {
		for (Axis axis : Axis.values())
			for (Direction side : Direction.values()) {
				if (!RackMeshing.canMesh(axis, side))
					continue;
				assertEquals(1, Math.abs(RackMeshing.meshDirection(axis, side)), 1e-9,
					"mesh direction for " + axis + "/" + side + " should be +/-1");
			}
	}

	@Test
	void aDrivenRacksShaftAlwaysCrossesItsBar() {
		for (Axis bar : Axis.values()) {
			Axis first = DrivenRackBlock.shaftAxis(bar, true);
			Axis second = DrivenRackBlock.shaftAxis(bar, false);
			assertNotEquals(bar, first, "the shaft cannot run along the bar");
			assertNotEquals(bar, second, "the shaft cannot run along the bar");
			assertNotEquals(first, second, "the two settings must pick different axes");
		}
	}

	@Test
	void aPistonDrivesThePinionAtItsOwnRpm() {
		// Create moves linear actuators at speed / 512 blocks per tick.
		double blocksPerTick = 32 / 512d;
		assertEquals(32, RackMeshing.toRotationSpeed(blocksPerTick, 1));
		assertEquals(-32, RackMeshing.toRotationSpeed(blocksPerTick, -1), "reversing the mesh reverses the pinion");
		assertEquals(-32, RackMeshing.toRotationSpeed(-blocksPerTick, 1), "reversing the rack reverses the pinion");
	}

	@Test
	void generatedSpeedIsRoundedAndCapped() {
		assertEquals(1, RackMeshing.toRotationSpeed(1.4 / 512d, 1), "speeds are rounded to whole RPM");
		assertEquals(0, RackMeshing.toRotationSpeed(0.4 / 512d, 1), "creeping movement generates nothing");
		assertEquals(RackMeshing.MAX_RPM, RackMeshing.toRotationSpeed(1, 1), "capped at Create's maximum speed");
		assertEquals(-RackMeshing.MAX_RPM, RackMeshing.toRotationSpeed(1, -1));
	}
}
