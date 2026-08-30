package com.minerguy341.rackgear.content.sublevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/**
 * Meshing against Sable sub-levels, the moving structures Create: Aeronautics and its relatives
 * assemble.
 *
 * <p>A sub-level is not a Create contraption: it keeps its blocks real, in a level of its own, and
 * ticks them. That makes both directions of the mechanic work there without a Driven Rack — a pinion
 * riding a ship has a kinetic network to generate into, which a pinion on a contraption never does.
 * It also means none of the contraption code reaches it, hence this second path.
 *
 * <p>All Sable types stay inside {@link SableRackSubLevels}, which is only ever loaded when Sable is.
 */
public interface RackSubLevels {

	RackSubLevels NONE = new RackSubLevels() {
	};

	RackSubLevels INSTANCE = detect();

	/** Rotation a pinion standing in the world takes from racks a sub-level carries past it. */
	default float speedFromCarriedRacks(Level level, BlockPos pinionPos, Axis pinionAxis) {
		return 0;
	}

	/** Rotation a pinion riding a sub-level takes from racks standing still in the world. */
	default float speedFromWorldRacks(BlockEntity pinion, Axis axisAboard) {
		return 0;
	}

	private static RackSubLevels detect() {
		if (!ModList.get()
			.isLoaded("sable"))
			return NONE;
		try {
			return new SableRackSubLevels();
		} catch (Throwable ignored) {
			// A Sable that no longer matches this integration should cost the mod nothing.
			return NONE;
		}
	}
}
