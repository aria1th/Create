package com.simibubi.create.lib.transfer.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Wraps an IItemHandler in a Storage, for use outside Create
 */
@SuppressWarnings("UnstableApiUsage")
public class StorageItemHandler implements Storage<ItemVariant> {
	@Nonnull
	protected IItemHandler handler;

	public StorageItemHandler(@Nullable IItemHandler handler) {
		if (handler == null) {
			this.handler = EmptyHandler.INSTANCE;
		} else {
			this.handler = handler;
		}
	}

	@Nonnull
	public IItemHandler getHandler() {
		return handler;
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		ItemStack toInsert = resource.toStack((int) maxAmount);
		ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, toInsert, true);
		int inserted = toInsert.getCount() - remainder.getCount();
		if (inserted < 0){
			throw new IllegalArgumentException("Inserted amount must not be negative!");
		}
		transaction.addOuterCloseCallback(result -> {
			if (result.wasCommitted() && inserted > 0) {
				ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
			}
		});
		return inserted;
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		ItemStack toExtract = resource.toStack((int) maxAmount);
		ItemStack extracted = ItemHandlerHelper.extract(handler, toExtract, true);
		int extractedCount = extracted.getCount();
		transaction.addOuterCloseCallback(result -> {
			if (result.wasCommitted() && extractedCount > 0) {
				ItemHandlerHelper.extract(handler, toExtract, false);
			}
		});
		return extracted.getCount();
	}

	@Override
	public Iterable<StorageView<ItemVariant>> iterable(TransactionContext transaction) {
		int slots = handler.getSlots();
		List<StorageView<ItemVariant>> views = new ArrayList<>();
		for (int i = 0; i < slots; i++) {
			views.add(new SlotStorageView(i, handler));
		}
		return views;
	}

	@Override
	public Iterator<StorageView<ItemVariant>> iterator(TransactionContext transaction) {
		return iterable(transaction).iterator();
	}

	@Override
	@Nullable
	public StorageView<ItemVariant> exactView(TransactionContext transaction, ItemVariant resource) {
		for (StorageView<ItemVariant> view : iterable(transaction)) {
			if (view.getResource().equals(resource)) {
				return view;
			}
		}
		return null;
	}

	public static class SlotStorageView implements StorageView<ItemVariant> {
		protected final int slotIndex;
		protected final IItemHandler owner;

		public SlotStorageView(int index, IItemHandler owner) {
			this.owner = owner;
			this.slotIndex = index;
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			long actual = 0;
			ItemStack extracted = owner.extractItem(slotIndex, (int) maxAmount, true);
			if (extracted.is(resource.getItem())) {
				actual = extracted.getCount();
				long finalActual = actual;
				transaction.addCloseCallback((transaction1, result) -> {
					if (result.wasCommitted() && finalActual > 0) {
						owner.extractItem(slotIndex, (int) finalActual, false);
					}
				});
			}
			return actual;
		}

		@Override
		public boolean isResourceBlank() {
			return owner.getStackInSlot(slotIndex).isEmpty();
		}

		@Override
		public ItemVariant getResource() {
			return ItemVariant.of(owner.getStackInSlot(slotIndex).copy());
		}

		@Override
		public long getAmount() {
			return owner.getStackInSlot(slotIndex).getCount();
		}

		@Override
		public long getCapacity() {
			return owner.getSlotLimit(slotIndex);
		}
	}
}
