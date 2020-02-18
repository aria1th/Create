package com.simibubi.create.modules.contraptions.components.contraptions.chassis;

import java.util.List;

import com.simibubi.create.AllSoundEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

public abstract class AbstractChassisBlock extends RotatedPillarBlock {

	public AbstractChassisBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new ChassisTileEntity();
	}

	@Override
	public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
			BlockRayTraceResult hit) {
		if (!player.isAllowEdit())
			return false;

		BooleanProperty affectedSide = getGlueableSide(state, hit.getFace());
		if (affectedSide == null)
			return false;

		ItemStack heldItem = player.getHeldItem(handIn);
		boolean isSlimeBall = heldItem.getItem().isIn(Tags.Items.SLIMEBALLS);

		if ((!heldItem.isEmpty() || !player.isSneaking()) && !isSlimeBall)
			return false;
		if (state.get(affectedSide) == isSlimeBall)
			return false;
		if (worldIn.isRemote) {
			Vec3d vec = hit.getHitVec();
			worldIn.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0, 0, 0);
			return true;
		}

		worldIn.playSound(null, pos, AllSoundEvents.SLIME_ADDED.get(), SoundCategory.BLOCKS, .5f, 1);
		if (isSlimeBall && !player.isCreative())
			heldItem.shrink(1);
		if (!isSlimeBall && !player.isCreative())
			Block.spawnAsEntity(worldIn, pos.offset(hit.getFace()), new ItemStack(Items.SLIME_BALL));

		worldIn.setBlockState(pos, state.with(affectedSide, isSlimeBall));
		return true;
	}

	public abstract BooleanProperty getGlueableSide(BlockState state, Direction face);

	@Override
	public List<ItemStack> getDrops(BlockState state, net.minecraft.world.storage.loot.LootContext.Builder builder) {
		@SuppressWarnings("deprecation")
		List<ItemStack> drops = super.getDrops(state, builder);
		for (Direction face : Direction.values()) {
			BooleanProperty glueableSide = getGlueableSide(state, face);
			if (glueableSide != null && state.get(glueableSide))
				drops.add(new ItemStack(Items.SLIME_BALL));
		}
		return drops;
	}

}
