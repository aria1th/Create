package com.simibubi.create.modules.logistics.block.inventories;

import com.simibubi.create.foundation.block.ITE;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class CreativeCrateBlock extends CrateBlock implements ITE<CreativeCrateTileEntity> {

	public CreativeCrateBlock(Properties p_i48415_1_) {
		super(p_i48415_1_);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new CreativeCrateTileEntity();
	}
	
	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		withTileEntityDo(worldIn, pos, CreativeCrateTileEntity::onPlaced);
	}

	@Override
	public Class<CreativeCrateTileEntity> getTileEntityClass() {
		return CreativeCrateTileEntity.class;
	}
}
