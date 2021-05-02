package com.jozufozu.flywheel.backend.core;

import java.util.ArrayList;

import com.jozufozu.flywheel.backend.MaterialTypes;
import com.jozufozu.flywheel.backend.gl.shader.ShaderCallback;
import com.jozufozu.flywheel.backend.instancing.InstancedTileRenderer;
import com.jozufozu.flywheel.backend.instancing.RenderMaterial;
import com.simibubi.create.foundation.render.AllProgramSpecs;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;

public class BasicInstancedTileRenderer extends InstancedTileRenderer<BasicProgram> {
	public static int MAX_ORIGIN_DISTANCE = 100;

	public BlockPos originCoordinate = BlockPos.ZERO;

	@Override
	public void registerMaterials() {
		materials.put(MaterialTypes.TRANSFORMED,
				new RenderMaterial<>(this, AllProgramSpecs.MODEL, TransformedModel::new));
		materials.put(MaterialTypes.ORIENTED, new RenderMaterial<>(this, AllProgramSpecs.ORIENTED, OrientedModel::new));
	}

	@Override
	public BlockPos getOriginCoordinate() {
		return originCoordinate;
	}

	@Override
	public void beginFrame(ActiveRenderInfo info, double cameraX, double cameraY, double cameraZ) {
		int cX = MathHelper.floor(cameraX);
		int cY = MathHelper.floor(cameraY);
		int cZ = MathHelper.floor(cameraZ);

		int dX = Math.abs(cX - originCoordinate.getX());
		int dY = Math.abs(cY - originCoordinate.getY());
		int dZ = Math.abs(cZ - originCoordinate.getZ());

		if (dX > MAX_ORIGIN_DISTANCE || dY > MAX_ORIGIN_DISTANCE || dZ > MAX_ORIGIN_DISTANCE) {

			originCoordinate = new BlockPos(cX, cY, cZ);

			ArrayList<TileEntity> instancedTiles = new ArrayList<>(instances.keySet());
			invalidate();
			instancedTiles.forEach(this::add);
		}

		super.beginFrame(info, cameraX, cameraY, cameraZ);
	}

	@Override
	public void render(RenderType layer, Matrix4f viewProjection, double camX, double camY, double camZ,
					   ShaderCallback<BasicProgram> callback) {
		BlockPos originCoordinate = getOriginCoordinate();

		camX -= originCoordinate.getX();
		camY -= originCoordinate.getY();
		camZ -= originCoordinate.getZ();

		Matrix4f translate = Matrix4f.translate((float) -camX, (float) -camY, (float) -camZ);

		translate.multiplyBackward(viewProjection);

		super.render(layer, translate, camX, camY, camZ, callback);
	}
}
