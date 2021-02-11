package com.simibubi.create.foundation.render.instancing;


import com.simibubi.create.foundation.render.BufferedModel;
import com.simibubi.create.foundation.render.RenderMath;
import com.simibubi.create.foundation.render.gl.GlVertexArray;
import com.simibubi.create.foundation.render.gl.attrib.CommonAttributes;
import com.simibubi.create.foundation.render.gl.attrib.VertexFormat;
import com.simibubi.create.foundation.render.gl.backend.Backend;
import com.simibubi.create.foundation.render.gl.GlBuffer;
import net.minecraft.client.renderer.BufferBuilder;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class InstancedModel<D extends InstanceData> extends BufferedModel {
    public static final VertexFormat FORMAT = new VertexFormat(CommonAttributes.POSITION, CommonAttributes.NORMAL, CommonAttributes.UV);

    protected GlVertexArray vao;
    protected GlBuffer instanceVBO;
    protected int glBufferSize = -1;
    protected int glInstanceCount = 0;

    protected final ArrayList<InstanceKey<D>> keys = new ArrayList<>();
    protected final ArrayList<D> data = new ArrayList<>();
    protected int minIndexChanged = -1;
    protected int maxIndexChanged = -1;

    public InstancedModel(BufferBuilder buf) {
        super(buf);
    }

    @Override
    protected void init() {
        vao = new GlVertexArray();
        instanceVBO = new GlBuffer(GL20.GL_ARRAY_BUFFER);

        vao.bind();
        super.init();
        vao.unbind();
    }

    @Override
    protected void initModel() {
        super.initModel();
        setupAttributes();
    }

    public int instanceCount() {
        return data.size();
    }

    public boolean isEmpty() {
        return instanceCount() == 0;
    }

    protected void deleteInternal() {
        keys.forEach(InstanceKey::invalidate);
        super.deleteInternal();

        instanceVBO.delete();
        vao.delete();
    }

    protected abstract D newInstance();

    public synchronized void deleteInstance(InstanceKey<D> key) {
        verifyKey(key);

        int index = key.index;

        keys.remove(index);
        data.remove(index);

        for (int i = index; i < keys.size(); i++) {
            keys.get(i).index--;
        }

        maxIndexChanged = keys.size() - 1;
        markIndexChanged(Math.min(maxIndexChanged, index));

        key.invalidate();
    }

    public synchronized void modifyInstance(InstanceKey<D> key, Consumer<D> edit) {
        verifyKey(key);

        D data = this.data.get(key.index);

        edit.accept(data);

        markIndexChanged(key.index);
    }

    public synchronized InstanceKey<D> setupInstance(Consumer<D> setup) {
        D instanceData = newInstance();
        setup.accept(instanceData);

        InstanceKey<D> key = new InstanceKey<>(this, data.size());
        data.add(instanceData);
        keys.add(key);

        markIndexChanged(key.index);

        return key;
    }

    protected void doRender() {
        vao.bind();
        renderSetup();
        GL31.glDrawArraysInstanced(GL11.GL_QUADS, 0, vertexCount, glInstanceCount);
        vao.unbind();
    }

    protected void renderSetup() {
        if (minIndexChanged < 0 || data.isEmpty()) return;

        VertexFormat instanceFormat = getInstanceFormat();

        int stride = instanceFormat.getStride();
        int newInstanceCount = instanceCount();
        int instanceSize = RenderMath.nextPowerOf2((newInstanceCount + 1) * stride);

        instanceVBO.bind();

        // this probably changes enough that it's not worth reallocating the entire buffer every time.
        if (instanceSize > glBufferSize) {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceSize, GL15.GL_STATIC_DRAW);
            glBufferSize = instanceSize;
            minIndexChanged = 0;
            maxIndexChanged = newInstanceCount - 1;
        }

        int offset = minIndexChanged * stride;
        int length = (1 + maxIndexChanged - minIndexChanged) * stride;

        Backend.mapBuffer(GL15.GL_ARRAY_BUFFER, offset, length, buffer -> {
            for (int i = minIndexChanged; i <= maxIndexChanged; i++) {
                data.get(i).write(buffer);
            }
        });

        if (newInstanceCount < glInstanceCount) {
            int clearFrom = (maxIndexChanged + 1) * stride;
            int clearTo = (glInstanceCount) * stride;
            Backend.mapBuffer(GL15.GL_ARRAY_BUFFER, clearFrom, clearTo - clearFrom, buffer -> {
                for (int i = clearFrom; i < clearTo; i++) {
                    buffer.put((byte) 0);
                }
            });
        }

        glInstanceCount = newInstanceCount;

        int staticAttributes = getModelFormat().getShaderAttributeCount();
        instanceFormat.informAttributes(staticAttributes);

        for (int i = 0; i < instanceFormat.getShaderAttributeCount(); i++) {
            GL33.glVertexAttribDivisor(i + staticAttributes, 1);
        }

        instanceVBO.unbind();

        minIndexChanged = -1;
        maxIndexChanged = -1;
    }

    protected void markIndexChanged(int index) {
        if (minIndexChanged < 0) {
            minIndexChanged = index;
        } else if (index < minIndexChanged) {
            minIndexChanged = index;
        }

        if (maxIndexChanged < 0) {
            maxIndexChanged = index;
        } else if (index > maxIndexChanged) {
            maxIndexChanged = index;
        }
    }

    protected final void verifyKey(InstanceKey<D> key) {
        if (key.model != this) throw new IllegalStateException("Provided key does not belong to model.");

        if (!key.isValid()) throw new IllegalStateException("Provided key has been invalidated.");

        if (key.index >= data.size()) throw new IndexOutOfBoundsException("Key points out of bounds. (" + key.index + " > " + (data.size() - 1) + ")");

        if (keys.get(key.index) != key) throw new IllegalStateException("Key desync!!");
    }

    @Override
    protected void copyVertex(ByteBuffer constant, int i) {
        constant.putFloat(getX(template, i));
        constant.putFloat(getY(template, i));
        constant.putFloat(getZ(template, i));

        constant.put(getNX(template, i));
        constant.put(getNY(template, i));
        constant.put(getNZ(template, i));

        constant.putFloat(getU(template, i));
        constant.putFloat(getV(template, i));
    }

    @Override
    protected VertexFormat getModelFormat() {
        return FORMAT;
    }

    protected abstract VertexFormat getInstanceFormat();

    protected int getTotalShaderAttributeCount() {
        return getInstanceFormat().getShaderAttributeCount() + super.getTotalShaderAttributeCount();
    }
}
