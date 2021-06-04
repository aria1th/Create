package com.jozufozu.flywheel.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.jozufozu.flywheel.Flywheel;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.ResourceUtil;
import com.jozufozu.flywheel.backend.ShaderContext;
import com.jozufozu.flywheel.backend.ShaderSources;
import com.jozufozu.flywheel.backend.gl.shader.ShaderType;
import com.jozufozu.flywheel.backend.instancing.MaterialManager;
import com.jozufozu.flywheel.backend.instancing.MaterialSpec;
import com.jozufozu.flywheel.backend.loading.InstancedArraysTemplate;
import com.jozufozu.flywheel.backend.loading.Program;
import com.jozufozu.flywheel.backend.loading.ProgramTemplate;
import com.jozufozu.flywheel.backend.loading.Shader;
import com.jozufozu.flywheel.backend.loading.ShaderLoadingException;
import com.jozufozu.flywheel.backend.loading.ShaderTransformer;
import com.jozufozu.flywheel.core.shader.ExtensibleGlProgram;
import com.jozufozu.flywheel.core.shader.IMultiProgram;
import com.jozufozu.flywheel.core.shader.StateSensitiveMultiProgram;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import com.jozufozu.flywheel.core.shader.spec.ProgramSpec;
import com.jozufozu.flywheel.util.WorldAttached;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorld;

public class WorldContext<P extends WorldProgram> extends ShaderContext<P> {

	private static final String declaration = "#flwbuiltins";
	private static final Pattern builtinPattern = Pattern.compile(declaration);

	public static final WorldContext<WorldProgram> INSTANCE = new WorldContext<>(new ResourceLocation(Flywheel.ID, "context/world"), WorldProgram::new);
	public static final WorldContext<CrumblingProgram> CRUMBLING = new WorldContext<>(new ResourceLocation(Flywheel.ID, "context/crumbling"), CrumblingProgram::new);

	protected final ResourceLocation name;
	protected Supplier<Stream<ResourceLocation>> specStream;
	protected TemplateFactory templateFactory;

	private final WorldAttached<MaterialManager<P>> materialManager = new WorldAttached<>($ -> new MaterialManager<>(this));

	private final Map<ShaderType, ResourceLocation> builtins = new EnumMap<>(ShaderType.class);
	private final Map<ShaderType, String> builtinSources = new EnumMap<>(ShaderType.class);

	private final ExtensibleGlProgram.Factory<P> factory;

	public WorldContext(ResourceLocation root, ExtensibleGlProgram.Factory<P> factory) {
		this.factory = factory;
		this.name = root;
		builtins.put(ShaderType.FRAGMENT, ResourceUtil.subPath(root, "/builtin.frag"));
		builtins.put(ShaderType.VERTEX, ResourceUtil.subPath(root, "/builtin.vert"));

		specStream = () -> Backend.allMaterials()
				.stream()
				.map(MaterialSpec::getProgramName);

		templateFactory = InstancedArraysTemplate::new;
	}

	public MaterialManager<P> getMaterialManager(IWorld world) {
		return materialManager.get(world);
	}

	public WorldContext<P> setSpecStream(Supplier<Stream<ResourceLocation>> specStream) {
		this.specStream = specStream;
		return this;
	}

	public WorldContext<P> setTemplateFactory(TemplateFactory templateFactory) {
		this.templateFactory = templateFactory;
		return this;
	}

	@Override
	protected IMultiProgram<P> loadSpecInternal(ShaderSources loader, ProgramSpec spec) {
		return new StateSensitiveMultiProgram<>(loader, factory, this, spec);
	}

	protected ProgramTemplate template;

	@Override
	public void load(ShaderSources loader) {
		programs.values().forEach(IMultiProgram::delete);
		programs.clear();

		Backend.log.info("Loading context '{}'", name);

		try {
			builtins.forEach((type, resourceLocation) -> builtinSources.put(type, loader.getShaderSource(resourceLocation)));
		} catch (ShaderLoadingException e) {
			loader.notifyError();

			Backend.log.error(String.format("Could not find builtin: %s", e.getMessage()));

			return;
		}

		template = templateFactory.create(loader);
		transformer = new ShaderTransformer()
				.pushStage(this::injectBuiltins)
				.pushStage(Shader::processIncludes)
				.pushStage(Shader::parseStructs)
				.pushStage(template)
				.pushStage(Shader::processIncludes);

		specStream.get()
				.map(Backend::getSpec)
				.forEach(spec -> loadProgramFromSpec(loader, spec));
	}

	@Override
	protected void preLink(Program program) {
		template.attachAttributes(program);
	}

	/**
	 * Replace #flwbuiltins with whatever expansion this context provides for the given shader.
	 */
	public void injectBuiltins(Shader shader) {
		Matcher matcher = builtinPattern.matcher(shader.getSource());

		if (matcher.find())
			shader.setSource(matcher.replaceFirst(builtinSources.get(shader.type)));
		else
			throw new ShaderLoadingException(String.format("%s is missing %s, cannot use in World Context", shader.type.name, declaration));
	}

	public interface TemplateFactory {
		ProgramTemplate create(ShaderSources loader);
	}
}
