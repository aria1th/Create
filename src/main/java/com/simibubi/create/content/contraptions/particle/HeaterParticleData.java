package com.simibubi.create.content.contraptions.particle;

import java.util.Locale;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.AllParticleTypes;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.particle.ParticleManager.IParticleMetaFactory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HeaterParticleData implements IParticleData, ICustomParticle<HeaterParticleData> {
	public static final IParticleData.IDeserializer<HeaterParticleData> DESERIALIZER =
		new IParticleData.IDeserializer<HeaterParticleData>() {
			@Override
			public HeaterParticleData deserialize(ParticleType<HeaterParticleData> arg0, StringReader reader)
				throws CommandSyntaxException {
				reader.expect(' ');
				float r = reader.readFloat();
				reader.expect(' ');
				float g = reader.readFloat();
				reader.expect(' ');
				float b = reader.readFloat();
				return new HeaterParticleData(r, g, b);
			}

			@Override
			public HeaterParticleData read(ParticleType<HeaterParticleData> type, PacketBuffer buffer) {
				return new HeaterParticleData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
			}
		};

	final float r;
	final float g;
	final float b;

	public HeaterParticleData(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public HeaterParticleData() {
		this(0, 0, 0);
	}

	@Override
	public IDeserializer<HeaterParticleData> getDeserializer() {
		return DESERIALIZER;
	}

	@Override
	public IParticleMetaFactory<HeaterParticleData> getFactory() {
		return HeaterParticle.Factory::new;
	}

	@Override
	public String getParameters() {
		return String.format(Locale.ROOT, "%s %f %f %f", AllParticleTypes.HEATER_PARTICLE.parameter(), r, g, b);
	}

	@Override
	public ParticleType<?> getType() {
		return AllParticleTypes.HEATER_PARTICLE.get();
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeFloat(r);
		buffer.writeFloat(g);
		buffer.writeFloat(b);
	}

}
