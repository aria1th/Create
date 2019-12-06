package com.simibubi.create.modules.contraptions.receivers.constructs.mounted;

import com.mojang.blaze3d.platform.GlStateManager;
import com.simibubi.create.foundation.utility.TessellatorHelper;
import com.simibubi.create.modules.contraptions.receivers.constructs.ContraptionRenderer;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ContraptionEntityRenderer extends EntityRenderer<ContraptionEntity> {

	public ContraptionEntityRenderer(EntityRendererManager rendererManager) {
		super(rendererManager);
	}

	@Override
	protected ResourceLocation getEntityTexture(ContraptionEntity arg0) {
		return null;
	}

	@Override
	public void doRender(ContraptionEntity entity, double x, double y, double z, float yaw, float partialTicks) {
		if (!entity.isAlive())
			return;
		if (entity.getContraption() == null)
			return;

		GlStateManager.pushMatrix();
		float angleYaw = (float) (entity.getYaw(partialTicks) / 180 * Math.PI);
		float anglePitch = (float) (entity.getPitch(partialTicks) / 180 * Math.PI);

		Entity ridingEntity = entity.getRidingEntity();
		if (ridingEntity != null && ridingEntity instanceof AbstractMinecartEntity) {
			AbstractMinecartEntity cart = (AbstractMinecartEntity) ridingEntity;
			GlStateManager.translated(0, .5, 0);

			long i = (long) entity.getEntityId() * 493286711L;
			i = i * i * 4392167121L + i * 98761L;
			float f = (((float) (i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
			float f1 = (((float) (i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
			float f2 = (((float) (i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
			GlStateManager.translatef(f, f1, f2);

			double cartX = MathHelper.lerp((double) partialTicks, cart.lastTickPosX, cart.posX);
			double cartY = MathHelper.lerp((double) partialTicks, cart.lastTickPosY, cart.posY);
			double cartZ = MathHelper.lerp((double) partialTicks, cart.lastTickPosZ, cart.posZ);
			Vec3d cartPos = cart.getPos(cartX, cartY, cartZ);

			if (cartPos != null) {

				Vec3d cartPosFront = cart.getPosOffset(cartX, cartY, cartZ, (double) 0.3F);
				Vec3d cartPosBack = cart.getPosOffset(cartX, cartY, cartZ, (double) -0.3F);
				if (cartPosFront == null)
					cartPosFront = cartPos;
				if (cartPosBack == null)
					cartPosBack = cartPos;

				cartX = cartPos.x - cartX;
				cartY = (cartPosFront.y + cartPosBack.y) / 2.0D - cartY;
				cartZ = cartPos.z - cartZ;

				GlStateManager.translatef((float) cartX, (float) cartY, (float) cartZ);
			}
		}

//		BlockPos anchor = entity.getContraption().getAnchor();
//		Vec3d rotationOffset = VecHelper.getCenterOf(anchor);
//		Vec3d offset = VecHelper.getCenterOf(anchor).scale(-1);

		TessellatorHelper.prepareFastRender();
		TessellatorHelper.begin(DefaultVertexFormats.BLOCK);
		ContraptionRenderer.render(entity.world, entity.getContraption(), superByteBuffer -> {
//			superByteBuffer.translate(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
			superByteBuffer.rotate(Axis.Y, angleYaw);
			superByteBuffer.rotate(Axis.Z, anglePitch);
			superByteBuffer.translate(x, y, z);
			superByteBuffer.offsetLighting(-x + entity.posX, -y + entity.posY, -z + entity.posZ);

		}, Tessellator.getInstance().getBuffer());
		TessellatorHelper.draw();

		GlStateManager.popMatrix();
		GlStateManager.shadeModel(7424);
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.matrixMode(5888);
		RenderHelper.enableStandardItemLighting();

		super.doRender(entity, x, y, z, yaw, partialTicks);
	}

}
