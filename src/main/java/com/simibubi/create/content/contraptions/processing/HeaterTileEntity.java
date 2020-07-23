package com.simibubi.create.content.contraptions.processing;

import java.util.List;
import java.util.Random;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.components.deployer.DeployerFakePlayer;
import com.simibubi.create.content.contraptions.particle.CubeParticleData;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.ColorHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.EggEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class HeaterTileEntity extends SmartTileEntity {

	private final static int[][] heatParticleColors = {
			{0x3B141A, 0x47141A, 0x7A3B24, 0x854D26},
			{0x2A0103, 0x741B0A, 0xC38246, 0xCCBD78},
			{0x630B03, 0x8B3503, 0xBC8200, 0xCCC849},
			{0x1C6378, 0x4798B5, 0x4DA6C0, 0xBAC8CE}
	};

	private static final int maxHeatCapacity = 10000;

	private int remainingBurnTime;
	private FuelType activeFuel;
	
	// Rendering state
	float rot, speed;

	public HeaterTileEntity(TileEntityType<? extends HeaterTileEntity> tileEntityTypeIn) {
		super(tileEntityTypeIn);
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		setLazyTickRate(40);
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isRemote) {
			tickRotation();
		}

		spawnParticles(getHeatLevel());

		if (remainingBurnTime <= 0) {
			return;
		}
		remainingBurnTime--;
		if (remainingBurnTime == 0)
			if (activeFuel == FuelType.SPECIAL) {
				activeFuel = FuelType.NORMAL;
				remainingBurnTime = maxHeatCapacity / 2;
				updateHeatLevel();
			} else {
				activeFuel = FuelType.NONE;
				updateHeatLevel();
			}
		markDirty();
	}
	
	private static final float MAX_ROT_SPEED = 5;
	private static final float ROT_DAMPING = 15;
	
	private void tickRotation() {
		ClientPlayerEntity player = Minecraft.getInstance().player;
		Angle target;
		if (player == null) {
			target = new Angle(360, 0);
		} else {
			double dx = player.getX() - (getPos().getX() + 0.5);
			double dz = player.getZ() - (getPos().getZ() + 0.5);
			target = new Angle(360, (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI + 90));
		}

		Angle current = new Angle(360, rot);
		float diff = new Angle(180, current.get() - target.get()).get();
		if (diff > 0.1 || diff < -0.1) {
			// Inverse function https://www.desmos.com/calculator/kiaberb6sf
			speed = MAX_ROT_SPEED + (-MAX_ROT_SPEED / ((Math.abs(diff) / ROT_DAMPING) + 1));
			if (diff > 0) {
				current.add(-Math.min(diff, speed));
				speed = Math.min(diff, speed);
			} else {
				current.add(Math.min(-diff, speed));
				speed = Math.min(-diff, -speed);
			}
		} else {
			speed = 0;
		}
		
		rot = current.get();
	}
	
	// From EnderIO with <3
	private static class Angle {
		private final float offset;
		private float a;

		Angle(float offset, float a) {
			this.offset = offset;
			set(a);
		}

		void set(float a) {
			while (a >= offset) {
				a -= 360;
			}
			while (a < (offset - 360)) {
				a += 360;
			}
			this.a = a;
		}

		void add(float b) {
			set(a + b);
		}

		float get() {
			return a;
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		//using lazy ticks to transition between kindled and fading, this doesn't need to happen instantly at the threshold
		updateHeatLevel();
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		compound.putInt("fuelLevel", activeFuel.ordinal());
		compound.putInt("burnTimeRemaining", remainingBurnTime);
		return super.write(compound);
	}

	@Override
	public void read(CompoundNBT compound) {
		activeFuel = FuelType.values()[compound.getInt("fuelLevel")];
		remainingBurnTime = compound.getInt("burnTimeRemaining");
		super.read(compound);
		updateHeatLevel();
	}

	/**
	 * @return true if the heater updated its burn time and a item should be consumed
	 */
	boolean tryUpdateFuel(ItemStack itemStack, PlayerEntity player) {
		FuelType newFuel = FuelType.NONE;
		int burnTick = ForgeHooks.getBurnTime(itemStack);
		if (burnTick > 0)
			newFuel = FuelType.NORMAL;
		if (itemStack.getItem() == AllItems.FUEL_PELLET.get()) {
			burnTick = 1000;
			newFuel = FuelType.SPECIAL;
		}

		if (newFuel == FuelType.NONE || newFuel.ordinal() < activeFuel.ordinal())
			return false;

		if (newFuel == activeFuel) {
			if (remainingBurnTime + burnTick > maxHeatCapacity && player instanceof DeployerFakePlayer)
				return false;

			remainingBurnTime = MathHelper.clamp(remainingBurnTime + burnTick, 0, maxHeatCapacity);
		} else {
			activeFuel = newFuel;
			remainingBurnTime = burnTick;
		}

		updateHeatLevel();
		return true;
	}

	public HeaterBlock.HeatLevel getHeatLevel() {
		return HeaterBlock.getHeaterLevel(getBlockState());
	}

	private void updateHeatLevel() {
		switch (activeFuel) {
			case SPECIAL:
				HeaterBlock.setBlazeLevel(world, pos, HeaterBlock.HeatLevel.SEETHING);
				break;
			case NORMAL:
				boolean lowPercent = (double) remainingBurnTime / maxHeatCapacity < 0.1;
				HeaterBlock.setBlazeLevel(world, pos, lowPercent ? HeaterBlock.HeatLevel.FADING : HeaterBlock.HeatLevel.KINDLED);
				break;
			case NONE:
				HeaterBlock.setBlazeLevel(world, pos, HeaterBlock.HeatLevel.SMOULDERING);
		}
	}

	private void spawnParticles(HeaterBlock.HeatLevel heatLevel) {
		if (world == null)
			return;

		if (heatLevel == HeaterBlock.HeatLevel.NONE)
			return;

		Random r = world.getRandom();
		if (heatLevel == HeaterBlock.HeatLevel.SMOULDERING) {
			if (r.nextDouble() > 0.25)
				return;

			Vec3d color = randomColor(heatLevel);
			spawnParticle(new CubeParticleData((float) color.x,(float)  color.y,(float)  color.z, 0.03F, 15), 0.015, 0.1);
		} else if (heatLevel == HeaterBlock.HeatLevel.FADING) {
			if (r.nextDouble() > 0.5)
				return;

			Vec3d color = randomColor(heatLevel);
			spawnParticle(new CubeParticleData((float) color.x,(float)  color.y,(float)  color.z, 0.035F, 18), 0.03, 0.15);
		} else if (heatLevel == HeaterBlock.HeatLevel.KINDLED) {
			Vec3d color = randomColor(heatLevel);
			spawnParticle(new CubeParticleData((float) color.x,(float)  color.y,(float)  color.z, 0.04F, 21), 0.05, 0.2);
		}else if (heatLevel == HeaterBlock.HeatLevel.SEETHING) {
			for (int i = 0; i < 2; i++) {
				if (r.nextDouble() > 0.6)
					return;
				Vec3d color = randomColor(heatLevel);
				spawnParticle(new CubeParticleData((float) color.x,(float)  color.y,(float)  color.z, 0.045F, 24), 0.06, 0.22);
			}
		}
	}

	private void spawnParticle(IParticleData particleData, double speed, double spread) {
		Random random = world.getRandom();

		world.addOptionalParticle(
				particleData,
				(double) pos.getX() + 0.5D + (random.nextDouble() * 2.0 - 1D) * spread,
				(double) pos.getY() + 0.6D + random.nextDouble() / 10.0,
				(double) pos.getZ() + 0.5D + (random.nextDouble() * 2.0 - 1D) * spread,
				0.0D,
				speed,
				0.0D);
	}

	private static Vec3d randomColor(HeaterBlock.HeatLevel heatLevel) {
		if (heatLevel == HeaterBlock.HeatLevel.NONE)
			return new Vec3d(0,0,0);

		return ColorHelper.getRGB(heatParticleColors[heatLevel.ordinal()-1][(int) (Math.random()*4)]);
	}

	@SubscribeEvent
	public static void eggsGetEaten(ProjectileImpactEvent.Throwable event) {
		if (!(event.getThrowable() instanceof EggEntity))
			return;

		if (event.getRayTraceResult().getType() != RayTraceResult.Type.BLOCK)
			return;

		TileEntity tile = event.getThrowable().world.getTileEntity(new BlockPos(event.getRayTraceResult().getHitVec()));
		if (!(tile instanceof HeaterTileEntity)) {
			return;
		}

		event.setCanceled(true);
		event.getThrowable().setMotion(Vec3d.ZERO);
		event.getThrowable().remove();

		HeaterTileEntity heater = (HeaterTileEntity) tile;
		if (heater.activeFuel != FuelType.SPECIAL) {
			heater.activeFuel = FuelType.NORMAL;
			heater.remainingBurnTime = MathHelper.clamp(heater.remainingBurnTime + 80, 0, maxHeatCapacity);
			heater.markDirty();
		}

		World world = event.getThrowable().world;
		if (world.isRemote)
			return;

		world.playSound(null, heater.getPos(), AllSoundEvents.BLAZE_MUNCH.get(), SoundCategory.BLOCKS, .5F, 1F);


	}

	private enum FuelType {
		NONE,
		NORMAL,
		SPECIAL
	}
}
