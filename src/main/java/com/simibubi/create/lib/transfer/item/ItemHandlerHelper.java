package com.simibubi.create.lib.transfer.item;

import net.minecraft.world.item.ItemStack;

public class ItemHandlerHelper {
	public static boolean canItemStacksStack(ItemStack first, ItemStack second) {
		if (first.isEmpty() || !first.sameItem(second) || first.hasTag() != second.hasTag()) return false;

		return !first.hasTag() || first.getTag().equals(second.getTag());
	}

	public static ItemStack copyStackWithSize(ItemStack stack, int size) {
		if (size == 0) return ItemStack.EMPTY;
		ItemStack copy = stack.copy();
		copy.setCount(size);
		return copy;
	}

	public static ItemStack insertItemStacked(IItemHandler inv, ItemStack stack, boolean sim) {
		if (sim){
			return insertItemStackedSimulates(inv, stack);
		}
		else {
			return insertItemStacked(inv, stack);
		}
	}

	private static ItemStack insertItemStackedSimulates(IItemHandler inv, ItemStack stack) {
		if (inv == null || stack.isEmpty()) return stack;
		if (!stack.isStackable()) return insertItem(inv, stack, true);

		int slotCount = inv.getSlots();

		for (int i = 0; i < slotCount; i++) {
			ItemStack stackInSlot = inv.getStackInSlot(i);
			if (canItemStacksStack(stack, stackInSlot)) {
				stack = inv.insertItem(i, stack, true);
				if (stack.isEmpty()) break;
			}
		}

		if (!stack.isEmpty()) {
			for (int i = 0; i < slotCount; i++) {
				if (inv.getStackInSlot(i).isEmpty()) {
					stack = inv.insertItem(i, stack, true);
					if (stack.isEmpty()) break;
				}
			}
		}

		return stack;
	}

	/**
	 *
	 * @param inv
	 * @param stack
	 * @return leftovers
	 * Insert without simulation, uses fabric transfer api.
	 */
	private static ItemStack insertItemStacked(IItemHandler inv, ItemStack stack) {
		if (inv == null || stack.isEmpty()) return stack;
		if (!stack.isStackable()) return insertItem(inv, stack, false);

		int slotCount = inv.getSlots();

		for (int i = 0; i < slotCount; i++) {
			ItemStack stackInSlot = inv.getStackInSlot(i);
			if (canItemStacksStack(stack, stackInSlot)) {
				stack = inv.insertItem(i, stack, false);
				if (stack.isEmpty()) break;
			}
		}

		if (!stack.isEmpty()) {
			for (int i = 0; i < slotCount; i++) {
				if (inv.getStackInSlot(i).isEmpty()) {
					stack = inv.insertItem(i, stack, false);
					if (stack.isEmpty()) break;
				}
			}
		}

		return stack;
	}
	/**
	 * @return stack extracted
	 */
	public static ItemStack extract(IItemHandler inv, ItemStack stack, boolean sim) {
		int toExtract = stack.getCount();
		int totalSlots = inv.getSlots();
		ItemStack finalStack = ItemStack.EMPTY;

		for (int i = 0; i < totalSlots; i++) {
			ItemStack stackInSlot = inv.getStackInSlot(i);
			if (!canItemStacksStack(stackInSlot, stack)) continue;
			ItemStack extracted = inv.extractItem(i, toExtract, sim);
			toExtract -= extracted.getCount();
			if (finalStack == ItemStack.EMPTY) {
				finalStack = extracted;
			} else {
				finalStack.setCount(finalStack.getCount() + extracted.getCount());
			}
		}

		return finalStack;
	}

	/**
	 * @return stack not inserted
	 */
	public static ItemStack insertItem(IItemHandler inv, ItemStack stack, boolean sim) {
		if (inv == null || stack.isEmpty()) return stack;

		for (int i = 0; i < inv.getSlots(); i++) {
			stack = inv.insertItem(i, stack, sim);
			if (stack.isEmpty()) return ItemStack.EMPTY;
		}

		return stack;
	}
}
