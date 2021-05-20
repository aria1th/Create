package com.simibubi.create.foundation.render.effects;

import com.jozufozu.flywheel.backend.ShaderContext;
import com.jozufozu.flywheel.backend.ShaderLoader;
import com.jozufozu.flywheel.backend.core.shader.SingleProgram;
import com.jozufozu.flywheel.backend.loading.ShaderTransformer;
import com.simibubi.create.foundation.render.AllProgramSpecs;

public class EffectsContext extends ShaderContext<SphereFilterProgram> {

	public static final EffectsContext INSTANCE = new EffectsContext();

	public EffectsContext() {
		super(new SingleProgram.SpecLoader<>(SphereFilterProgram::new));
	}

	@Override
	public void load(ShaderLoader loader) {
		transformer = new ShaderTransformer()
				.pushStage(loader::processIncludes);
		loadProgramFromSpec(loader, AllProgramSpecs.CHROMATIC);
	}
}
