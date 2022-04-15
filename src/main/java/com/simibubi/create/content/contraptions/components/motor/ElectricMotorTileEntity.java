package com.simibubi.create.content.contraptions.components.motor;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueBehaviour.StepContext;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import reborncore.api.IListInfoProvider;
import reborncore.common.powerSystem.PowerSystem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Referenced from createaddition ElectricMotorTileEntity
 * (https://github.com/mrh0/createaddition/blob/fabric-1.18/src/main/java/com/mrh0/createaddition/blocks/electric_motor/ElectricMotorTileEntity.java)
 */

@SuppressWarnings("UnstableApiUsage")
public class ElectricMotorTileEntity extends GeneratingKineticTileEntity implements IListInfoProvider {
	protected ScrollValueBehaviour generatedSpeed;
	private final SimpleEnergyStorage energyStorage;

	// settings for now
	public static final int RPM_RANGE = AllConfigs.SERVER.kinetics.maxMotorSpeed.get();
	public static final int DEFAULT_SPEED = 32;
	public static final int MIN_CONSUMPTION = 8;
	public static final long MAX_TRANSFER = 256L;
	public static final long CAPACITY = 2048L;
	public static final int STRESS = 8192;

	private boolean active;

	public ElectricMotorTileEntity(BlockEntityType<? extends ElectricMotorTileEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
			@Override
			protected void onFinalCommit() {
				setChanged();
			}

			@Override
			public boolean supportsExtraction() {
				return false;
			}
		};
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		CenteredSideValueBoxTransform slot = new CenteredSideValueBoxTransform((motor, side) -> motor.getValue(ElectricMotorBlock.FACING) == side.getOpposite());
		generatedSpeed = new ScrollValueBehaviour(Lang.translate("generic.speed"), this, slot);
		generatedSpeed.between(-RPM_RANGE, RPM_RANGE);
		generatedSpeed.value = DEFAULT_SPEED;
		generatedSpeed.scrollableValue = DEFAULT_SPEED;
		generatedSpeed.withUnit(i -> Lang.translate("generic.unit.rpm"));
		generatedSpeed.withCallback(this::updateGeneratedRotation);
		generatedSpeed.withStepFunction(ElectricMotorTileEntity::step);
		behaviours.add(generatedSpeed);
	}

	public static int step(StepContext context) {
		int current = context.currentValue;
		int step = 1;
		if(!context.shift) {
			int magnitude = Math.abs(current) - (context.forward == current > 0 ? 0 : 1);
			if(magnitude >= 4) step *= 4;
			if(magnitude >= 32) step *= 4;
			if(magnitude >= 128) step *= 4;
		}

		return step;
	}

	public float calculateAddedStressCapacity() {
		float capacity = STRESS / 256f;
		this.lastCapacityProvided = capacity;
		return capacity;
	}

	public void updateGeneratedRotation(int i) {
		super.updateGeneratedRotation();
		setRPM(i);
	}

	@Override
	public void initialize() {
		super.initialize();
		if(!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
			updateGeneratedRotation();
	}

	@Override
	public float getGeneratedSpeed() {
		if(!AllBlocks.ELECTRIC_MOTOR.has(getBlockState()))
			return 0;
		return convertToDirection(active ? generatedSpeed.getValue() : 0, getBlockState().getValue(ElectricMotorBlock.FACING));
	}

	@Override
	protected Block getStressConfigKey() {
		return AllBlocks.WATER_WHEEL.get();
	}

	public static SimpleEnergyStorage getEnergyStorage(ElectricMotorTileEntity tileEntity, Direction direction) {
		if(tileEntity.isEnergyInput(direction)) {
			return tileEntity.energyStorage;
		}
		return null;
	}

	public boolean isEnergyInput(Direction side) {
		return side != getBlockState().getValue(ElectricMotorBlock.FACING);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		active = compound.getBoolean("active");
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putBoolean("active", active);
	}

	public static int getEnergyConsumptionRate(int rpm) {
		return Math.abs(rpm) > 0 ? (int)Math.max(16 * (double)Math.abs(rpm), (double)MIN_CONSUMPTION) : 0;
	}

	@Override
	public void tick() {
		super.tick();

		if(level.isClientSide) return;
		long consumption = getEnergyConsumptionRate(generatedSpeed.getValue());
		if(!active && energyStorage.amount > consumption * 2) {
			active = true;
			updateGeneratedRotation();
		} else {
			try(Transaction transaction = Transaction.openOuter()) {
				long extracted = energyStorage.extract(consumption, transaction);
				if(extracted < consumption) {
					active = false;
					updateGeneratedRotation();
				}
			}
		}
	}


	public int getDurationAngle(int deg, float initialProgress, float speed) {
		float absSpeed = Math.abs(speed);
		float absDeg = Math.abs(deg);
		if(absSpeed < 0.1f) return 0;
		double degreesPerTick = (absSpeed * 360) / 60 / 20;
		return (int)  ((1 - initialProgress) * absDeg / degreesPerTick);
	}

	public int getDurationDistance(int dis, float initialProgress, float speed) {
		float absSpeed = Math.abs(speed);
		int absDis = Math.abs(dis);
		if(absSpeed < 0.1f) return 0;
		double metersPerTick = absSpeed / 512;
		return (int) ((1 - initialProgress) * absDis / metersPerTick);
	}

	public void setRPM(int rpm) {
		generatedSpeed.setValue(Math.max(Math.min(rpm, RPM_RANGE), -RPM_RANGE));
	}

	public int getRPM() {
		return generatedSpeed.getValue();
	}

	public int getGeneratedStress() {
		return (int) calculateAddedStressCapacity();
	}

	public int getEnergyConsumption() {
		return getEnergyConsumptionRate(generatedSpeed.getValue());
	}

	@Override
	public void addInfo(List<Component> info, boolean isReal, boolean hasData) {
		if (!isReal && hasData) {
			info.add(
					new TranslatableComponent("reborncore.tooltip.energy")
							.withStyle(ChatFormatting.GRAY)
							.append(": ")
							.append(PowerSystem.getLocalizedPower(energyStorage.amount))
							.withStyle(ChatFormatting.GOLD)
			);
		}

		info.add(
				new TranslatableComponent("reborncore.tooltip.energy.maxEnergy")
						.withStyle(ChatFormatting.GRAY)
						.append(": ")
						.append(PowerSystem.getLocalizedPower(CAPACITY))
						.withStyle(ChatFormatting.GOLD)
		);
		info.add(
				new TranslatableComponent("reborncore.tooltip.energy.inputRate")
						.withStyle(ChatFormatting.GRAY)
						.append(": ")
						.append(PowerSystem.getLocalizedPower(MAX_TRANSFER))
						.withStyle(ChatFormatting.GOLD)
		);
		info.add(
				new TranslatableComponent("reborncore.tooltip.energy.outputRate")
						.withStyle(ChatFormatting.GRAY)
						.append(": ")
						.append(PowerSystem.getLocalizedPower(MAX_TRANSFER))
						.withStyle(ChatFormatting.GOLD)
		);

		if (isReal) {
			info.add(
					new TranslatableComponent("reborncore.tooltip.energy.change")
							.withStyle(ChatFormatting.GRAY)
							.append(": ")
							.append(PowerSystem.getLocalizedPower(getEnergyConsumption()))
							.append("/t")
							.withStyle(ChatFormatting.GOLD)
			);
		}
	}
}
