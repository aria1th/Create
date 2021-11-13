package com.simibubi.create.content.contraptions.fluids.particle;

import java.util.Optional;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.contraptions.particle.ICustomParticleData;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.material.Fluids;

import com.simibubi.create.lib.transfer.fluid.FluidStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class FluidParticleData implements ParticleOptions, ICustomParticleData<FluidParticleData> {

	private ParticleType<FluidParticleData> type;
	private FluidStack fluid;

	public FluidParticleData() {}

	@SuppressWarnings("unchecked")
	public FluidParticleData(ParticleType<?> type, FluidStack fluid) {
		this.type = (ParticleType<FluidParticleData>) type;
		this.fluid = fluid;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ParticleProvider<FluidParticleData> getFactory() {
		return (data, world, x, y, z, vx, vy, vz) -> FluidStackParticle.create(data.type, world, data.fluid, x, y, z,
			vx, vy, vz);
	}

	@Override
	public ParticleType<?> getType() {
		return type;
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buffer) {
		fluid.toBuffer(buffer);
	}

	@Override
	public String writeToString() {
		return Registry.PARTICLE_TYPE.getKey(type) + " " + Registry.FLUID.getKey(fluid.getFluid());
	}

	public static final Codec<FluidStack> FLUID_CODEC = RecordCodecBuilder.create(i -> i.group(
					Registry.FLUID.fieldOf("FluidName")
							.forGetter(FluidStack::getFluid),
					Codec.LONG.fieldOf("Amount")
							.forGetter(FluidStack::getAmount),
					CompoundTag.CODEC.optionalFieldOf("tag")
							.forGetter((fs) -> {
								return Optional.ofNullable(fs.toTag());
							}))
			//Just ignore tags for now
			.apply(i, (f, a, t) -> new FluidStack(f, a)));

	public static final Codec<FluidParticleData> CODEC = RecordCodecBuilder.create(i -> i
		.group(FLUID_CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.FLUID_PARTICLE.get(), fs)));

	public static final Codec<FluidParticleData> BASIN_CODEC = RecordCodecBuilder.create(i -> i
		.group(FLUID_CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), fs)));

	public static final Codec<FluidParticleData> DRIP_CODEC = RecordCodecBuilder.create(i -> i
		.group(FLUID_CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.FLUID_DRIP.get(), fs)));

	public static final ParticleOptions.Deserializer<FluidParticleData> DESERIALIZER =
		new ParticleOptions.Deserializer<FluidParticleData>() {

			// TODO Fluid particles on command
			public FluidParticleData fromCommand(ParticleType<FluidParticleData> particleTypeIn, StringReader reader)
				throws CommandSyntaxException {
				return new FluidParticleData(particleTypeIn, new FluidStack(Fluids.WATER, 1));
			}

			public FluidParticleData fromNetwork(ParticleType<FluidParticleData> particleTypeIn, FriendlyByteBuf buffer) {
				return new FluidParticleData(particleTypeIn, FluidStack.fromBuffer(buffer));
			}
		};

	@Override
	public Deserializer<FluidParticleData> getDeserializer() {
		return DESERIALIZER;
	}

	@Override
	public Codec<FluidParticleData> getCodec(ParticleType<FluidParticleData> type) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return BASIN_CODEC;
		if (type == AllParticleTypes.FLUID_DRIP.get())
			return DRIP_CODEC;
		return CODEC;
	}

}
