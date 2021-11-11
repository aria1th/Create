package com.simibubi.create.foundation.render;

import java.util.stream.Stream;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.SpecMetaRegistry;
import com.jozufozu.flywheel.backend.pipeline.IShaderPipeline;
import com.jozufozu.flywheel.backend.pipeline.InstancingTemplate;
import com.jozufozu.flywheel.backend.pipeline.OneShotTemplate;
import com.jozufozu.flywheel.backend.pipeline.WorldShaderPipeline;
import com.jozufozu.flywheel.backend.source.FileResolution;
import com.jozufozu.flywheel.backend.source.Resolver;
import com.jozufozu.flywheel.core.WorldContext;
import com.jozufozu.flywheel.event.GatherContextEvent;
import com.jozufozu.flywheel.util.ResourceUtil;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionProgram;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreateContexts {
	private static final ResourceLocation CONTRAPTION = Create.asResource("context/contraption");

	public static WorldContext<ContraptionProgram> CWORLD;
	public static WorldContext<ContraptionProgram> STRUCTURE;

	public static void flwInit(GatherContextEvent event) {
		Backend backend = event.getBackend();

		SpecMetaRegistry.register(RainbowDebugStateProvider.INSTANCE);
        FileResolution header = Resolver.INSTANCE.findShader(ResourceUtil.subPath(CONTRAPTION, ".glsl"));

		IShaderPipeline<ContraptionProgram> instancing = new WorldShaderPipeline<>(ContraptionProgram::new, InstancingTemplate.INSTANCE, header);
		IShaderPipeline<ContraptionProgram> structure = new WorldShaderPipeline<>(ContraptionProgram::new, OneShotTemplate.INSTANCE, header);

		CWORLD = backend.register(WorldContext.builder(backend, CONTRAPTION)
				.build(instancing));

		STRUCTURE = backend.register(WorldContext.builder(backend, CONTRAPTION)
				.setSpecStream(() -> Stream.of(AllProgramSpecs.STRUCTURE))
				.build(structure));
	}

}
