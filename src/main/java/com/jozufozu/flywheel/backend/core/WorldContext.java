package com.jozufozu.flywheel.backend.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.ResourceUtil;
import com.jozufozu.flywheel.backend.ShaderContext;
import com.jozufozu.flywheel.backend.ShaderLoader;
import com.jozufozu.flywheel.backend.gl.shader.FogSensitiveProgram;
import com.jozufozu.flywheel.backend.gl.shader.IMultiProgram;
import com.jozufozu.flywheel.backend.gl.shader.ShaderSpecLoader;
import com.jozufozu.flywheel.backend.gl.shader.ShaderType;
import com.jozufozu.flywheel.backend.instancing.MaterialSpec;
import com.jozufozu.flywheel.backend.loading.Shader;

import net.minecraft.util.ResourceLocation;

public class WorldContext<P extends BasicProgram> extends ShaderContext<P> {

	private static final String declaration = "#flwbuiltins";
	private static final Pattern builtinPattern = Pattern.compile(declaration);

	public static final WorldContext<BasicProgram> INSTANCE = new WorldContext<>(new ResourceLocation("create", "context/std"), new FogSensitiveProgram.SpecLoader<>(BasicProgram::new));
	public static final WorldContext<CrumblingProgram> CRUMBLING = new WorldContext<>(new ResourceLocation("create", "context/crumbling"), new FogSensitiveProgram.SpecLoader<>(CrumblingProgram::new));

	private final ShaderSpecLoader<P> loader;

	final Map<ShaderType, ResourceLocation> builtins;

	public WorldContext(ResourceLocation root, ShaderSpecLoader<P> loader) {
		super(root);
		builtins = new EnumMap<>(ShaderType.class);
		builtins.put(ShaderType.FRAGMENT, ResourceUtil.subPath(root, "/builtin.frag"));
		builtins.put(ShaderType.VERTEX, ResourceUtil.subPath(root, "/builtin.vert"));

		this.loader = loader;
	}

	@Override
	public void load(ShaderLoader loader) {
		programs.values().forEach(IMultiProgram::delete);
		programs.clear();

		Backend.allMaterials()
				.stream()
				.map(MaterialSpec::getProgramSpec)
				.forEach(spec -> loadProgramFromSpec(loader, spec));
	}

	@Override
	public void preProcess(ShaderLoader loader, Shader shader) {
		String builtinSrc = loader.getShaderSource(builtins.get(shader.type));

		Matcher matcher = builtinPattern.matcher(shader.getSource());

		if (matcher.find())
			shader.setSource(matcher.replaceFirst(builtinSrc));
		else
			throw new RuntimeException(String.format("%s shader '%s' is missing %s, cannot use in World Context", shader.type.name, shader.name, declaration));
	}

	@Override
	public ShaderSpecLoader<P> getLoader() {
		return loader;
	}
}
