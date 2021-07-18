package com.simibubi.create.content.logistics.block.redstone;

import java.util.Random;

import com.simibubi.create.AllTileEntities;
import com.simibubi.create.foundation.block.ITE;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFaceBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AnalogLeverBlock extends HorizontalFaceBlock implements ITE<AnalogLeverTileEntity> {

	public AnalogLeverBlock(Properties p_i48402_1_) {
		super(p_i48402_1_);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return AllTileEntities.ANALOG_LEVER.create();
	}

	@Override
	public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
								  BlockRayTraceResult hit) {
		if (worldIn.isClientSide) {
			addParticles(state, worldIn, pos, 1.0F);
			return ActionResultType.SUCCESS;
		}

		return onTileEntityUse(worldIn, pos, te -> {
			boolean sneak = player.isShiftKeyDown();
			te.changeState(sneak);
			float f = .25f + ((te.state + 5) / 15f) * .5f;
			worldIn.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundCategory.BLOCKS, 0.2F, f);
			return ActionResultType.SUCCESS;
		});
	}

	@Override
	public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
		return getTileEntityOptional(blockAccess, pos).map(al -> al.state)
				.orElse(0);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getDirectSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
		return getConnectedDirection(blockState) == side ? getSignal(blockState, blockAccess, pos, side) : 0;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		withTileEntityDo(worldIn, pos, te -> {
			if (te.state != 0 && rand.nextFloat() < 0.25F)
				addParticles(stateIn, worldIn, pos, 0.5F);
		});
	}

	@Override
	public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (isMoving || state.getBlock() == newState.getBlock())
			return;
		withTileEntityDo(worldIn, pos, te -> {
			if (te.state != 0)
				updateNeighbors(state, worldIn, pos);
			worldIn.removeBlockEntity(pos);
		});
	}

	private static void addParticles(BlockState state, IWorld worldIn, BlockPos pos, float alpha) {
		Direction direction = state.getValue(FACING)
				.getOpposite();
		Direction direction1 = getConnectedDirection(state).getOpposite();
		double d0 = (double) pos.getX() + 0.5D + 0.1D * (double) direction.getStepX()
				+ 0.2D * (double) direction1.getStepX();
		double d1 = (double) pos.getY() + 0.5D + 0.1D * (double) direction.getStepY()
				+ 0.2D * (double) direction1.getStepY();
		double d2 = (double) pos.getZ() + 0.5D + 0.1D * (double) direction.getStepZ()
				+ 0.2D * (double) direction1.getStepZ();
		worldIn.addParticle(new RedstoneParticleData(1.0F, 0.0F, 0.0F, alpha), d0, d1, d2, 0.0D, 0.0D, 0.0D);
	}

	static void updateNeighbors(BlockState state, World world, BlockPos pos) {
		world.updateNeighborsAt(pos, state.getBlock());
		world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return Blocks.LEVER.getShape(state, worldIn, pos, context);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, FACE));
	}

	@Override
	public Class<AnalogLeverTileEntity> getTileEntityClass() {
		return AnalogLeverTileEntity.class;
	}

	@Override
	public boolean isPathfindable(BlockState state, IBlockReader reader, BlockPos pos, PathType type) {
		return false;
	}

}
