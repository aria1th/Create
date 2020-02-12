package com.simibubi.create.foundation.utility;

import static net.minecraft.block.Block.makeCuboidShape;

import java.util.Arrays;

import net.minecraft.block.Blocks;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

public class AllShapes {

	public static final VoxelShaper
		SHORT_CASING_14_VOXEL = VoxelShaper.forDirectional(makeCuboidShape(0, 0, 0, 16, 14, 16), Direction.UP),
		SHORT_CASING_12_VOXEL = VoxelShaper.forDirectional(makeCuboidShape(0, 0, 0, 16, 12, 16), Direction.UP),
		SHORT_CASING_11_VOXEL = VoxelShaper.forDirectional(makeCuboidShape(0, 0, 0, 16, 11, 16), Direction.UP),
		HARVESTER_BASE = VoxelShaper.forHorizontal(makeCuboidShape(0, 2, 0, 16, 14, 3), Direction.SOUTH),
		MOTOR_BLOCK = VoxelShaper.forHorizontal(makeCuboidShape(0, 3, 3, 16, 13, 13), Direction.EAST),
		FOUR_VOXEL_POLE = VoxelShaper.forDirectionalAxis(makeCuboidShape(6, 0, 6, 10, 16, 10), Direction.Axis.Y),
		SIX_VOXEL_POLE = VoxelShaper.forDirectionalAxis(makeCuboidShape(5, 0, 5, 11, 16, 11), Direction.Axis.Y),
		BELT_FUNNEL = VoxelShaper.forHorizontal(makeCuboidShape(3, -4, 11, 13, 8, 17), Direction.SOUTH),
		FUNNEL = VoxelShaper.forDirectional(makeCuboidShape(1, 1, 13, 15, 15, 17), Direction.SOUTH),
		EXTRACTOR = VoxelShaper.forDirectional(makeCuboidShape(4, 2, 11, 12, 10, 17), Direction.SOUTH)
				.withVerticalShapes(makeCuboidShape(4, 11, 4, 12, 17, 12)),
		TRANSPOSER = VoxelShaper.forDirectional(VoxelShapes.or(
				makeCuboidShape(4, 4, -1, 12, 12, 1), 
				makeCuboidShape(5, 5, 0, 11, 11, 16), 
				makeCuboidShape(4, 4, 11, 12, 12, 17)), Direction.SOUTH),
		FURNACE_ENGINE = VoxelShaper.forHorizontal(VoxelShapes.or(
				makeCuboidShape(1, 1, 0, 15, 15, 16), 
				makeCuboidShape(0, 0, 9, 16, 16, 14)), Direction.SOUTH),
		PORTABLE_STORAGE_INTERFACE = VoxelShaper.forDirectional(VoxelShapes.or(
				makeCuboidShape(0, 0, 0, 16, 12, 16), 
				makeCuboidShape(3, 12, 3, 13, 16, 13)), Direction.UP)

				;


