// PORTED CREATE SOURCE

package com.simibubi.create.content.logistics.item.filter.attribute;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParseException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;

import com.simibubi.create.content.logistics.item.filter.ItemAttribute;

public class ItemNameAttribute implements ItemAttribute {
	String itemName;

	public ItemNameAttribute(String itemName) {
		this.itemName = itemName;
	}

	@Override
	public boolean appliesTo(ItemStack itemStack) {
		return extractCustomName(itemStack).equals(itemName);
	}

	@Override
	public List<ItemAttribute> listAttributesOf(ItemStack itemStack) {
		String name = extractCustomName(itemStack);

		List<ItemAttribute> atts = new ArrayList<>();
		if(name.length() > 0) {
			atts.add(new ItemNameAttribute(name));
		}
		return atts;
	}

	@Override
	public String getTranslationKey() {
		return "has_name";
	}

	@Override
	public Object[] getTranslationParameters() {
		return new Object[] { itemName };
	}

	@Override
	public void writeNBT(CompoundTag nbt) {
		nbt.putString("name", this.itemName);
	}

	@Override
	public ItemAttribute readNBT(CompoundTag nbt) {
		return new ItemNameAttribute(nbt.getString("name"));
	}

	private String extractCustomName(ItemStack stack) {
		CompoundTag compoundnbt = stack.getSubTag("display");
		if (compoundnbt != null && compoundnbt.contains("Name", 8)) {
			try {
				Text itextcomponent = Text.Serializer.fromJson(compoundnbt.getString("Name"));
				if (itextcomponent != null) {
					return itextcomponent.getString();
				}
			} catch (JsonParseException ignored) {
			}
		}
		return "";
	}
}
