package com.simibubi.create.content.contraptions.components.actors;

import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.ProperDirectionalBlock;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;


import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableStorageInterfaceBlock extends ProperDirectionalBlock {

	public PortableStorageInterfaceBlock(Properties p_i48415_1_) {
		super(p_i48415_1_);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return getDefaultState().with(FACING, context.getNearestLookingDirection()
			.getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return AllShapes.PORTABLE_STORAGE_INTERFACE.get(state.get(FACING));
	}

}
