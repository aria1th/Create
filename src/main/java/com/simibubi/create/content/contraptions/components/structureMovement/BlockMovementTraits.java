package com.simibubi.create.content.contraptions.components.structureMovement;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.components.crank.HandCrankBlock;
import com.simibubi.create.content.contraptions.components.fan.NozzleBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingBlockEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyBlockEntity;
import com.simibubi.create.content.logistics.block.redstone.RedstoneLinkBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

// TODO FIX WHOLE CLASS WITH CHECKS
public class BlockMovementTraits {

	public static boolean movementNecessary(BlockState state, World world, BlockPos pos) {
		if (isBrittle(state))
			return true;
		if (state.getBlock() instanceof FenceGateBlock)
			return true;
		if (state.getMaterial()
			.isReplaceable())
			return false;
		return !state.getCollisionShape(world, pos)
			.isEmpty();
	}

	public static boolean movementAllowed(BlockState state, World world, BlockPos pos) {
		Block block = state.getBlock();
		if (block instanceof AbstractChassisBlock)
		 	return true;
		if (state.getHardness(world, pos) == -1)
			return false;
		/*if (AllTags.AllBlockTags.NON_MOVABLE.matches(state))
			return false;*/

		// Move controllers only when they aren't moving
		/*if (block instanceof MechanicalPistonBlock && state.get(MechanicalPistonBlock.STATE) != MechanicalPistonBlock.PistonState.MOVING)
	 		return true;*/
	 	if (block instanceof MechanicalBearingBlock) {
	 		BlockEntity te = world.getBlockEntity(pos);
	 		if (te instanceof MechanicalBearingBlockEntity)
	 		return !((MechanicalBearingBlockEntity) te).isRunning();
	 	}
	 	if (block instanceof ClockworkBearingBlock) {
	 		BlockEntity te = world.getBlockEntity(pos);
	 		if (te instanceof ClockworkBearingBlockEntity)
	 		return !((ClockworkBearingBlockEntity) te).isRunning();
	 	}
	 	if (block instanceof PulleyBlock) {
	 		BlockEntity te = world.getBlockEntity(pos);
	 		if (te instanceof PulleyBlockEntity)
	 		return !((PulleyBlockEntity) te).running;
	 	}

		if (AllBlocks.BELT.hasBlockEntity())
			return true;
		if (state.getBlock() instanceof GrindstoneBlock)
			return true;
		return state.getPistonBehavior() != PistonBehavior.BLOCK;
	}

	/**
	 * Brittle blocks will be collected first, as they may break when other blocks
	 * are removed before them
	 */
	public static boolean isBrittle(BlockState state) {
		Block block = state.getBlock();
		if (state.contains(Properties.HANGING))
			return true;
		if (block instanceof LadderBlock)
			return true;
		if (block instanceof TorchBlock)
			return true;
		if (block instanceof AbstractPressurePlateBlock)
			return true;
		if (block instanceof WallMountedBlock && !(block instanceof GrindstoneBlock))
			return true;
		if (block instanceof CartAssemblerBlock)
			return false;
		if (block instanceof AbstractRailBlock)
			return true;
		if (block instanceof AbstractRedstoneGateBlock)
			return true;
		if (block instanceof RedstoneWireBlock)
			return true;
		if (block instanceof CarpetBlock)
			return true;
//		return AllTags.AllBlockTags.BRITTLE.tag.contains(block);
		return false;
	}

