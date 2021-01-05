package com.simibubi.create.foundation.utility.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.temporal.TemporalAccessor;

public class TemplateBuffer {
    protected ByteBuffer template;
    protected int formatSize;
    protected int count;

    public TemplateBuffer(BufferBuilder buf) {
        Pair<BufferBuilder.DrawState, ByteBuffer> state = buf.popData();
        ByteBuffer rendered = state.getSecond();
        rendered.order(ByteOrder.nativeOrder()); // Vanilla bug, endianness does not carry over into sliced buffers

        formatSize = buf.getVertexFormat()
                        .getSize();
        count = state.getFirst().getCount();
        int size = count * formatSize;

        template = GLAllocation.createDirectByteBuffer(size);
        template.order(rendered.order());
        ((Buffer)template).limit(((Buffer)rendered).limit());
        template.put(rendered);
        ((Buffer)template).rewind();
    }

    public boolean isEmpty() {
        return ((Buffer) template).limit() == 0;
    }

    protected int vertexCount(ByteBuffer buffer) {
        return ((Buffer)buffer).limit() / formatSize;
    }

    protected int getBufferPosition(int vertexIndex) {
        return vertexIndex * formatSize;
    }

    protected float getX(ByteBuffer buffer, int index) {
        return buffer.getFloat(getBufferPosition(index));
    }

    protected float getY(ByteBuffer buffer, int index) {
        return buffer.getFloat(getBufferPosition(index) + 4);
    }

    protected float getZ(ByteBuffer buffer, int index) {
        return buffer.getFloat(getBufferPosition(index) + 8);
    }

    protected byte getR(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 12);
    }

    protected byte getG(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 13);
    }

    protected byte getB(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 14);
    }

    protected byte getA(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 15);
    }

    protected float getU(ByteBuffer buffer, int index) {
        return buffer.getFloat(getBufferPosition(index) + 16);
    }

    protected float getV(ByteBuffer buffer, int index) {
        return buffer.getFloat(getBufferPosition(index) + 20);
    }

    protected int getLight(ByteBuffer buffer, int index) {
        return buffer.getInt(getBufferPosition(index) + 24);
    }

    protected byte getNX(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 28);
    }

    protected byte getNY(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 29);
    }

    protected byte getNZ(ByteBuffer buffer, int index) {
        return buffer.get(getBufferPosition(index) + 30);
    }
}
