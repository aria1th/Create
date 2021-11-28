package com.simibubi.create.content.logistics.block.inventories;

import java.util.List;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class CreativeCrateTileEntity extends CrateTileEntity {

	public CreativeCrateTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inv = new BottomlessItemHandler(filtering::getFilter);
		itemHandler = LazyOptional.of(() -> inv);
	}

	FilteringBehaviour filtering;
	LazyOptional<IItemHandler> itemHandler;
	private BottomlessItemHandler inv;

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		behaviours.add(filtering = createFilter().onlyActiveWhen(this::filterVisible)
			.withCallback(this::filterChanged));
	}

	private boolean filterVisible() {
		if (!hasLevel() || isDoubleCrate() && !isSecondaryCrate())
			return false;
		return true;
	}

	private void filterChanged(ItemStack filter) {
		if (!filterVisible())
			return;
		CreativeCrateTileEntity otherCrate = getOtherCrate();
		if (otherCrate == null)
			return;
		if (ItemStack.isSame(filter, otherCrate.filtering.getFilter()))
			return;
		otherCrate.filtering.setFilter(filter);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (itemHandler != null)
			itemHandler.invalidate();
	}

	private CreativeCrateTileEntity getOtherCrate() {
		if (!AllBlocks.CREATIVE_CRATE.has(getBlockState()))
			return null;
		BlockEntity tileEntity = level.getBlockEntity(worldPosition.relative(getFacing()));
		if (tileEntity instanceof CreativeCrateTileEntity)
			return (CreativeCrateTileEntity) tileEntity;
		return null;
	}

	public void onPlaced() {
		if (!isDoubleCrate())
			return;
		CreativeCrateTileEntity otherCrate = getOtherCrate();
		if (otherCrate == null)
			return;

		filtering.withCallback($ -> {
		});
		filtering.setFilter(otherCrate.filtering.getFilter());
		filtering.withCallback(this::filterChanged);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return itemHandler.cast();
		return super.getCapability(cap, side);
	}

	public FilteringBehaviour createFilter() {
		return new FilteringBehaviour(this, new ValueBoxTransform() {

			@Override
			protected void rotate(BlockState state, PoseStack ms) {
				TransformStack.cast(ms)
					.rotateX(90);
			}

			@Override
			protected Vec3 getLocalOffset(BlockState state) {
				return new Vec3(0.5, 13 / 16d, 0.5);
			}

			protected float getScale() {
				return super.getScale() * 1.5f;
			};

		});
	}

}
