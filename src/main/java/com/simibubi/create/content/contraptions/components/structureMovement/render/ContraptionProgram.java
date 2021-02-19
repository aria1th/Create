package com.simibubi.create.content.contraptions.components.structureMovement.render;

import org.lwjgl.opengl.GL20;

import com.simibubi.create.foundation.render.backend.gl.BasicProgram;

import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class ContraptionProgram extends BasicProgram {
    protected final int uLightBoxSize;
    protected final int uLightBoxMin;
    protected final int uModel;

    protected int uLightVolume;

    public ContraptionProgram(ResourceLocation name, int handle) {
        super(name, handle);
        uLightBoxSize = getUniformLocation("uLightBoxSize");
        uLightBoxMin = getUniformLocation("uLightBoxMin");
        uModel = getUniformLocation("uModel");
    }

    @Override
    protected void registerSamplers() {
        super.registerSamplers();
        uLightVolume = setSamplerBinding("uLightVolume", 4);
    }

    public void bind(Matrix4f model, AxisAlignedBB lightVolume) {
        double sizeX = lightVolume.maxX - lightVolume.minX;
        double sizeY = lightVolume.maxY - lightVolume.minY;
        double sizeZ = lightVolume.maxZ - lightVolume.minZ;
        GL20.glUniform3f(uLightBoxSize, (float) sizeX, (float) sizeY, (float) sizeZ);
        GL20.glUniform3f(uLightBoxMin, (float) lightVolume.minX, (float) lightVolume.minY, (float) lightVolume.minZ);
        uploadMatrixUniform(uModel, model);
    }
}
