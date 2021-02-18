package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.CreateClient;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(World.class)
public class OnRemoveTileMixin {

    @Shadow @Final public boolean isRemote;

    /**
     * JUSTIFICATION: This method is called whenever a tile entity is removed due
     * to a change in block state, even on the client. By hooking into this method,
     * we gain easy access to the information while having no impact on performance.
     */
    @Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/World;getTileEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/tileentity/TileEntity;"), method = "removeTileEntity", locals = LocalCapture.CAPTURE_FAILHARD)
    private void onRemoveTile(BlockPos pos, CallbackInfo ci, TileEntity te) {
        if (isRemote) CreateClient.kineticRenderer.remove(te);
    }
}
