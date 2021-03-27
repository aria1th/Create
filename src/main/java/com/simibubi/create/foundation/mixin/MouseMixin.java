package com.simibubi.create.foundation.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.entity.player.PlayerInventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.foundation.block.entity.behaviour.scrollvalue.ScrollValueHandler;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public class MouseMixin {
	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(D)V"), method = "onMouseScroll(JDD)V")
	private void onMouseScroll(PlayerInventory inventory, double delta) {
		boolean cancelled = /*CreateClient.schematicHandler.mouseScrolled(delta)
				|| CreateClient.schematicAndQuillHandler.mouseScrolled(delta) || FilteringHandler.onScroll(delta)
				|| */ScrollValueHandler.onScroll(delta);

		if (!cancelled) {
			inventory.scrollInHotbar(delta);
		}
	}
}