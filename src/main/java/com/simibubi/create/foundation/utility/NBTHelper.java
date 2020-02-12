package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.AxisAlignedBB;

public class NBTHelper {

	public static <T extends Enum<?>> T readEnum(String name, Class<T> enumClass) {
		T[] enumConstants = enumClass.getEnumConstants();
		if (enumConstants == null)
			throw new IllegalArgumentException("Non-Enum class passed to readEnum(): " + enumClass.getName());
		for (T t : enumConstants) {
			if (t.name().equals(name))
				return t;
		}
		return enumConstants[0];
	}

	public static <T extends Enum<?>> String writeEnum(T enumConstant) {
		return enumConstant.name();
	}

	public static <T> ListNBT writeCompoundList(List<T> list, Function<T, CompoundNBT> serializer) {
		ListNBT listNBT = new ListNBT();
		list.forEach(t -> listNBT.add(serializer.apply(t)));
		return listNBT;
	}

	public static <T> List<T> readCompoundList(ListNBT listNBT, Function<CompoundNBT, T> deserializer) {
		List<T> list = new ArrayList<>(listNBT.size());
		listNBT.forEach(inbt -> list.add(deserializer.apply((CompoundNBT) inbt)));
		return list;
	}
	
	public static ListNBT writeItemList(List<ItemStack> stacks) {
		return writeCompoundList(stacks, ItemStack::serializeNBT);
	}
	
	public static List<ItemStack> readItemList(ListNBT stacks) {
		return readCompoundList(stacks, ItemStack::read);
	}
	
	public static ListNBT writeAABB(AxisAlignedBB bb) {
		ListNBT bbtag = new ListNBT();
		bbtag.add(new FloatNBT((float) bb.minX));
		bbtag.add(new FloatNBT((float) bb.minY));
		bbtag.add(new FloatNBT((float) bb.minZ));
		bbtag.add(new FloatNBT((float) bb.maxX));
		bbtag.add(new FloatNBT((float) bb.maxY));
		bbtag.add(new FloatNBT((float) bb.maxZ));
		return bbtag;
	}

	public static AxisAlignedBB readAABB(ListNBT bbtag) {
		if (bbtag == null || bbtag.isEmpty())
			return null;
		return new AxisAlignedBB(bbtag.getFloat(0), bbtag.getFloat(1), bbtag.getFloat(2), bbtag.getFloat(3),
				bbtag.getFloat(4), bbtag.getFloat(5));

	}

}
