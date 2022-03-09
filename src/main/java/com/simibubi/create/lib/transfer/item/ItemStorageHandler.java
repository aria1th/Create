package com.simibubi.create.lib.transfer.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

// this class is awful, but we don't have many options

/**
 * Wraps a Storage in an IItemHandler, for use in Create
 */
@SuppressWarnings({"UnstableApiUsage"})
public class ItemStorageHandler implements IItemHandlerModifiable {
	protected final Storage<ItemVariant> storage;
	private long version;
	private Int2ObjectOpenHashMap<ItemStack> cachedStack = new Int2ObjectOpenHashMap<>();
	public ItemStorageHandler(Storage<ItemVariant> storage) {
		this.storage = storage;
		this.version = storage.getVersion();
		getSlots();
	}
	private int slotCount = 0;
	@Override
	public int getSlots() {
		if(this.slotCount>0) return slotCount;
		//Do we have chance to change Slot count in storage? I doubt it.
		int slots = 0;
		try (Transaction t = Transaction.openOuter()) {
			for (StorageView<ItemVariant> view : storage.iterable(t)) {
				slots++;
			}
			t.abort();
		}
		this.slotCount = slots;
		return slots;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		try {
			if (storage.getVersion() == version && cachedStack.containsKey(slot)) {
				return cachedStack.get(slot);
			} else {
				if (storage.getVersion() != version) {
					version = storage.getVersion();
					cachedStack.clear();
				}
				ItemStack targetStack = ItemStack.EMPTY;
				try (Transaction t = Transaction.openOuter()) {
					int index = 0;
					for (StorageView<ItemVariant> view : storage.iterable(t)) {
						if (index == slot) {
							targetStack = view.getResource().toStack((int) view.getAmount());
							break;
						}
						index++;
					}
					t.abort();
				}
				cachedStack.put(slot, targetStack);
				return targetStack;
			}
		}
		catch (Exception e){
			return ItemStack.EMPTY;
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean sim) {
		if(stack.isEmpty())
			return stack;
		try (Transaction t = Transaction.openOuter()){
			long inserted = this.storage.insert(ItemVariant.of(stack), stack.getCount(), t);
			if (sim)
				t.abort();
			else{
				t.commit();
			}
			ItemStack remainder = stack.copy();
			remainder.setCount((int) (stack.getCount() - inserted));
			return remainder;
		}
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean sim) {
		ItemStack finalVal = ItemStack.EMPTY;
		try (Transaction t = Transaction.openOuter()) {
			int index = 0;
			for (StorageView<ItemVariant> view : storage.iterable(t)) {
				if (index == slot) {
					ItemVariant variant = view.getResource();
					long extracted = view.isResourceBlank() ? 0 : view.extract(variant, amount, t);
					if (extracted != 0) {
						finalVal = variant.toStack((int) extracted);
					}
					break;
				}
				index++;
			}
			if (!sim) {
				t.commit();
			}
			else {
				t.abort();
			}
		}
		return finalVal;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		// jank
		extractItem(slot, getSlotLimit(slot), false);
		insertItem(slot, stack, false);
	}
}
