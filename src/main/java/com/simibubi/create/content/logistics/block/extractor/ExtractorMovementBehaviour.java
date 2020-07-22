package com.simibubi.create.content.logistics.block.extractor;

import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.logistics.block.AttachedLogisticalBlock;
import com.simibubi.create.content.logistics.item.filter.FilterItem;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;

public class ExtractorMovementBehaviour extends MovementBehaviour {

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		super.visitNewPosition(context, pos);

		World world = context.world;
		VoxelShape collisionShape = world.getBlockState(pos).getCollisionShape(world, pos);
		if (!collisionShape.isEmpty())
			return;
		if (!world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(pos)).isEmpty())
			return;

		ItemStack filter = getFilter(context);
		int amount = getFilterAmount(context);
		ItemStack dropped = ItemHelper.extract(context.contraption.inventory,
				stack -> FilterItem.test(context.world, stack, filter), amount == 0 ? 64 : amount, false);

		if (dropped.isEmpty())
			return;
		if (world.isRemote)
			return;
		
		Vector3d entityPos = context.position;
		Entity entityIn = null;
		Direction facing = AttachedLogisticalBlock.getBlockFacing(context.state);
		if (facing != Direction.DOWN)
			entityPos = entityPos.add(0, -0.5f, 0);

		entityIn = new ItemEntity(world, entityPos.x, entityPos.y, entityPos.z, dropped);
		entityIn.setMotion(Vector3d.ZERO);
		((ItemEntity) entityIn).setPickupDelay(5);
		world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1/16f, .1f);
		world.addEntity(entityIn);
	}

	private ItemStack getFilter(MovementContext context) {
		return ItemStack.read(context.tileData.getCompound("Filter"));
	}

	private int getFilterAmount(MovementContext context) {
		return context.tileData.getInt("FilterAmount");
	}

}