	private static final VoxelShape
		LOGISTICAL_CASING_MIDDLE_SHAPE = VoxelShapes.or(
		makeCuboidShape(1,0,1,15,16,15),
		makeCuboidShape(0,0,0,2,16,2),
		makeCuboidShape(14,0,0,16,16,2),
		makeCuboidShape(0,0,14,2,16,16),
		makeCuboidShape(14,0,14,16,16,16)),
		LOGISTICAL_CASING_CAP_SHAPE = VoxelShapes.or(
				LOGISTICAL_CASING_MIDDLE_SHAPE,
				makeCuboidShape(0,0,0,16,2,16)),
		CART_ASSEMBLER_SHAPE = VoxelShapes.or(
				VoxelShapes.fullCube(),
				makeCuboidShape(-2, 0, 1, 18, 13, 15)),
		MECHANICAL_PISTON_HEAD_SHAPE_UP = Blocks.PISTON_HEAD.getStateContainer().getBaseState().with(DirectionalBlock.FACING, Direction.UP).with(PistonHeadBlock.SHORT, true).getShape(null, null),
		MECHANICAL_PISTON_EXTENDED_SHAPE_UP = VoxelShapes.or(
				SHORT_CASING_12_VOXEL.get(Direction.UP),
				FOUR_VOXEL_POLE.get(Direction.Axis.Y)),
		SMALL_GEAR_SHAPE =  makeCuboidShape(2, 6, 2, 14, 10, 14),
		LARGE_GEAR_SHAPE =  makeCuboidShape(0, 6, 0, 16, 10, 16),
		VERTICAL_TABLET_SHAPE_SOUTH =  makeCuboidShape(3, 1, -1, 13, 15, 3),
		SQUARE_TABLET_SHAPE_SOUTH = makeCuboidShape(2, 2, -1, 14, 14, 3),
		PACKAGE_FUNNEL_SHAPE_UP = makeCuboidShape(1, -1, 1, 15, 3, 15),
		TABLE_POLE_SHAPE = VoxelShapes.or(
				makeCuboidShape(4, 0, 4, 12, 2, 12),
				makeCuboidShape(5, 2, 5, 11, 14, 11)),
		LOGISTICS_TABLE_SLOPE_SOUTH = VoxelShapes.or(
				makeCuboidShape(0, 10D, 15, 16, 14, 10.667),
				makeCuboidShape(0, 12, 10.667, 16, 16, 6.333),
				makeCuboidShape(0, 14, 6.333, 16, 18, 2)),
		SCHEMATICS_TABLE_SLOPE_SOUTH = VoxelShapes.or(
				makeCuboidShape(0, 10, 16, 16, 14, 11),
				makeCuboidShape(0, 12, 11, 16, 16, 6),
				makeCuboidShape(0, 14, 6, 16, 18, 1)),
		GAUGE_SHAPE_UP = VoxelShapes.or(
				makeCuboidShape(1, 0, 0, 15, 2, 16),//plate
				makeCuboidShape(2, 2, 1, 14, 14, 15)),//log
		NOZZLE_SHAPE_UP = VoxelShapes.or(
				makeCuboidShape(2, 0, 2, 14, 14, 14),
				VoxelShapes.combine(
						makeCuboidShape(1, 13, 1, 15, 15, 15), 
						makeCuboidShape(3, 13, 3, 13, 15, 13), 
						IBooleanFunction.ONLY_FIRST)),
		CRANK_SHAPE_UP = VoxelShapes.or(
				makeCuboidShape(5, 0, 5, 11, 6, 11),
				makeCuboidShape(1, 3, 1, 15, 8, 15)
				)

				;

	public static final VoxelShape
		LOGISTICAL_CASING_SINGLE_SHAPE = VoxelShapes.or(
				makeCuboidShape(0, 0, 0, 16, 2, 16),
				makeCuboidShape(1, 1, 1, 15, 15, 15),
				makeCuboidShape(0, 14, 0, 16, 16, 16)),
		BASIN_BLOCK_SHAPE = VoxelShapes.or(
				makeCuboidShape(2, 0, 2, 14, 2, 14), 
				VoxelShapes.combine(
						makeCuboidShape(0, 2, 0, 16, 13, 16),
						makeCuboidShape(2, 5, 2, 14, 14, 14),
						IBooleanFunction.ONLY_FIRST)),
		CRUSHING_WHEEL_COLLISION_SHAPE = makeCuboidShape(0, 0, 0, 16, 22, 16),
		MECHANICAL_PROCESSOR_SHAPE = VoxelShapes.combineAndSimplify(
				VoxelShapes.fullCube(),
				makeCuboidShape(4, 0, 4, 12, 16, 12),
				IBooleanFunction.ONLY_FIRST),
		TURNTABLE_SHAPE = VoxelShapes.or(
				makeCuboidShape(1, 4, 1, 15, 8, 15),
				makeCuboidShape(5, 0, 5, 11, 4, 11)),
		CRATE_BLOCK_SHAPE = makeCuboidShape(1, 0, 1, 15, 14, 15),
		LOGISTICS_TABLE_BASE = TABLE_POLE_SHAPE,
		BELT_COLLISION_MASK = makeCuboidShape(0, 0, 0, 16, 19, 16),
		SCHEMATICANNON_SHAPE = VoxelShapes.or(
				makeCuboidShape(1, 0, 1, 15, 8, 15),
				makeCuboidShape(0.5, 8, 0.5, 15.5, 11, 15.5))

