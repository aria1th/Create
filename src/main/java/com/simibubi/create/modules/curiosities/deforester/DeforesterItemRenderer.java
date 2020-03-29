package com.simibubi.create.modules.curiosities.deforester;

import org.lwjgl.opengl.GL13;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;

public class DeforesterItemRenderer extends ItemStackTileEntityRenderer {

	@Override
	public void render(ItemStack stack, MatrixStack ms, IRenderTypeBuffer buffer, int light, int overlay) {

		ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
		DeforesterModel mainModel = (DeforesterModel) itemRenderer.getItemModelWithOverrides(stack, Minecraft.getInstance().world, Minecraft.getInstance().player);
		float worldTime = AnimationTickHolder.getRenderTick();
		float lastCoordx = GLX.lastBrightnessX;
		float lastCoordy = GLX.lastBrightnessY;
		
		RenderSystem.pushMatrix();
		RenderSystem.translatef(0.5F, 0.5F, 0.5F);
		itemRenderer.renderItem(stack, mainModel.getBakedModel());

		RenderSystem.disableLighting();
		GLX.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240, 120);
		itemRenderer.renderItem(stack, mainModel.getPartial("light"));
		itemRenderer.renderItem(stack, mainModel.getPartial("blade"));
		GLX.glMultiTexCoord2f(GL13.GL_TEXTURE1, lastCoordx, lastCoordy);
		RenderSystem.enableLighting();
		
		float angle = worldTime * -.5f % 360;
		float xOffset = 0;
		float zOffset = 0;
		RenderSystem.translatef(-xOffset, 0, -zOffset);
		RenderSystem.rotatef(angle, 0, 1, 0);
		RenderSystem.translatef(xOffset, 0, zOffset);
		
		itemRenderer.renderItem(stack, mainModel.getPartial("gear"));
		

		RenderSystem.popMatrix();
	}

}
