package com.minerguy341.rackgear.content.pinion;

import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

import java.util.function.Predicate;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Places pinions the way Create places cogwheels: click the face of a cog already standing there and
 * the next one lands where it will actually mesh, rather than where the cursor happened to point.
 *
 * <p>Create's own {@code CogwheelBlockItem} cannot be reused — it takes a {@code CogWheelBlock}, which
 * this pinion is not, being a generator — so its large cogwheel helper is mirrored here on top of the
 * shared {@code DiagonalCogHelper}.
 */
public class RackPinionBlockItem extends BlockItem {

	private final int placementHelperId;

	public RackPinionBlockItem(Block block, Properties properties) {
		super(block, properties);
		placementHelperId = PlacementHelpers.register(new PinionPlacementHelper());
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();

		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		BlockHitResult ray = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, true);
		if (helper.matchesState(state) && player != null && !player.isShiftKeyDown())
			return helper.getOffset(player, level, state, pos, ray)
				.placeInWorld(level, this, player, context.getHand(), ray)
				.result();

		return super.onItemUseFirst(stack, context);
	}

	/** Offers the position a pinion would mesh from, given the cogwheel that was clicked. */
	private static class PinionPlacementHelper extends CogwheelBlockItem.DiagonalCogHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return stack -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof RackPinionBlock;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			if (hitOnShaft(state, ray))
				return PlacementOffset.fail();

			// Against a small cog, a large one goes diagonally, which the shared helper already does.
			if (!ICogWheel.isLargeCog(state))
				return super.getOffset(player, world, state, pos, ray);

			// Against another large cog, it goes one across and one along, on a crossing axis.
			Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
			Direction side = IPlacementHelper.orderedByDistanceOnlyAxis(pos, ray.getLocation(), axis)
				.get(0);
			for (Direction dir : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), axis)) {
				BlockPos placement = pos.relative(dir)
					.relative(side);
				if (!CogWheelBlock.isValidCogwheelPosition(true, world, placement, dir.getAxis()))
					continue;
				if (!world.getBlockState(placement)
					.canBeReplaced())
					continue;
				return PlacementOffset.success(placement, s -> s.setValue(AXIS, dir.getAxis()));
			}
			return PlacementOffset.fail();
		}
	}
}
