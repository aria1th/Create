package com.jozufozu.flywheel.backend.instancing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jozufozu.flywheel.backend.RenderWork;
import com.jozufozu.flywheel.backend.core.BufferedModel;
import com.jozufozu.flywheel.backend.core.PartialModel;
import com.jozufozu.flywheel.backend.core.shader.ShaderCallback;
import com.jozufozu.flywheel.backend.core.shader.WorldProgram;
import com.jozufozu.flywheel.backend.gl.attrib.VertexFormat;
import com.jozufozu.flywheel.util.BufferBuilderReader;
import com.jozufozu.flywheel.util.RenderUtil;
import com.jozufozu.flywheel.util.VirtualEmptyModelData;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;

public class RenderMaterial<P extends WorldProgram, D extends InstanceData> {

	protected final InstancedTileRenderer<P> renderer;
	protected final Cache<Object, InstancedModel<D>> models;
	protected final MaterialSpec<D> spec;

	public RenderMaterial(InstancedTileRenderer<P> renderer, MaterialSpec<D> spec) {
		this.renderer = renderer;
		this.spec = spec;

		this.models = CacheBuilder.newBuilder()
				.removalListener(notification -> {
					InstancedModel<?> model = (InstancedModel<?>) notification.getValue();
					RenderWork.enqueue(model::delete);
				})
				.build();
	}

	public void render(RenderType layer, Matrix4f projection, double camX, double camY, double camZ) {
		render(layer, projection, camX, camY, camZ, null);
	}

	public void render(RenderType layer, Matrix4f viewProjection, double camX, double camY, double camZ, ShaderCallback<P> setup) {
		if (!(layer == RenderType.getCutoutMipped())) return;

		P program = renderer.context.getProgram(this.spec.getProgramSpec());
		program.bind();
		program.uploadViewProjection(viewProjection);
		program.uploadCameraPos(camX, camY, camZ);

		if (setup != null) setup.call(program);

		makeRenderCalls();
	}

	public void delete() {
		//runOnAll(InstancedModel::delete);
		models.invalidateAll();
	}

	protected void makeRenderCalls() {
		runOnAll(InstancedModel::render);
	}

	public void runOnAll(Consumer<InstancedModel<D>> f) {
		for (InstancedModel<D> model : models.asMap().values()) {
			f.accept(model);
		}
	}

	public InstancedModel<D> getModel(PartialModel partial, BlockState referenceState) {
		return get(partial, () -> buildModel(partial.get(), referenceState));
	}

	public InstancedModel<D> getModel(PartialModel partial, BlockState referenceState, Direction dir) {
		return getModel(partial, referenceState, dir, RenderUtil.rotateToFace(dir));
	}

	public InstancedModel<D> getModel(PartialModel partial, BlockState referenceState, Direction dir, Supplier<MatrixStack> modelTransform) {
		return get(Pair.of(dir, partial),
				() -> buildModel(partial.get(), referenceState, modelTransform.get()));
	}

	public InstancedModel<D> getModel(BlockState toRender) {
		return get(toRender, () -> buildModel(toRender));
	}

	public InstancedModel<D> get(Object key, Supplier<InstancedModel<D>> supplier) {
		try {
			return models.get(key, supplier::get);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}

	private InstancedModel<D> buildModel(BlockState renderedState) {
		BlockRendererDispatcher dispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
		return buildModel(dispatcher.getModelForState(renderedState), renderedState);
	}

	private InstancedModel<D> buildModel(IBakedModel model, BlockState renderedState) {
		return buildModel(model, renderedState, new MatrixStack());
	}

	private InstancedModel<D> buildModel(IBakedModel model, BlockState referenceState, MatrixStack ms) {
		BufferBuilderReader reader = new BufferBuilderReader(getBufferBuilder(model, referenceState, ms));

		VertexFormat format = spec.getModelFormat();
		int vertexCount = reader.getVertexCount();

		ByteBuffer to = ByteBuffer.allocate(vertexCount * format.getStride());
		to.order(ByteOrder.nativeOrder());

		for (int i = 0; i < vertexCount; i++) {
			to.putFloat(reader.getX(i));
			to.putFloat(reader.getY(i));
			to.putFloat(reader.getZ(i));

			to.put(reader.getNX(i));
			to.put(reader.getNY(i));
			to.put(reader.getNZ(i));

			to.putFloat(reader.getU(i));
			to.putFloat(reader.getV(i));
		}

		to.rewind();

		BufferedModel bufferedModel = new BufferedModel(format, to, vertexCount);

		return new InstancedModel<>(bufferedModel, renderer, spec.getInstanceFormat(), spec.getInstanceFactory());
	}

	private static final Direction[] dirs;

	static {
		Direction[] directions = Direction.values();

		dirs = Arrays.copyOf(directions, directions.length + 1);
	}

	public static BufferBuilder getBufferBuilder(IBakedModel model, BlockState referenceState, MatrixStack ms) {
		Minecraft mc = Minecraft.getInstance();
		BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
		BlockModelRenderer blockRenderer = dispatcher.getBlockModelRenderer();
		BufferBuilder builder = new BufferBuilder(512);

//		BakedQuadWrapper quadReader = new BakedQuadWrapper();
//
//		IModelData modelData = model.getModelData(mc.world, BlockPos.ZERO.up(255), referenceState, VirtualEmptyModelData.INSTANCE);
//		List<BakedQuad> quads = Arrays.stream(dirs)
//				.flatMap(dir -> model.getQuads(referenceState, dir, mc.world.rand, modelData).stream())
//				.collect(Collectors.toList());

		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		blockRenderer.renderModel(mc.world, model, referenceState, BlockPos.ZERO.up(255), ms, builder, true,
				mc.world.rand, 42, OverlayTexture.DEFAULT_UV, VirtualEmptyModelData.INSTANCE);
		builder.finishDrawing();
		return builder;
	}

}
