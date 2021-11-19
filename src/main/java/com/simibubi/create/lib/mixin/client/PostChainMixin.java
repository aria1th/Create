package com.simibubi.create.lib.mixin.client;

import com.google.gson.JsonSyntaxException;

import com.mojang.blaze3d.pipeline.RenderTarget;

import com.simibubi.create.lib.extensions.RenderTargetExtensions;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

@Mixin(PostChain.class)
public class PostChainMixin {
	@Shadow
	@Final
	private RenderTarget screenTarget;

	@Inject(method = "addTempTarget", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;setClearColor(FFFF)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
	public void isStenicl(String name, int width, int height, CallbackInfo ci, RenderTarget rendertarget) {
		if (((RenderTargetExtensions)screenTarget).isStencilEnabled()) { ((RenderTargetExtensions)rendertarget).enableStencil(); }
	}
}
