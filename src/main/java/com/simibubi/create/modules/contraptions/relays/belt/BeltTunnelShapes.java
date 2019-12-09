package com.simibubi.create.modules.contraptions.relays.belt;

import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

import static net.minecraft.block.Block.makeCuboidShape;

public class BeltTunnelShapes {

	private static VoxelShape block = makeCuboidShape(0, -5, 0, 16, 16, 16);

	private static VoxelShaper opening = VoxelShaper.forHorizontal( makeCuboidShape(2, -5, 14, 14, 8, 16), Direction.SOUTH);
	private static VoxelShaper notch = VoxelShaper.forHorizontal(makeCuboidShape(2, 14, 14, 14, 16, 16), Direction.SOUTH);

	private static final VoxelShaper
			STRAIGHT = VoxelShaper.forHorizontalAxis(
					VoxelShapes.combineAndSimplify(
							block,
							VoxelShapes.or(
									opening.get(Direction.SOUTH),
									opening.get(Direction.NORTH),
									notch.get(Direction.WEST),
									notch.get(Direction.EAST)
							),
							IBooleanFunction.NOT_SAME),
					Direction.SOUTH),

			TEE = VoxelShaper.forHorizontal(
					VoxelShapes.combineAndSimplify(
							block,
							VoxelShapes.or(
									notch.get(Direction.SOUTH),
									opening.get(Direction.NORTH),
									opening.get(Direction.WEST),
									opening.get(Direction.EAST)
							),
							IBooleanFunction.NOT_SAME),
					Direction.SOUTH);

	private static final VoxelShape
			CROSS = VoxelShapes.combineAndSimplify(
					block,
					VoxelShapes.or(
							opening.get(Direction.SOUTH),
							opening.get(Direction.NORTH),
							opening.get(Direction.WEST),
							opening.get(Direction.EAST)
					),
					IBooleanFunction.NOT_SAME);


	public static VoxelShape getShape(BlockState state) {
		BeltTunnelBlock.Shape shape = state.get(BeltTunnelBlock.SHAPE);
		Direction.Axis axis = state.get(BeltTunnelBlock.HORIZONTAL_AXIS);

		if (shape == BeltTunnelBlock.Shape.CROSS)
			return CROSS;

		if (shape == BeltTunnelBlock.Shape.STRAIGHT || shape == BeltTunnelBlock.Shape.WINDOW)
			return STRAIGHT.get(axis);

		if (shape == BeltTunnelBlock.Shape.T_LEFT)
			return TEE.get(axis == Direction.Axis.Z ? Direction.EAST : Direction.NORTH);

		if (shape == BeltTunnelBlock.Shape.T_RIGHT)
			return TEE.get(axis == Direction.Axis.Z ? Direction.WEST : Direction.SOUTH);

		//something went wrong
		return VoxelShapes.fullCube();
	}
}
