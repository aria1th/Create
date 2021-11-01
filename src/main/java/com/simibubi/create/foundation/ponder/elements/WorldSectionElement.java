package com.simibubi.create.foundation.ponder.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.Selection;
import com.simibubi.create.foundation.render.Compartment;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperByteBufferCache;
import com.simibubi.create.foundation.render.TileEntityRenderHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;

public class WorldSectionElement extends AnimatedSceneElement {

	public static final Compartment<Pair<Integer, Integer>> DOC_WORLD_SECTION = new Compartment<>();

	List<BlockEntity> renderedTileEntities;
	List<BlockEntity> tickableTileEntities;
	Selection section;
	boolean redraw;

	Vec3 prevAnimatedOffset = Vec3.ZERO;
	Vec3 animatedOffset = Vec3.ZERO;
	Vec3 prevAnimatedRotation = Vec3.ZERO;
	Vec3 animatedRotation = Vec3.ZERO;
	Vec3 centerOfRotation = Vec3.ZERO;
	Vec3 stabilizationAnchor = null;

	BlockPos selectedBlock;

	public WorldSectionElement() {}

	public WorldSectionElement(Selection section) {
		this.section = section.copy();
		centerOfRotation = section.getCenter();
	}

	public void mergeOnto(WorldSectionElement other) {
		setVisible(false);
		if (other.isEmpty())
			other.set(section);
		else
			other.add(section);
	}

	public void set(Selection selection) {
		applyNewSelection(selection.copy());
	}

	public void add(Selection toAdd) {
		applyNewSelection(this.section.add(toAdd));
	}

	public void erase(Selection toErase) {
		applyNewSelection(this.section.substract(toErase));
	}

	private void applyNewSelection(Selection selection) {
		this.section = selection;
		queueRedraw();
	}

	public void setCenterOfRotation(Vec3 center) {
		centerOfRotation = center;
	}

	public void stabilizeRotation(Vec3 anchor) {
		stabilizationAnchor = anchor;
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		resetAnimatedTransform();
		resetSelectedBlock();
	}

	public void selectBlock(BlockPos pos) {
		selectedBlock = pos;
	}

	public void resetSelectedBlock() {
		selectedBlock = null;
	}

	public void resetAnimatedTransform() {
		prevAnimatedOffset = Vec3.ZERO;
		animatedOffset = Vec3.ZERO;
		prevAnimatedRotation = Vec3.ZERO;
		animatedRotation = Vec3.ZERO;
	}

	public void queueRedraw() {
		redraw = true;
	}

	public boolean isEmpty() {
		return section == null;
	}

	public void setEmpty() {
		section = null;
	}

	public void setAnimatedRotation(Vec3 eulerAngles, boolean force) {
		this.animatedRotation = eulerAngles;
		if (force)
			prevAnimatedRotation = animatedRotation;
	}

	public Vec3 getAnimatedRotation() {
		return animatedRotation;
	}

	public void setAnimatedOffset(Vec3 offset, boolean force) {
		this.animatedOffset = offset;
		if (force)
			prevAnimatedOffset = animatedOffset;
	}

	public Vec3 getAnimatedOffset() {
		return animatedOffset;
	}

	@Override
	public boolean isVisible() {
		return super.isVisible() && !isEmpty();
	}

	class WorldSectionRayTraceResult {
		Vec3 actualHitVec;
		BlockPos worldPos;
	}

	public Pair<Vec3, BlockPos> rayTrace(PonderWorld world, Vec3 source, Vec3 target) {
		world.setMask(this.section);
		Vec3 transformedTarget = reverseTransformVec(target);
		BlockHitResult rayTraceBlocks = world.clip(new ClipContext(reverseTransformVec(source),
			transformedTarget, Block.OUTLINE, Fluid.NONE, null));
		world.clearMask();

		if (rayTraceBlocks == null)
			return null;
		if (rayTraceBlocks.getLocation() == null)
			return null;

		double t = rayTraceBlocks.getLocation()
			.subtract(transformedTarget)
			.lengthSqr()
			/ source.subtract(target)
				.lengthSqr();
		Vec3 actualHit = VecHelper.lerp((float) t, target, source);
		return Pair.of(actualHit, rayTraceBlocks.getBlockPos());
	}

