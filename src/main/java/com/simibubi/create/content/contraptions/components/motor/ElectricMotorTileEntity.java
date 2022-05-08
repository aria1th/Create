package com.simibubi.create.content.contraptions.components.motor;

import java.util.List;
import java.util.Random;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueBehaviour.StepContext;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import team.reborn.energy.api.EnergyStorage;

/**
 * Referenced from createaddition ElectricMotorTileEntity
 * (https://github.com/mrh0/createaddition/blob/fabric-1.18/src/main/java/com/mrh0/createaddition/blocks/electric_motor/ElectricMotorTileEntity.java)
 */

@SuppressWarnings("UnstableApiUsage")
public class ElectricMotorTileEntity extends GeneratingKineticTileEntity {
	// settings for now
	public static final int SPEED_RANGE = AllConfigs.SERVER.kinetics.maxMotorSpeed.get();
	public static final int DEFAULT_SPEED = 16;
	public static final int TICK_PERIOD = 20;
	public static final int ENERGY_CAPACITY_MULTIPLIER = 4;

	private static final Random rand = new Random();

	private final int offset;
	private boolean active;
	private int capacity = 4;
	protected ScrollValueBehaviour generatedSpeed;
	private final ElectricMotorEnergyStorage energyStorage;


	public ElectricMotorTileEntity(BlockEntityType<? extends ElectricMotorTileEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energyStorage = new ElectricMotorEnergyStorage();
		offset = rand.nextInt(TICK_PERIOD);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		// generatedSpeed
		CenteredSideValueBoxTransform speedBoxTransform = new CenteredSideValueBoxTransform((motor, side) -> motor.getValue(ElectricMotorBlock.FACING) == side.getOpposite());
		generatedSpeed = new ScrollValueBehaviour(Lang.translate("generic.speed"), this, speedBoxTransform);
		generatedSpeed.between(-SPEED_RANGE, SPEED_RANGE);
		generatedSpeed.value = DEFAULT_SPEED;
		generatedSpeed.scrollableValue = DEFAULT_SPEED;
		generatedSpeed.withUnit(i -> Lang.translate("generic.unit.rpm"));
		generatedSpeed.withCallback(i -> updateOperation());
		generatedSpeed.withStepFunction(ElectricMotorTileEntity::stepSpeed);
		behaviours.add(generatedSpeed);
	}

	public static int stepSpeed(StepContext context) {
		int current = context.currentValue;
		int step = 1;

		if (!context.shift) {
			int magnitude = Math.abs(current) - (context.forward == current > 0 ? 0 : 1);

			if (magnitude >= 4)
				step *= 4;
			if (magnitude >= 32)
				step *= 4;
			if (magnitude >= 128)
				step *= 4;
		}

		return current + (context.forward ? step : -step) == 0 ? step + 1 : step;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
			updateGeneratedRotation();
	}

	@Override
	public float getGeneratedSpeed() {
		if (!AllBlocks.ELECTRIC_MOTOR.has(getBlockState()))
			return 0;
		return convertToDirection(active ? generatedSpeed.getValue() : 0, getBlockState().getValue(ElectricMotorBlock.FACING));
	}

	public void setCapacity(int capacity) {
		if (capacity != this.capacity) {
			this.capacity = capacity;
			updateOperation();
		}
	}

	private void updateOperation() {
		if (active && !canActivate())
			deactivate();
		energyStorage.update();
		updateGeneratedRotation();
	}

	@Override
	public float calculateAddedStressCapacity() {
		return capacity;
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		active = compound.getBoolean("active");
		capacity = compound.getInt("capacity");
		CompoundTag energyStorageTag = compound.getCompound("EnergyStorage");
		energyStorage.amount = energyStorageTag.getLong("energy");
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putBoolean("active", active);
		compound.putInt("capacity", capacity);
		CompoundTag energyStorageTag = new CompoundTag();
		energyStorageTag.putLong("energy", energyStorage.amount);
		compound.put("EnergyStorage", energyStorageTag);
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide || (level.getGameTime() + offset) % TICK_PERIOD != 0) return;

