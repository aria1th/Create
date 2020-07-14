package com.simibubi.create.content.contraptions.processing;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.foundation.block.ITE;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeaterBlock extends Block implements ITE<HeaterTileEntity> {

	public static IProperty<Integer> BLAZE_LEVEL = IntegerProperty.create("blaze_level", 0, 4);

	public HeaterBlock(Properties properties) {
		super(properties);
		setDefaultState(super.getDefaultState().with(BLAZE_LEVEL, 0));
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		super.fillStateContainer(builder);
		builder.add(BLAZE_LEVEL);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return state.get(BLAZE_LEVEL) >= 1;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return AllTileEntities.HEATER.create();
	}

	@Override
	public Class<HeaterTileEntity> getTileEntityClass() {
		return HeaterTileEntity.class;
	}

	@Override
	public ActionResultType onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockRayTraceResult blockRayTraceResult) {
		if (!hasTileEntity(state))
			return ActionResultType.PASS;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof HeaterTileEntity && ((HeaterTileEntity) te).tryUpdateFuel(player.getHeldItem(hand), player)) {
			if (!player.isCreative())
				player.getHeldItem(hand)
					.shrink(1);
			return ActionResultType.SUCCESS;
		}
		return ActionResultType.PASS;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		ItemStack item = context.getItem();
		BlockState state = super.getStateForPlacement(context);
		return (state != null ? state : getDefaultState()).with(BLAZE_LEVEL,
				(item.hasTag() && item.getTag() != null && item.getTag()
				.contains("has_blaze") && item.getTag()
					.getBoolean("has_blaze")) ? 1 : 0);
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
		return AllShapes.HEATER_BLOCK_SHAPE;
	}

	@Override
	public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
		return MathHelper.clamp(state.get(BLAZE_LEVEL) * 4 - 1, 0, 15);
	}

	static void setBlazeLevel(@Nullable World world, BlockPos pos, int blazeLevel) {
		if (world != null)
			world.setBlockState(pos, world.getBlockState(pos).with(BLAZE_LEVEL, blazeLevel));
	}

	public static int getHeaterLevel(BlockState blockState) {
		return blockState.has(HeaterBlock.BLAZE_LEVEL) ? blockState.get(HeaterBlock.BLAZE_LEVEL) : 0;
	}
}
