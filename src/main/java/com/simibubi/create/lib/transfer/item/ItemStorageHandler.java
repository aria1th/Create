package com.simibubi.create.lib.transfer.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

// this class is awful, but we don't have many options

/**
 * Wraps a Storage in an IItemHandler, for use in Create
 */
@SuppressWarnings({"UnstableApiUsage"})
public class ItemStorageHandler implements IItemHandlerModifiable {
	protected final Storage<ItemVariant> storage;
	private long version;
	private final Int2ObjectOpenHashMap<ItemStack> cachedStack = new Int2ObjectOpenHashMap<>();
	public ItemStorageHandler(Storage<ItemVariant> storage) {
		this.storage = storage;
		this.version = storage.getVersion();
		getSlots();
	}
	private int slotCount = 0;
	@Override
	public int getSlots() {
		if (this.slotCount>0) return slotCount;
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
		if (storage.getVersion() == version && cachedStack.containsKey(slot)) {
			return cachedStack.get(slot);
		}
		else {
			if (storage.getVersion() != version) {
				version = storage.getVersion();
				cachedStack.clear();
			}
			ItemStack targetStack = ItemStack.EMPTY;
			if (Transaction.isOpen()){
				TransactionContext context = Transaction.getCurrentUnsafe();
				int index = 0;
				for (StorageView<ItemVariant> view : storage.iterable(context)) {
					if (index == slot){
						targetStack = view.getResource().toStack((int) view.getAmount());
						break;
					}
					index++;
				}
				cachedStack.put(slot, targetStack);
				return targetStack;
			}
			else {
				try (Transaction t = Transaction.openOuter()) {
					int index = 0;
					for (StorageView<ItemVariant> view : storage.iterable(t)) {
						if (index == slot) {
							targetStack = view.getResource().toStack((int) view.getAmount());
							break;
						}
						index++;
					}
				}
				catch (Exception e){
					return ItemStack.EMPTY;
				}
			}

			cachedStack.put(slot, targetStack);
			return targetStack;
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean sim) {
		if(stack.isEmpty())
			return stack;
		try (Transaction t = Transaction.openOuter()){
			long inserted;
			ItemStack remainder = stack.copy();
			if (sim)
				inserted = this.storage.simulateInsert(ItemVariant.of(remainder), stack.getCount(), t);
			else {
				inserted = this.storage.insert(ItemVariant.of(remainder), stack.getCount(), t);
				t.commit();
			}
			remainder.shrink((int) inserted);
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
