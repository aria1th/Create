package com.simibubi.create.content.contraptions.components.structureMovement;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.components.structureMovement.render.RenderedContraption;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class MovementBehaviour {

	public boolean isActive(MovementContext context) {
		return true;
	}

	public void tick(MovementContext context) {}

	public void startMoving(MovementContext context) {}

	public void visitNewPosition(MovementContext context, BlockPos pos) {}

	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.ZERO;
	}

	public void dropItem(MovementContext context, ItemStack stack) {
		ItemStack remainder = ItemHandlerHelper.insertItem(context.contraption.inventory, stack, false);
		if (remainder.isEmpty())
			return;

		Vec3d vec = context.position;
		ItemEntity itemEntity = new ItemEntity(context.world, vec.x, vec.y, vec.z, remainder);
		itemEntity.setMotion(context.motion.add(0, 0.5f, 0)
			.scale(context.world.rand.nextFloat() * .3f));
		context.world.addEntity(itemEntity);
	}

	public void stopMoving(MovementContext context) {

	}

	public void writeExtraData(MovementContext context) {

	}

	public boolean renderAsNormalTileEntity() {
		return false;
	}

	public boolean hasSpecialInstancedRendering() {
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public void renderInContraption(MovementContext context, MatrixStack ms, MatrixStack msLocal,
		IRenderTypeBuffer buffer) {}

	@OnlyIn(Dist.CLIENT)
	public void addInstance(RenderedContraption contraption, MovementContext context) {}

	public void onSpeedChanged(MovementContext context, Vec3d oldMotion, Vec3d motion) {

	}
}
