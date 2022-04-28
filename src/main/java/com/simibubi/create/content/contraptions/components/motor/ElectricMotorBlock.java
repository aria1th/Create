package com.simibubi.create.content.contraptions.components.motor;

import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.content.contraptions.relays.encased.AdjustablePulleyTileEntity;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;

/**
 * Referenced from createaddition ElectricMotorBlock
 * (https://github.com/mrh0/createaddition/blob/fabric-1.18/src/main/java/com/mrh0/createaddition/blocks/electric_motor/ElectricMotorBlock.java)
 */
public class ElectricMotorBlock extends DirectionalKineticBlock implements ITE<ElectricMotorTileEntity> {
	public ElectricMotorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, worldIn, pos, oldState, isMoving);
		if (oldState.getBlock() == state.getBlock())
			return;
		int capacity = (worldIn.getBestNeighborSignal(pos) + 1) * 4;
		withTileEntityDo(worldIn, pos, te -> { te.setCapacity(capacity);});
	}

	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.mayBuild())
			return InteractionResult.FAIL;
		if (player.isShiftKeyDown())
			return InteractionResult.FAIL;
		if (player.getItemInHand(hand).isEmpty()) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			withTileEntityDo(level, pos, te -> {
				if (te.isActive()) {
					te.deactivate();
				}
				else if (te.canActivate()) {
					te.activate();
				}
			});
			return InteractionResult.SUCCESS;
		}
		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		if (level.isClientSide) return;

		int capacity = (level.getBestNeighborSignal(pos) + 1) * 4;
		withTileEntityDo(level, pos, te -> { te.setCapacity(capacity); });
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.MOTOR_BLOCK.get(state.getValue(FACING));
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
