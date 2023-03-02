package com.simibubi.create.content.contraptions.fluids.actors;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HosePulleyTileEntity extends KineticTileEntity implements SidedStorageBlockEntity {

	LerpedFloat offset;
	boolean isMoving;

	private SmartFluidTank internalTank;
	private FluidDrainingBehaviour drainer;
	private FluidFillingBehaviour filler;
	private HosePulleyFluidHandler handler;
	private boolean infinite;
	private FluidStack cachedFluid = FluidStack.EMPTY;

	public HosePulleyTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		offset = LerpedFloat.linear()
			.startWithValue(0);
		isMoving = true;					// fabric: we hold twice forge, due to differences in logic
											// the handler refills from the world when stored > BUCKET, which could result in nearly 2 buckets worth.
											// on forge, there's no risk of negative results with stored > capacity
											// fabric does have this issue, so we can *not* allow it.
		internalTank = new SmartFluidTank(FluidConstants.BUCKET * 4, this::onTankContentsChanged);
		handler = new HosePulleyFluidHandler(internalTank, filler, drainer,
			() -> worldPosition.below((int) Math.ceil(offset.getValue())), () -> !this.isMoving);
	}

	@Override
	public void sendData() {
		infinite = filler.infinite || drainer.infinite || (cachedFluid != null && !cachedFluid.isEmpty());
		if(infinite && !internalTank.isEmpty() && (cachedFluid == null || cachedFluid.isEmpty())){
			cachedFluid = internalTank.getFluid().copy();
			cachedFluid.setAmount(2174741824L);
		}
		if (infinite && !cachedFluid.isEmpty()){
			this.internalTank.setFluid(cachedFluid.copy());
		}
		super.sendData();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (infinite)
			TooltipHelper.addHint(tooltip, "hint.hose_pulley");
		return addToGoggleTooltip;
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		drainer = new FluidDrainingBehaviour(this);
		filler = new FluidFillingBehaviour(this);
		behaviours.add(drainer);
		behaviours.add(filler);
		super.addBehaviours(behaviours);
		registerAwardables(behaviours, AllAdvancements.HOSE_PULLEY, AllAdvancements.HOSE_PULLEY_LAVA);
	}

	protected void onTankContentsChanged(FluidStack contents) {}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		isMoving = true;
		if (getSpeed() == 0) {
			offset.forceNextSync();
			offset.setValue(Math.round(offset.getValue()));
			isMoving = false;
		}

		if (isMoving) {
			float newOffset = offset.getValue() + getMovementSpeed();
			if (newOffset < 0)
				isMoving = false;
			if (!level.getBlockState(worldPosition.below((int) Math.ceil(newOffset)))
				.getMaterial()
				.isReplaceable()) {
				isMoving = false;
			}
			if (isMoving) {
				drainer.reset(null);
				filler.reset(null);
			}
		}

		super.onSpeedChanged(previousSpeed);
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().expandTowards(0, -offset.getValue(), 0);
	}

	@Override
	public void tick() {
		super.tick();
		float newOffset = offset.getValue() + getMovementSpeed();
		if (newOffset < 0) {
			newOffset = 0;
			isMoving = false;
		}
		if (!level.getBlockState(worldPosition.below((int) Math.ceil(newOffset)))
			.getMaterial()
			.isReplaceable()) {
			newOffset = (int) newOffset;
			isMoving = false;
		}
		if (getSpeed() == 0)
			isMoving = false;
		if (cachedFluid == null && internalTank.getFluidAmount() > 1048576L){
			infinite = true;
			cachedFluid = internalTank.getFluid().copy();
			cachedFluid.setAmount(2147483624L);
		}
		if (cachedFluid != null && !cachedFluid.isEmpty()){
			infinite = true;
			this.internalTank.setFluid(cachedFluid.copy());
		}
		offset.setValue(newOffset);
		invalidateRenderBoundingBox();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide)
			return;
		if (isMoving)
			return;

		int ceil = (int) Math.ceil(offset.getValue() + getMovementSpeed());
		if (getMovementSpeed() > 0 && level.getBlockState(worldPosition.below(ceil))
			.getMaterial()
			.isReplaceable()) {
			isMoving = true;
			drainer.reset(null);
			filler.reset(null);
			return;
		}

		sendData();
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		if (clientPacket)
			offset.forceNextSync();
		compound.put("Offset", offset.writeNBT());
		compound.put("Tank", internalTank.writeToNBT(new CompoundTag()));
		if (infinite) compound.put("cachedFluid", cachedFluid.writeToNBT(new CompoundTag()));
		super.write(compound, clientPacket);
		if (clientPacket)
			compound.putBoolean("Infinite", infinite);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		offset.readNBT(compound.getCompound("Offset"), clientPacket);
		internalTank.readFromNBT(compound.getCompound("Tank"));
		internalTank.setCapacity((Long) (FluidConstants.BUCKET * 4));
		infinite = compound.getBoolean("Infinite");
		cachedFluid = compound.contains("cachedFluid") ?  FluidStack.loadFluidStackFromNBT(compound.getCompound("cachedFluid")) : FluidStack.EMPTY;
		if (!cachedFluid.isEmpty()) infinite = true;
		if(internalTank.getFluidAmount() > 1048576L || (infinite && (cachedFluid == null || cachedFluid.isEmpty())) && !internalTank.isEmpty() ){
			infinite = true;
			cachedFluid = internalTank.getFluid().copy();
			cachedFluid.setAmount(2147483624L);
		}
		super.read(compound, clientPacket);
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	public float getMovementSpeed() {
		float movementSpeed = convertToLinear(getSpeed());
		if (level.isClientSide)
			movementSpeed *= ServerSpeedProvider.get();
		return movementSpeed;
	}

	public float getInterpolatedOffset(float pt) {
		return offset.getValue(pt);
	}

	@Nullable
	@Override
	public Storage<FluidVariant> getFluidStorage(@Nullable Direction face) {
		if (face == null || HosePulleyBlock.hasPipeTowards(level, worldPosition, getBlockState(), face)) {
			return handler;
		}
		return null;
	}
}