	private Vec3 reverseTransformVec(Vec3 in) {
		float pt = AnimationTickHolder.getPartialTicks();
		in = in.subtract(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = section.getCenter();
			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			in = in.subtract(centerOfRotation);
			in = VecHelper.rotate(in, -rotX, Axis.X);
			in = VecHelper.rotate(in, -rotZ, Axis.Z);
			in = VecHelper.rotate(in, -rotY, Axis.Y);
			in = in.add(centerOfRotation);
			if (stabilizationAnchor != null) {
				in = in.subtract(stabilizationAnchor);
				in = VecHelper.rotate(in, rotX, Axis.X);
				in = VecHelper.rotate(in, rotZ, Axis.Z);
				in = VecHelper.rotate(in, rotY, Axis.Y);
				in = in.add(stabilizationAnchor);
			}
		}
		return in;
	}

	public void transformMS(PoseStack ms, float pt) {
		MatrixTransformStack.of(ms)
			.translate(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = section.getCenter();
			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			MatrixTransformStack.of(ms)
				.translate(centerOfRotation)
				.rotateX(rotX)
				.rotateZ(rotZ)
				.rotateY(rotY)
				.translateBack(centerOfRotation);
			if (stabilizationAnchor != null) {
				MatrixTransformStack.of(ms)
					.translate(stabilizationAnchor)
					.rotateX(-rotX)
					.rotateZ(-rotZ)
					.rotateY(-rotY)
					.translateBack(stabilizationAnchor);
			}
		}
	}

	public void tick(PonderScene scene) {
		prevAnimatedOffset = animatedOffset;
		prevAnimatedRotation = animatedRotation;
		if (!isVisible())
			return;
		loadTEsIfMissing(scene.getWorld());
		renderedTileEntities.removeIf(te -> scene.getWorld()
			.getBlockEntity(te.getBlockPos()) != te);
		tickableTileEntities.removeIf(te -> scene.getWorld()
			.getBlockEntity(te.getBlockPos()) != te);
		tickableTileEntities.forEach(te -> {
			if (te instanceof TickableBlockEntity)
				((TickableBlockEntity) te).tick();
		});
	}

	@Override
	public void whileSkipping(PonderScene scene) {
		if (redraw) {
			renderedTileEntities = null;
			tickableTileEntities = null;
		}
		redraw = false;
	}

	protected void loadTEsIfMissing(PonderWorld world) {
		if (renderedTileEntities != null)
			return;
		tickableTileEntities = new ArrayList<>();
		renderedTileEntities = new ArrayList<>();
		section.forEach(pos -> {
			BlockEntity tileEntity = world.getBlockEntity(pos);
			if (tileEntity == null)
				return;
			tickableTileEntities.add(tileEntity);
			renderedTileEntities.add(tileEntity);
			tileEntity.clearCache();
		});
	}

	@Override
	protected void renderLayer(PonderWorld world, MultiBufferSource buffer, RenderType type, PoseStack ms, float fade,
		float pt) {
		transformMS(ms, pt);
		renderStructure(world, ms, buffer, type, fade);
	}