		long consumption = getBatchedEnergyConsumption();
		if (active) {
			try (Transaction transaction = Transaction.openOuter()) {
				long extracted = energyStorage.extract(consumption, transaction);
				if (extracted < consumption) {
					transaction.abort();
					deactivate();
				} else {
					transaction.commit();
				}
			}
		}
	}

	public boolean isActive() {
		return active;
	}

	public void activate() {
		this.active = true;
		try (Transaction transaction = Transaction.openOuter()) {
			int remainingPeriod = (int) (TICK_PERIOD - (level.getGameTime() + offset) % TICK_PERIOD);
			long batchedConsumption = getBatchedEnergyConsumption(remainingPeriod);
			long extracted = energyStorage.extract(batchedConsumption, transaction);
			if(batchedConsumption > extracted) {
				active = false;
			}
		}
		updateGeneratedRotation();
	}

	public void deactivate() {
		this.active = false;
		updateGeneratedRotation();
	}

	public boolean canActivate() {
		try (Transaction transaction = Transaction.openOuter()) {
			int remainingPeriod = (int) (TICK_PERIOD - (level.getGameTime() + offset) % TICK_PERIOD);
			long batchedConsumption = getBatchedEnergyConsumption(remainingPeriod);
			long extracted = energyStorage.extract(batchedConsumption, transaction);
			transaction.abort();
			return batchedConsumption <= extracted;
		}
	}

	public static EnergyStorage getEnergyStorage(ElectricMotorTileEntity tileEntity, Direction direction) {
		if (tileEntity.isEnergyInput(direction)) {
			return tileEntity.energyStorage;
		}
		return null;
	}

	public boolean isEnergyInput(Direction side) {
		return side != getBlockState().getValue(ElectricMotorBlock.FACING);
	}


	public float getEnergyConsumptionRateMultiplier() {
		return (float) 1 / AllConfigs.SERVER.kinetics.motorStressPerEnergy.get();
	}

	public long getEnergyConsumption() {
		return (long) Math.ceil(getEnergyConsumptionRateMultiplier() * Math.abs(generatedSpeed.getValue()) * capacity);
	}


	public long getBatchedEnergyConsumption(int period) {
		return getEnergyConsumption() * period;
	}

	public long getBatchedEnergyConsumption() {
		return getBatchedEnergyConsumption(TICK_PERIOD);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		float consumption = getEnergyConsumption();
		long amount = energyStorage.getAmount();

		tooltip.add(componentSpacing.plainCopy()
				.append(new TextComponent(" " + String.format("%.2f", consumption))
						.append(Lang.translate("generic.unit.energy_per_tick"))
						.withStyle(ChatFormatting.AQUA))
				.append(" ")
				.append(Lang.translate("gui.goggles.at_current_energy_consumption")
						.withStyle(ChatFormatting.DARK_GRAY)));

		return added;
	}

	private class ElectricMotorEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
		private long amount;

		public ElectricMotorEnergyStorage() {
			StoragePreconditions.notNegative(amount);
		}

		@Override
		protected Long createSnapshot() {
			return amount;
		}

		@Override
		protected void readSnapshot(Long snapshot) {
			amount = snapshot;
		}

		@Override
		public boolean supportsInsertion() {
			return true;
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notNegative(maxAmount);

			long inserted = (long) Math.min(maxAmount, getCapacity() - amount);

			if (inserted > 0) {
				updateSnapshots(transaction);
				amount += inserted;
				return inserted;
			}

			return 0;
		}

		@Override
		public boolean supportsExtraction() {
			return false;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notNegative(maxAmount);

			long extracted = Math.min(maxAmount, amount);

			if (extracted > 0) {
				updateSnapshots(transaction);
				amount -= extracted;
				return extracted;
			}

			return 0;
		}

		@Override
		public long getAmount() {
			return amount;
		}

		@Override
		public long getCapacity() {
			return getBatchedEnergyConsumption() * ENERGY_CAPACITY_MULTIPLIER;
		}

		public void update() {
			if (amount > getCapacity())
				amount = getCapacity();
		}
	}


}