				;




	public static final VoxelShaper
		LOGISTICAL_CASING_MIDDLE = VoxelShaper.forDirectional(LOGISTICAL_CASING_MIDDLE_SHAPE, Direction.UP),
		LOGISTICAL_CASING_CAP = VoxelShaper.forDirectional(LOGISTICAL_CASING_CAP_SHAPE, Direction.UP),
		NOZZLE = VoxelShaper.forDirectional(NOZZLE_SHAPE_UP, Direction.UP),
		CRANK = VoxelShaper.forDirectional(CRANK_SHAPE_UP, Direction.UP),
		CART_ASSEMBLER = VoxelShaper.forHorizontalAxis(CART_ASSEMBLER_SHAPE, Direction.SOUTH),
		MECHANICAL_PISTON_HEAD = VoxelShaper.forDirectional(MECHANICAL_PISTON_HEAD_SHAPE_UP, Direction.UP),
		MECHANICAL_PISTON = SHORT_CASING_12_VOXEL,
		MECHANICAL_PISTON_EXTENDED = VoxelShaper.forDirectional(MECHANICAL_PISTON_EXTENDED_SHAPE_UP, Direction.UP),
		SMALL_GEAR = VoxelShaper.forDirectionalAxis(VoxelShapes.or(SMALL_GEAR_SHAPE, SIX_VOXEL_POLE.get(Direction.Axis.Y)), Direction.Axis.Y),
		LARGE_GEAR = VoxelShaper.forDirectionalAxis(VoxelShapes.or(LARGE_GEAR_SHAPE, SIX_VOXEL_POLE.get(Direction.Axis.Y)), Direction.Axis.Y),
		LOGISTICAL_CONTROLLER = VoxelShaper.forDirectional(SQUARE_TABLET_SHAPE_SOUTH, Direction.SOUTH),
		REDSTONE_BRIDGE = VoxelShaper.forHorizontal(VERTICAL_TABLET_SHAPE_SOUTH, Direction.SOUTH).withVerticalShapes(LOGISTICAL_CONTROLLER.get(Direction.UP)),
		LOGISTICAL_INDEX = REDSTONE_BRIDGE,
		PACKAGE_FUNNEL = VoxelShaper.forDirectional(PACKAGE_FUNNEL_SHAPE_UP, Direction.UP),
		LOGISTICS_TABLE = VoxelShaper.forHorizontal(VoxelShapes.or(TABLE_POLE_SHAPE, LOGISTICS_TABLE_SLOPE_SOUTH), Direction.SOUTH),
		SCHEMATICS_TABLE = VoxelShaper.forDirectional(VoxelShapes.or(TABLE_POLE_SHAPE, SCHEMATICS_TABLE_SLOPE_SOUTH), Direction.SOUTH)

				;

	public static final GaugeShaper GAUGE = GaugeShaper.make();


	public static class GaugeShaper extends VoxelShaper {

		private VoxelShaper axisFalse, axisTrue;

		private static GaugeShaper make(){
			GaugeShaper shaper = new GaugeShaper();
			shaper.axisFalse = forDirectional(AllShapes.GAUGE_SHAPE_UP, Direction.UP);
			shaper.axisTrue = forDirectional(rotatedCopy(AllShapes.GAUGE_SHAPE_UP, new Vec3d(0, 90, 0)), Direction.UP);
			//shapes for X axis need to be swapped
			Arrays.asList(Direction.EAST, Direction.WEST).forEach(direction -> {
				VoxelShape mem = shaper.axisFalse.get(direction);
				shaper.axisFalse.withShape(shaper.axisTrue.get(direction), direction);
				shaper.axisTrue.withShape(mem, direction);
			});
			return shaper;
		}

		public VoxelShape get(Direction direction, boolean axisAlong) {
			return (axisAlong ? axisTrue : axisFalse).get(direction);
		}
	}
}