	@Override
	public void renderFirst(PonderWorld world, MultiBufferSource buffer, PoseStack ms, float fade, float pt) {
		int light = -1;
		if (fade != 1)
			light = (int) (Mth.lerp(fade, 5, 14));
		if (redraw) {
			renderedTileEntities = null;
			tickableTileEntities = null;
		}
		transformMS(ms, pt);
		world.pushFakeLight(light);
		renderTileEntities(world, ms, buffer, pt);
		world.popLight();

		Map<BlockPos, Integer> blockBreakingProgressions = world.getBlockBreakingProgressions();
		PoseStack overlayMS = null;

		for (Entry<BlockPos, Integer> entry : blockBreakingProgressions.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!section.test(pos))
				continue;
			if (overlayMS == null) {
				overlayMS = new PoseStack();
				world.scene.getTransform()
					.apply(overlayMS, pt, true);
				transformMS(overlayMS, pt);
			}
			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());
			VertexConsumer builder = new SheetedDecalTextureGenerator(
				buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(entry.getValue())),
					overlayMS.last().pose(),
					overlayMS.last().normal());
			Minecraft.getInstance()
				.getBlockRenderer()
				.renderModel(world.getBlockState(pos), pos, world, ms, builder, true, world.random, EmptyModelData.INSTANCE);
			ms.popPose();
		}
	}

	protected void renderStructure(PonderWorld world, PoseStack ms, MultiBufferSource buffer, RenderType type,
		float fade) {
		SuperByteBufferCache bufferCache = CreateClient.BUFFER_CACHE;
		int code = hashCode() ^ world.hashCode();

		Pair<Integer, Integer> key = Pair.of(code, RenderType.chunkBufferLayers()
				.indexOf(type));
		if (redraw)
			bufferCache.invalidate(DOC_WORLD_SECTION, key);
		SuperByteBuffer contraptionBuffer =
				bufferCache.get(DOC_WORLD_SECTION, key, () -> buildStructureBuffer(world, type));
		if (contraptionBuffer.isEmpty())
			return;

		int light = lightCoordsFromFade(fade);
		contraptionBuffer.light(light)
			.renderInto(ms, buffer.getBuffer(type));
	}

	@Override
	protected void renderLast(PonderWorld world, MultiBufferSource buffer, PoseStack ms, float fade, float pt) {
		redraw = false;
		if (selectedBlock == null)
			return;
		BlockState blockState = world.getBlockState(selectedBlock);
		if (blockState.isAir())
			return;
		VoxelShape shape =
			blockState.getShape(world, selectedBlock, CollisionContext.of(Minecraft.getInstance().player));
		if (shape.isEmpty())
			return;

		ms.pushPose();
		transformMS(ms, pt);
		RenderSystem.disableTexture();
		LevelRenderer.renderLineBox(ms, buffer.getBuffer(RenderType.lines()), shape.bounds()
			.move(selectedBlock), 1, 1, 1, 0.6f);
		ms.popPose();
	}

	private void renderTileEntities(PonderWorld world, PoseStack ms, MultiBufferSource buffer, float pt) {
		loadTEsIfMissing(world);
		TileEntityRenderHelper.renderTileEntities(world, renderedTileEntities, ms, buffer, pt);
	}

	private SuperByteBuffer buildStructureBuffer(PonderWorld world, RenderType layer) {
		ForgeHooksClient.setRenderLayer(layer);
		PoseStack ms = new PoseStack();
		BlockRenderDispatcher dispatcher = Minecraft.getInstance()
			.getBlockRenderer();
		ModelBlockRenderer blockRenderer = dispatcher.getModelRenderer();
		Random random = new Random();
		BufferBuilder builder = new BufferBuilder(DefaultVertexFormat.BLOCK.getIntegerSize());
		builder.begin(GL11.GL_QUADS, DefaultVertexFormat.BLOCK);
		world.setMask(this.section);

		section.forEach(pos -> {
			BlockState state = world.getBlockState(pos);
			FluidState ifluidstate = world.getFluidState(pos);

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			if (state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED && state.getBlock() != Blocks.AIR
				&& ItemBlockRenderTypes.canRenderInLayer(state, layer)) {
				BlockEntity tileEntity = world.getBlockEntity(pos);
				blockRenderer.renderModel(world, dispatcher.getBlockModel(state), state, pos, ms, builder, true,
					random, 42, OverlayTexture.NO_OVERLAY,
					tileEntity != null ? tileEntity.getModelData() : EmptyModelData.INSTANCE);
			}

			if (!ifluidstate.isEmpty() && ItemBlockRenderTypes.canRenderInLayer(ifluidstate, layer))
				dispatcher.renderLiquid(pos, world, builder, ifluidstate);

			ms.popPose();
		});

		world.clearMask();
		builder.end();
		ForgeHooksClient.setRenderLayer(null);
		return new SuperByteBuffer(builder);
	}

}
