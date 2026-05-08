package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderPipeline {
    private final ResourceLocation location;
    private final ResourceLocation vertexShader;
    private final ResourceLocation fragmentShader;
    private final ShaderDefines shaderDefines;
    private final List<String> samplers;
    private final List<RenderPipeline.UniformDescription> uniforms;
    private final DepthTestFunction depthTestFunction;
    private final PolygonMode polygonMode;
    private final boolean cull;
    private final LogicOp colorLogic;
    private final Optional<BlendFunction> blendFunction;
    private final boolean writeColor;
    private final boolean writeAlpha;
    private final boolean writeDepth;
    private final VertexFormat vertexFormat;
    private final VertexFormat.Mode vertexFormatMode;
    private final float depthBiasScaleFactor;
    private final float depthBiasConstant;
    private final int sortKey;
    private static int sortKeySeed;
    private final Optional<net.neoforged.neoforge.client.stencil.StencilTest> stencilTest;

    /**
 * @deprecated Neo: use {@link #RenderPipeline(ResourceLocation, ResourceLocation,
 *             ResourceLocation, ShaderDefines, List, List, Optional,
 *             DepthTestFunction, PolygonMode, boolean, boolean, boolean, boolean,
 *             LogicOp, VertexFormat, VertexFormat.Mode, float, float, int,
 *             Optional)} instead
 */
    @Deprecated
    protected RenderPipeline(
        ResourceLocation location,
        ResourceLocation vertexShader,
        ResourceLocation fragmentShader,
        ShaderDefines shaderDefines,
        List<String> samplers,
        List<RenderPipeline.UniformDescription> uniforms,
        Optional<BlendFunction> blendFunction,
        DepthTestFunction depthTestFunction,
        PolygonMode polygonMode,
        boolean cull,
        boolean writeColor,
        boolean writeAlpha,
        boolean writeDepth,
        LogicOp colorLogic,
        VertexFormat vertexFormat,
        VertexFormat.Mode vertexFormatMode,
        float depthBiasScaleFactor,
        float depthBiasConstant,
        int sortKey
    ) {
        this(location, vertexShader, fragmentShader, shaderDefines, samplers, uniforms, blendFunction, depthTestFunction, polygonMode, cull, writeColor, writeAlpha, writeDepth, colorLogic, vertexFormat, vertexFormatMode, depthBiasScaleFactor, depthBiasConstant, sortKey, Optional.empty());
    }

    protected RenderPipeline(
            ResourceLocation location,
            ResourceLocation vertexShader,
            ResourceLocation fragmentShader,
            ShaderDefines shaderDefines,
            List<String> samplers,
            List<RenderPipeline.UniformDescription> uniforms,
            Optional<BlendFunction> blendFunction,
            DepthTestFunction depthTestFunction,
            PolygonMode polygonMode,
            boolean cull,
            boolean writeColor,
            boolean writeAlpha,
            boolean writeDepth,
            LogicOp colorLogic,
            VertexFormat vertexFormat,
            VertexFormat.Mode vertexFormatMode,
            float depthBiasScaleFactor,
            float depthBiasConstant,
            int sortKey,
            Optional<net.neoforged.neoforge.client.stencil.StencilTest> stencilTest
            ) {
        this.location = location;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.shaderDefines = shaderDefines;
        this.samplers = samplers;
        this.uniforms = uniforms;
        this.depthTestFunction = depthTestFunction;
        this.polygonMode = polygonMode;
        this.cull = cull;
        this.blendFunction = blendFunction;
        this.writeColor = writeColor;
        this.writeAlpha = writeAlpha;
        this.writeDepth = writeDepth;
        this.colorLogic = colorLogic;
        this.vertexFormat = vertexFormat;
        this.vertexFormatMode = vertexFormatMode;
        this.depthBiasScaleFactor = depthBiasScaleFactor;
        this.depthBiasConstant = depthBiasConstant;
        this.sortKey = sortKey;
        this.stencilTest = stencilTest;
    }

    public int getSortKey() {
        return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? super.hashCode() * (sortKeySeed + 1) : this.sortKey;
    }

    public static void updateSortKeySeed() {
        sortKeySeed = Math.round(100000.0F * (float)Math.random());
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

    public DepthTestFunction getDepthTestFunction() {
        return this.depthTestFunction;
    }

    public PolygonMode getPolygonMode() {
        return this.polygonMode;
    }

    public boolean isCull() {
        return this.cull;
    }

    public LogicOp getColorLogic() {
        return this.colorLogic;
    }

    public Optional<BlendFunction> getBlendFunction() {
        return this.blendFunction;
    }

    public boolean isWriteColor() {
        return this.writeColor;
    }

    public boolean isWriteAlpha() {
        return this.writeAlpha;
    }

    public boolean isWriteDepth() {
        return this.writeDepth;
    }

    public float getDepthBiasScaleFactor() {
        return this.depthBiasScaleFactor;
    }

    public float getDepthBiasConstant() {
        return this.depthBiasConstant;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }

    public VertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public VertexFormat.Mode getVertexFormatMode() {
        return this.vertexFormatMode;
    }

    public ResourceLocation getVertexShader() {
        return this.vertexShader;
    }

    public ResourceLocation getFragmentShader() {
        return this.fragmentShader;
    }

    public ShaderDefines getShaderDefines() {
        return this.shaderDefines;
    }

    public List<String> getSamplers() {
        return this.samplers;
    }

    public List<RenderPipeline.UniformDescription> getUniforms() {
        return this.uniforms;
    }

    public boolean wantsDepthTexture() {
        return this.depthTestFunction != DepthTestFunction.NO_DEPTH_TEST
            || this.depthBiasConstant != 0.0F
            || this.depthBiasScaleFactor != 0.0F
            || this.writeDepth;
    }

    public Optional<net.neoforged.neoforge.client.stencil.StencilTest> getStencilTest() {
        return stencilTest;
    }

    /**
     * Neo: Create a {@link RenderPipeline.Builder} from this {@link RenderPipeline} to adjust its configuration and
     * build a new, modified {@link RenderPipeline} from it
     */
    public RenderPipeline.Builder toBuilder() {
        RenderPipeline.Builder builder = new RenderPipeline.Builder();
        builder.location = Optional.of(this.location);
        builder.fragmentShader = Optional.of(this.fragmentShader);
        builder.vertexShader = Optional.of(this.vertexShader);
        if (!this.shaderDefines.isEmpty()) {
            ShaderDefines.Builder defBuilder = ShaderDefines.builder();
            for (Entry<String, String> entry : this.shaderDefines.values().entrySet()) {
                defBuilder.define(entry.getKey(), entry.getValue());
            }
            for (String flag : this.shaderDefines.flags()) {
                defBuilder.define(flag);
            }
            builder.definesBuilder = Optional.of(defBuilder);
        }
        if (!this.samplers.isEmpty()) {
            builder.samplers = Optional.of(new ArrayList<>(this.samplers));
        }
        if (!this.uniforms.isEmpty()) {
            builder.uniforms = Optional.of(new ArrayList<>(this.uniforms));
        }
        builder.depthTestFunction = Optional.of(this.depthTestFunction);
        builder.polygonMode = Optional.of(this.polygonMode);
        builder.cull = Optional.of(this.cull);
        builder.writeColor = Optional.of(this.writeColor);
        builder.writeAlpha = Optional.of(this.writeAlpha);
        builder.writeDepth = Optional.of(this.writeDepth);
        builder.colorLogic = Optional.of(this.colorLogic);
        builder.blendFunction = this.blendFunction;
        builder.vertexFormat = Optional.of(this.vertexFormat);
        builder.vertexFormatMode = Optional.of(this.vertexFormatMode);
        builder.depthBiasScaleFactor = this.depthBiasScaleFactor;
        builder.depthBiasConstant = this.depthBiasConstant;
        builder.stencilTest = this.stencilTest;
        return builder;
    }

    public static RenderPipeline.Builder builder(RenderPipeline.Snippet... snippets) {
        RenderPipeline.Builder renderpipeline$builder = new RenderPipeline.Builder();

        for (RenderPipeline.Snippet renderpipeline$snippet : snippets) {
            renderpipeline$builder.withSnippet(renderpipeline$snippet);
        }

        return renderpipeline$builder;
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public static class Builder {
        private static int nextPipelineSortKey;
        private Optional<ResourceLocation> location = Optional.empty();
        private Optional<ResourceLocation> fragmentShader = Optional.empty();
        private Optional<ResourceLocation> vertexShader = Optional.empty();
        private Optional<ShaderDefines.Builder> definesBuilder = Optional.empty();
        private Optional<List<String>> samplers = Optional.empty();
        private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
        private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
        private Optional<PolygonMode> polygonMode = Optional.empty();
        private Optional<Boolean> cull = Optional.empty();
        private Optional<Boolean> writeColor = Optional.empty();
        private Optional<Boolean> writeAlpha = Optional.empty();
        private Optional<Boolean> writeDepth = Optional.empty();
        private Optional<LogicOp> colorLogic = Optional.empty();
        private Optional<BlendFunction> blendFunction = Optional.empty();
        private Optional<VertexFormat> vertexFormat = Optional.empty();
        private Optional<VertexFormat.Mode> vertexFormatMode = Optional.empty();
        private float depthBiasScaleFactor;
        private float depthBiasConstant;
        private Optional<net.neoforged.neoforge.client.stencil.StencilTest> stencilTest = Optional.empty();

        Builder() {
        }

        public RenderPipeline.Builder withLocation(String location) {
            this.location = Optional.of(ResourceLocation.withDefaultNamespace(location));
            return this;
        }

        public RenderPipeline.Builder withLocation(ResourceLocation location) {
            this.location = Optional.of(location);
            return this;
        }

        public RenderPipeline.Builder withFragmentShader(String fragmentShader) {
            this.fragmentShader = Optional.of(ResourceLocation.withDefaultNamespace(fragmentShader));
            return this;
        }

        public RenderPipeline.Builder withFragmentShader(ResourceLocation fragmentShader) {
            this.fragmentShader = Optional.of(fragmentShader);
            return this;
        }

        public RenderPipeline.Builder withVertexShader(String vertexShader) {
            this.vertexShader = Optional.of(ResourceLocation.withDefaultNamespace(vertexShader));
            return this;
        }

        public RenderPipeline.Builder withVertexShader(ResourceLocation vertexShader) {
            this.vertexShader = Optional.of(vertexShader);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String flag) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            this.definesBuilder.get().define(flag);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String key, int value) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            this.definesBuilder.get().define(key, value);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String key, float value) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            this.definesBuilder.get().define(key, value);
            return this;
        }

        public RenderPipeline.Builder withSampler(String sampler) {
            if (this.samplers.isEmpty()) {
                this.samplers = Optional.of(new ArrayList<>());
            }

            this.samplers.get().add(sampler);
            return this;
        }

        public RenderPipeline.Builder withUniform(String uniform, UniformType type) {
            if (this.uniforms.isEmpty()) {
                this.uniforms = Optional.of(new ArrayList<>());
            }

            if (type == UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
            } else {
                this.uniforms.get().add(new RenderPipeline.UniformDescription(uniform, type));
                return this;
            }
        }

        public RenderPipeline.Builder withUniform(String uniform, UniformType type, TextureFormat format) {
            if (this.uniforms.isEmpty()) {
                this.uniforms = Optional.of(new ArrayList<>());
            }

            if (type != UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Only texel buffer can specify texture format");
            } else {
                this.uniforms.get().add(new RenderPipeline.UniformDescription(uniform, format));
                return this;
            }
        }

        public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction depthTestFunction) {
            this.depthTestFunction = Optional.of(depthTestFunction);
            return this;
        }

        public RenderPipeline.Builder withPolygonMode(PolygonMode polygonMode) {
            this.polygonMode = Optional.of(polygonMode);
            return this;
        }

        public RenderPipeline.Builder withCull(boolean cull) {
            this.cull = Optional.of(cull);
            return this;
        }

        public RenderPipeline.Builder withBlend(BlendFunction blendFunction) {
            this.blendFunction = Optional.of(blendFunction);
            return this;
        }

        public RenderPipeline.Builder withoutBlend() {
            this.blendFunction = Optional.empty();
            return this;
        }

        public RenderPipeline.Builder withColorWrite(boolean writeColor) {
            this.writeColor = Optional.of(writeColor);
            this.writeAlpha = Optional.of(writeColor);
            return this;
        }

        public RenderPipeline.Builder withColorWrite(boolean writeColor, boolean writeAlpha) {
            this.writeColor = Optional.of(writeColor);
            this.writeAlpha = Optional.of(writeAlpha);
            return this;
        }

        public RenderPipeline.Builder withDepthWrite(boolean writeDepth) {
            this.writeDepth = Optional.of(writeDepth);
            return this;
        }

        @Deprecated
        public RenderPipeline.Builder withColorLogic(LogicOp colorLogic) {
            this.colorLogic = Optional.of(colorLogic);
            return this;
        }

        public RenderPipeline.Builder withVertexFormat(VertexFormat vertexFormat, VertexFormat.Mode vertexFormatMode) {
            this.vertexFormat = Optional.of(vertexFormat);
            this.vertexFormatMode = Optional.of(vertexFormatMode);
            return this;
        }

        public RenderPipeline.Builder withDepthBias(float scaleFactor, float constant) {
            this.depthBiasScaleFactor = scaleFactor;
            this.depthBiasConstant = constant;
            return this;
        }

        public RenderPipeline.Builder withStencilTest(net.neoforged.neoforge.client.stencil.StencilTest stencilTest) {
            this.stencilTest = Optional.of(stencilTest);
            return this;
        }

        public RenderPipeline.Builder withoutStencilTest(){
            this.stencilTest = Optional.empty();
            return this;
        }

        void withSnippet(RenderPipeline.Snippet snippet) {
            if (snippet.vertexShader.isPresent()) {
                this.vertexShader = snippet.vertexShader;
            }

            if (snippet.fragmentShader.isPresent()) {
                this.fragmentShader = snippet.fragmentShader;
            }

            if (snippet.shaderDefines.isPresent()) {
                if (this.definesBuilder.isEmpty()) {
                    this.definesBuilder = Optional.of(ShaderDefines.builder());
                }

                ShaderDefines shaderdefines = snippet.shaderDefines.get();

                for (Entry<String, String> entry : shaderdefines.values().entrySet()) {
                    this.definesBuilder.get().define(entry.getKey(), entry.getValue());
                }

                for (String s : shaderdefines.flags()) {
                    this.definesBuilder.get().define(s);
                }
            }

            snippet.samplers.ifPresent(p_405714_ -> {
                if (this.samplers.isPresent()) {
                    this.samplers.get().addAll(p_405714_);
                } else {
                    this.samplers = Optional.of(new ArrayList<>(p_405714_));
                }
            });
            snippet.uniforms.ifPresent(p_405104_ -> {
                if (this.uniforms.isPresent()) {
                    this.uniforms.get().addAll(p_405104_);
                } else {
                    this.uniforms = Optional.of(new ArrayList<>(p_405104_));
                }
            });
            if (snippet.depthTestFunction.isPresent()) {
                this.depthTestFunction = snippet.depthTestFunction;
            }

            if (snippet.cull.isPresent()) {
                this.cull = snippet.cull;
            }

            if (snippet.writeColor.isPresent()) {
                this.writeColor = snippet.writeColor;
            }

            if (snippet.writeAlpha.isPresent()) {
                this.writeAlpha = snippet.writeAlpha;
            }

            if (snippet.writeDepth.isPresent()) {
                this.writeDepth = snippet.writeDepth;
            }

            if (snippet.colorLogic.isPresent()) {
                this.colorLogic = snippet.colorLogic;
            }

            if (snippet.blendFunction.isPresent()) {
                this.blendFunction = snippet.blendFunction;
            }

            if (snippet.vertexFormat.isPresent()) {
                this.vertexFormat = snippet.vertexFormat;
            }

            if (snippet.vertexFormatMode.isPresent()) {
                this.vertexFormatMode = snippet.vertexFormatMode;
            }

            if (snippet.stencilTest.isPresent()) {
                this.stencilTest = snippet.stencilTest;
            }
        }

        public RenderPipeline.Snippet buildSnippet() {
            return new RenderPipeline.Snippet(
                this.vertexShader,
                this.fragmentShader,
                this.definesBuilder.map(ShaderDefines.Builder::build),
                this.samplers.map(Collections::unmodifiableList),
                this.uniforms.map(Collections::unmodifiableList),
                this.blendFunction,
                this.depthTestFunction,
                this.polygonMode,
                this.cull,
                this.writeColor,
                this.writeAlpha,
                this.writeDepth,
                this.colorLogic,
                this.vertexFormat,
                this.vertexFormatMode,
                this.stencilTest
            );
        }

        public RenderPipeline build() {
            if (this.location.isEmpty()) {
                throw new IllegalStateException("Missing location");
            } else if (this.vertexShader.isEmpty()) {
                throw new IllegalStateException("Missing vertex shader");
            } else if (this.fragmentShader.isEmpty()) {
                throw new IllegalStateException("Missing fragment shader");
            } else if (this.vertexFormat.isEmpty()) {
                throw new IllegalStateException("Missing vertex buffer format");
            } else if (this.vertexFormatMode.isEmpty()) {
                throw new IllegalStateException("Missing vertex mode");
            } else {
                return new RenderPipeline(
                    this.location.get(),
                    this.vertexShader.get(),
                    this.fragmentShader.get(),
                    this.definesBuilder.orElse(ShaderDefines.builder()).build(),
                    List.copyOf(this.samplers.orElse(new ArrayList<>())),
                    this.uniforms.orElse(Collections.emptyList()),
                    this.blendFunction,
                    this.depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST),
                    this.polygonMode.orElse(PolygonMode.FILL),
                    this.cull.orElse(true),
                    this.writeColor.orElse(true),
                    this.writeAlpha.orElse(true),
                    this.writeDepth.orElse(true),
                    this.colorLogic.orElse(LogicOp.NONE),
                    this.vertexFormat.get(),
                    this.vertexFormatMode.get(),
                    this.depthBiasScaleFactor,
                    this.depthBiasConstant,
                    nextPipelineSortKey++,
                    this.stencilTest
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public record Snippet(
        Optional<ResourceLocation> vertexShader,
        Optional<ResourceLocation> fragmentShader,
        Optional<ShaderDefines> shaderDefines,
        Optional<List<String>> samplers,
        Optional<List<RenderPipeline.UniformDescription>> uniforms,
        Optional<BlendFunction> blendFunction,
        Optional<DepthTestFunction> depthTestFunction,
        Optional<PolygonMode> polygonMode,
        Optional<Boolean> cull,
        Optional<Boolean> writeColor,
        Optional<Boolean> writeAlpha,
        Optional<Boolean> writeDepth,
        Optional<LogicOp> colorLogic,
        Optional<VertexFormat> vertexFormat,
        Optional<VertexFormat.Mode> vertexFormatMode,
        Optional<net.neoforged.neoforge.client.stencil.StencilTest> stencilTest
    ) {
        /** @deprecated Neo: use {@link #Snippet(Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional, Optional)} instead */
        @Deprecated
        public Snippet(
                Optional<ResourceLocation> vertexShader,
                Optional<ResourceLocation> fragmentShader,
                Optional<ShaderDefines> shaderDefines,
                Optional<List<String>> samplers,
                Optional<List<RenderPipeline.UniformDescription>> uniforms,
                Optional<BlendFunction> blendFunction,
                Optional<DepthTestFunction> depthTestFunction,
                Optional<PolygonMode> polygonMode,
                Optional<Boolean> cull,
                Optional<Boolean> writeColor,
                Optional<Boolean> writeAlpha,
                Optional<Boolean> writeDepth,
                Optional<LogicOp> colorLogic,
                Optional<VertexFormat> vertexFormat,
                Optional<VertexFormat.Mode> vertexFormatMode
        ) {
            this(vertexShader, fragmentShader, shaderDefines, samplers, uniforms, blendFunction, depthTestFunction, polygonMode, cull, writeColor, writeAlpha, writeDepth, colorLogic, vertexFormat, vertexFormatMode, Optional.empty());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public record UniformDescription(String name, UniformType type, @Nullable TextureFormat textureFormat) {
        public UniformDescription(String p_405787_, UniformType p_410454_) {
            this(p_405787_, p_410454_, null);
            if (p_410454_ == UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Texel buffer needs a texture format");
            }
        }

        public UniformDescription(String p_418130_, TextureFormat p_418054_) {
            this(p_418130_, UniformType.TEXEL_BUFFER, p_418054_);
        }
    }
}
