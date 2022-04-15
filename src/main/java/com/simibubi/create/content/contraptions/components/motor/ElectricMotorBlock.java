package com.simibubi.create.content.contraptions.components.motor;

import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Referenced from createaddition ElectricMotorBlock
 * (https://github.com/mrh0/createaddition/blob/fabric-1.18/src/main/java/com/mrh0/createaddition/blocks/electric_motor/ElectricMotorBlock.java)
 */
public class ElectricMotorBlock extends DirectionalKineticBlock implements ITE<ElectricMotorTileEntity> {
	public static final VoxelShaper ELECTRIC_MOTOR_SHAPE;
	static {
		ELECTRIC_MOTOR_SHAPE = VoxelShaper.forDirectional(Shapes.or(
				Block.box(0, 5, 0, 16, 11, 16), Block.box(3, 0, 3, 13, 14, 13)
		), Direction.UP);
	}

	public ElectricMotorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return ELECTRIC_MOTOR_SHAPE.get(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferred = getPreferredFacing(context);
		if ((context.getPlayer() != null && context.getPlayer()
				.isShiftKeyDown()) || preferred == null)
			return super.getStateForPlacement(context);
		return defaultBlockState().setValue(FACING, preferred);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return AllTileEntities.ELECTRIC_MOTOR.create(pos, state);
	}

	@Override
	public Class<ElectricMotorTileEntity> getTileEntityClass() {
		return ElectricMotorTileEntity.class;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING)
				.getAxis();
	}

	@Override
	public boolean hideStressImpact() {
		return true;
	}

	@Override
	public BlockEntityType<? extends ElectricMotorTileEntity> getTileEntityType() {
		return AllTileEntities.ELECTRIC_MOTOR.get();
	}
}
