package com.simibubi.create.lib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.lib.item.EquipmentItem;

import net.minecraft.entity.MobEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/MobEntity;getSlotForItemStack(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/inventory/EquipmentSlotType;", cancellable = true)
	static void create$getSlotForItemStack(ItemStack itemStack, CallbackInfoReturnable<EquipmentSlotType> cir) {
		if (itemStack.getItem() instanceof EquipmentItem) {
			cir.setReturnValue(((EquipmentItem) itemStack.getItem()).getEquipmentSlot(itemStack));
		}
	}
}