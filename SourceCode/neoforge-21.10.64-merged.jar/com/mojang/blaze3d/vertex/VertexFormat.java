package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class VertexFormat {
    public static final int UNKNOWN_ELEMENT = -1;
    private final List<VertexFormatElement> elements;
    private final List<String> names;
    private final int vertexSize;
    private final int elementsMask;
    private final int[] offsetsByElement = new int[32];
    @Nullable
    private GpuBuffer immediateDrawVertexBuffer;
    @Nullable
    private GpuBuffer immediateDrawIndexBuffer;

    VertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
        this.elements = elements;
        this.names = names;
        this.vertexSize = vertexSize;
        this.elementsMask = elements.stream().mapToInt(VertexFormatElement::mask).reduce(0, (p_350941_, p_350570_) -> p_350941_ | p_350570_);

        for (int i = 0; i < this.offsetsByElement.length; i++) {
            VertexFormatElement vertexformatelement = VertexFormatElement.byId(i);
            int j = vertexformatelement != null ? elements.indexOf(vertexformatelement) : -1;
            this.offsetsByElement[i] = j != -1 ? offsets.getInt(j) : -1;
        }
    }

    public static VertexFormat.Builder builder() {
        return new VertexFormat.Builder();
    }

    @Override
    public String toString() {
        return "VertexFormat" + this.names;
    }

    public int getVertexSize() {
        return this.vertexSize;
    }

    public List<VertexFormatElement> getElements() {
        return this.elements;
    }

    public List<String> getElementAttributeNames() {
        return this.names;
    }

    public int[] getOffsetsByElement() {
        return this.offsetsByElement;
    }

    public int getOffset(VertexFormatElement element) {
        return this.offsetsByElement[element.id()];
    }

    public boolean contains(VertexFormatElement element) {
        return (this.elementsMask & element.mask()) != 0;
    }

    public int getElementsMask() {
        return this.elementsMask;
    }

    public String getElementName(VertexFormatElement element) {
        int i = this.elements.indexOf(element);
        if (i == -1) {
            throw new IllegalArgumentException(element + " is not contained in format");
        } else {
            return this.names.get(i);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other
            ? true
            : other instanceof VertexFormat vertexformat
                && this.elementsMask == vertexformat.elementsMask
                && this.vertexSize == vertexformat.vertexSize
                && this.names.equals(vertexformat.names)
                && Arrays.equals(this.offsetsByElement, vertexformat.offsetsByElement);
    }

    @Override
    public int hashCode() {
        return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
    }

    private static GpuBuffer uploadToBuffer(@Nullable GpuBuffer buffer, ByteBuffer data, int usage, Supplier<String> label) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (GraphicsWorkarounds.get(gpudevice).alwaysCreateFreshImmediateBuffer()) {
            if (buffer != null) {
                buffer.close();
            }

            return gpudevice.createBuffer(label, usage, data);
        } else {
            if (buffer == null) {
                buffer = gpudevice.createBuffer(label, usage, data);
            } else {
                CommandEncoder commandencoder = gpudevice.createCommandEncoder();
                if (buffer.size() < data.remaining()) {
                    buffer.close();
                    buffer = gpudevice.createBuffer(label, usage, data);
                } else {
                    commandencoder.writeToBuffer(buffer.slice(), data);
                }
            }

            return buffer;
        }
    }

    public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer buffer) {
        this.immediateDrawVertexBuffer = uploadToBuffer(this.immediateDrawVertexBuffer, buffer, 40, () -> "Immediate vertex buffer for " + this);
        return this.immediateDrawVertexBuffer;
    }

    public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer buffer) {
        this.immediateDrawIndexBuffer = uploadToBuffer(this.immediateDrawIndexBuffer, buffer, 72, () -> "Immediate index buffer for " + this);
        return this.immediateDrawIndexBuffer;
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public static class Builder {
        private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        private final IntList offsets = new IntArrayList();
        private int offset;

        Builder() {
        }

        public VertexFormat.Builder add(String name, VertexFormatElement element) {
            this.elements.put(name, element);
            this.offsets.add(this.offset);
            this.offset = this.offset + element.byteSize();
            return this;
        }

        public VertexFormat.Builder padding(int padding) {
            this.offset += padding;
            return this;
        }

        public VertexFormat build() {
            ImmutableMap<String, VertexFormatElement> immutablemap = this.elements.buildOrThrow();
            ImmutableList<VertexFormatElement> immutablelist = immutablemap.values().asList();
            ImmutableList<String> immutablelist1 = immutablemap.keySet().asList();
            return new VertexFormat(immutablelist, immutablelist1, this.offsets, this.offset);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum IndexType {
        SHORT(2),
        INT(4);

        public final int bytes;

        private IndexType(int bytes) {
            this.bytes = bytes;
        }

        public static VertexFormat.IndexType least(int indexCount) {
            return (indexCount & -65536) != 0 ? INT : SHORT;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Mode {
        LINES(2, 2, false),
        LINE_STRIP(2, 1, true),
        DEBUG_LINES(2, 2, false),
        DEBUG_LINE_STRIP(2, 1, true),
        TRIANGLES(3, 3, false),
        TRIANGLE_STRIP(3, 1, true),
        TRIANGLE_FAN(3, 1, true),
        QUADS(4, 4, false);

        public final int primitiveLength;
        public final int primitiveStride;
        public final boolean connectedPrimitives;

        private Mode(int primitiveLength, int primitiveStride, boolean connectedPrimitives) {
            this.primitiveLength = primitiveLength;
            this.primitiveStride = primitiveStride;
            this.connectedPrimitives = connectedPrimitives;
        }

        public int indexCount(int vertices) {
            return switch (this) {
                case LINES, QUADS -> vertices / 4 * 6;
                case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> vertices;
                default -> 0;
            };
        }
    }

    public ImmutableMap<String, VertexFormatElement> getElementMapping() {
        ImmutableMap.Builder<String, VertexFormatElement> builder = ImmutableMap.builder();
        for (int i = 0; i < elements.size(); i++) {
            builder.put(names.get(i), elements.get(i));
        }
        return builder.build();
    }

    public boolean hasPosition() {
        return elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.POSITION);
    }

    public boolean hasNormal() {
        return elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.NORMAL);
    }

    public boolean hasColor() {
        return elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.COLOR);
    }

    public boolean hasUV(int which) {
        return elements.stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.UV && e.index() == which);
    }
}