	/**
	 * Attached blocks will move if blocks they are attached to are moved
	 */
	public static boolean isBlockAttachedTowards(World world, BlockPos pos, BlockState state,
												 Direction direction) {
		Block block = state.getBlock();
		if (block instanceof LadderBlock)
			return state.get(LadderBlock.FACING) == direction.getOpposite();
		if (block instanceof WallTorchBlock)
			return state.get(WallTorchBlock.FACING) == direction.getOpposite();
		if (block instanceof AbstractPressurePlateBlock)
			return direction == Direction.DOWN;
		if (block instanceof DoorBlock)
			return direction == Direction.DOWN;
		if (block instanceof RedstoneLinkBlock)
			return direction.getOpposite() == state.get(RedstoneLinkBlock.FACING);
		if (block instanceof FlowerPotBlock)
			return direction == Direction.DOWN;
		if (block instanceof AbstractRedstoneGateBlock)
			return direction == Direction.DOWN;
		if (block instanceof RedstoneWireBlock)
			return direction == Direction.DOWN;
		if (block instanceof CarpetBlock)
			return direction == Direction.DOWN;
		if (block instanceof WallRedstoneTorchBlock)
			return state.get(WallRedstoneTorchBlock.FACING) == direction.getOpposite();
		if (block instanceof TorchBlock)
			return direction == Direction.DOWN;
		if (block instanceof WallMountedBlock) {
			WallMountLocation attachFace = state.get(WallMountedBlock.FACE);
			 if (attachFace == WallMountLocation.CEILING)
				return direction == Direction.UP;
			 if (attachFace == WallMountLocation.FLOOR)
				return direction == Direction.DOWN;
			 if (attachFace == WallMountLocation.WALL)
				return direction.getOpposite() == state.get(WallMountedBlock.FACING);
		}
		if (state.contains(Properties.HANGING))
			return direction == (state.get(Properties.HANGING) ? Direction.UP : Direction.DOWN);
		if (block instanceof AbstractRailBlock)
			return direction == Direction.DOWN;
		/*if (block instanceof AttachedActorBlock)
			return direction == state.get(HarvesterBlock.HORIZONTAL_FACING).getOpposite();*/
		if (block instanceof HandCrankBlock)
			return direction == state.get(HandCrankBlock.FACING).getOpposite();
		if (block instanceof NozzleBlock)
			return direction == state.get(NozzleBlock.FACING).getOpposite();
		/*if (block instanceof EngineBlock)
			return direction == state.get(EngineBlock.HORIZONTAL_FACING).getOpposite();*/
		if (block instanceof BellBlock) {
			Attachment attachment = state.get(Properties.ATTACHMENT);
			if (attachment == Attachment.FLOOR)
				return direction == Direction.DOWN;
			if (attachment == Attachment.CEILING)
				return direction == Direction.UP;
			return direction == state.get(HorizontalFacingBlock.FACING);
		}
		/*if (state.getBlock() instanceof SailBlock)
			return direction.getAxis() != state.get(SailBlock.FACING).getAxis();
		if (state.getBlock() instanceof FluidTankBlock)
			return FluidTankConnectivityHandler.isConnected(world, pos, pos.offset(direction));*/
		return false;
	}

	/**
	 * Non-Supportive blocks will not continue a chain of blocks picked up by e.g. a
	 * piston
	 */
	public static boolean notSupportive(BlockState state, Direction facing) {
		/*if (AllBlocks.MECHANICAL_DRILL.has(state))
			return state.get(BlockStateProperties.FACING) == facing;*/
	 	if (AllBlocks.MECHANICAL_BEARING.getStateManager().getStates().contains(state))
		 	return state.get(Properties.FACING) == facing;
	 	if (AllBlocks.CART_ASSEMBLER.getStateManager().getStates().contains(state))
		 	return Direction.DOWN == facing;
	 	/*if (AllBlocks.MECHANICAL_SAW.has(state))
	 		return state.get(BlockStateProperties.FACING) == facing;
	 	if (AllBlocks.PORTABLE_STORAGE_INTERFACE.has(state))
	 		return state.get(PortableStorageInterfaceBlock.FACING) == facing;
		if (state.getBlock() instanceof AttachedActorBlock)
			return state.get(BlockStateProperties.HORIZONTAL_FACING) == facing;*/
	 	if (AllBlocks.ROPE_PULLEY.getStateManager().getStates().contains(state))
	 		return facing == Direction.DOWN;
		if (state.getBlock() instanceof CarpetBlock)
			return facing == Direction.UP;
		/*if (state.getBlock() instanceof SailBlock)
		 return facing.getAxis() == state.get(SailBlock.FACING)
		 .getAxis();
		 if (AllBlocks.PISTON_EXTENSION_POLE.has(state))
		 return facing.getAxis() != state.get(BlockStateProperties.FACING)
		 .getAxis();
		 if (AllBlocks.MECHANICAL_PISTON_HEAD.has(state))
		 return facing.getAxis() != state.get(BlockStateProperties.FACING)
		 .getAxis();*/
		return isBrittle(state);
	}

}
