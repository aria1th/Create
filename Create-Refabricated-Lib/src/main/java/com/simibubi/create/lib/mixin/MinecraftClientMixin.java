package com.simibubi.create.lib.mixin;

import org.objectweb.asm.Opcodes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import com.simibubi.create.lib.event.BeforeFirstReloadCallback;
import com.simibubi.create.lib.event.ClientWorldEvents;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	private ClientWorld world;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"), method = "<init>(Lnet/minecraft/client/RunArgs;)V")
	public void create$beforeFirstReload(RunArgs args, CallbackInfo ci) {
		BeforeFirstReloadCallback.EVENT.invoker().beforeFirstReload((MinecraftClient) (Object) this);
	}

	@Inject(at = @At("HEAD"), method = "joinWorld(Lnet/minecraft/client/world/ClientWorld;)V")
	public void create$onHeadJoinWorld(ClientWorld world, CallbackInfo ci) {
		if (this.world != null) {
			ClientWorldEvents.UNLOAD.invoker().onWorldUnload((MinecraftClient) (Object) this, this.world);
		}
	}

	@Inject(at = @At(value = "JUMP", opcode = Opcodes.IFNULL, ordinal = 1, shift = Shift.AFTER), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V")
	public void create$onDisconnect(Screen screen, CallbackInfo ci) {
		ClientWorldEvents.UNLOAD.invoker().onWorldUnload((MinecraftClient) (Object) this, this.world);
	}
}