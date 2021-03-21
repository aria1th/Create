package com.simibubi.create.content.curiosities.tools;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.TreeCutter;
import com.simibubi.create.foundation.utility.TreeCutter.Tree;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class DeforesterItem extends AxeItem {

	public DeforesterItem(Settings builder) {
		super(AllToolTiers.RADIANT, 5.0F, -3.1F, builder);
	}

	// Moved away from Item#onBlockDestroyed as it does not get called in Creative
	public static void destroyTree(ItemStack stack, WorldAccess iWorld, BlockState state, BlockPos pos,
			PlayerEntity player) {
		if (!state.isIn(BlockTags.LOGS) || player.isSneaking() || !(iWorld instanceof World))
			return;
		World worldIn = (World) iWorld;
		Tree tree = TreeCutter.cutTree(worldIn, pos);
		if (tree == null)
			return;
		boolean dropBlock = !player.isCreative();

		Vec3d vec = player.getRotationVector();
		for (BlockPos log : tree.logs)
			BlockHelper.destroyBlock(worldIn, log, 1 / 2f, item -> {
				if (dropBlock) {
					dropItemFromCutTree(worldIn, pos, vec, log, item);
					stack.damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
				}
			});
		for (BlockPos leaf : tree.leaves)
			BlockHelper.destroyBlock(worldIn, leaf, 1 / 8f, item -> {
				if (dropBlock)
					dropItemFromCutTree(worldIn, pos, vec, leaf, item);
			});
	}

	public static void onBlockDestroyed(World world, PlayerEntity playerEntity, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
		ItemStack heldItemMainhand = playerEntity.getMainHandStack();
		if (!heldItemMainhand.isItemEqualIgnoreDamage(AllItems.DEFORESTER.getDefaultStack()))
			return;
		destroyTree(heldItemMainhand, world, blockState, blockPos, playerEntity);
	}

	public static void dropItemFromCutTree(World world, BlockPos breakingPos, Vec3d fallDirection, BlockPos pos,
			ItemStack stack) {
		float distance = (float) Math.sqrt(pos.getSquaredDistance(breakingPos));
		Vec3d dropPos = VecHelper.getCenterOf(pos);
		ItemEntity entity = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack);
		entity.setVelocity(fallDirection.multiply(distance / 20f));
		world.spawnEntity(entity);
	}

}
