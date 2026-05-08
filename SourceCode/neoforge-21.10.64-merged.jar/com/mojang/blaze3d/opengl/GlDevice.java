package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlDevice implements GpuDevice {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static boolean USE_GL_ARB_vertex_attrib_binding = true;
    protected static boolean USE_GL_KHR_debug = true;
    protected static boolean USE_GL_EXT_debug_label = true;
    protected static boolean USE_GL_ARB_debug_output = true;
    protected static boolean USE_GL_ARB_direct_state_access = true;
    protected static boolean USE_GL_ARB_buffer_storage = true;
    private final CommandEncoder encoder;
    @Nullable
    private final GlDebug debugLog;
    private final GlDebugLabel debugLabels;
    private final int maxSupportedTextureSize;
    private final DirectStateAccess directStateAccess;
    private final BiFunction<ResourceLocation, ShaderType, String> defaultShaderSource;
    private final Map<RenderPipeline, GlRenderPipeline> pipelineCache = new IdentityHashMap<>();
    private final Map<GlDevice.ShaderCompilationKey, GlShaderModule> shaderCache = new HashMap<>();
    private final VertexArrayCache vertexArrayCache;
    private final BufferStorage bufferStorage;
    private final Set<String> enabledExtensions = new HashSet<>();
    private final int uniformOffsetAlignment;
    private final net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties deviceProperties;
    private final net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures enabledFeatures;

    public GlDevice(long window, int debugVerbosity, boolean synchronous, BiFunction<ResourceLocation, ShaderType, String> defaultShaderSource, boolean renderDebugLabels) {
        GLFW.glfwMakeContextCurrent(window);
        GLCapabilities glcapabilities = GL.createCapabilities();
        int i = getMaxSupportedTextureSize();
        GLFW.glfwSetWindowSizeLimits(window, -1, -1, i, i);
        GraphicsWorkarounds graphicsworkarounds = GraphicsWorkarounds.get(this);
        this.debugLog = GlDebug.enableDebugCallback(debugVerbosity, synchronous, this.enabledExtensions);
        this.debugLabels = GlDebugLabel.create(glcapabilities, renderDebugLabels, this.enabledExtensions);
        this.vertexArrayCache = VertexArrayCache.create(glcapabilities, this.debugLabels, this.enabledExtensions);
        this.bufferStorage = BufferStorage.create(glcapabilities, this.enabledExtensions);
        this.directStateAccess = DirectStateAccess.create(glcapabilities, this.enabledExtensions, graphicsworkarounds);
        this.maxSupportedTextureSize = i;
        this.defaultShaderSource = defaultShaderSource;
        this.encoder = new GlCommandEncoder(this);
        this.uniformOffsetAlignment = GL11.glGetInteger(35380);
        GL11.glEnable(34895);
        deviceProperties = new net.neoforged.neoforge.client.blaze3d.opengl.ImmutableGlDeviceProperties(new net.neoforged.neoforge.client.blaze3d.opengl.DefaultGlDeviceProperties());
        final var event = net.neoforged.fml.ModLoader.postEventWithReturn(new net.neoforged.neoforge.client.event.ConfigureGpuDeviceEvent(deviceProperties(), new net.neoforged.neoforge.client.blaze3d.opengl.DefaultGlDeviceFeatures()));
        enabledFeatures = new net.neoforged.neoforge.client.blaze3d.opengl.ImmutableGlDeviceFeatures(event);
    }

    public GlDebugLabel debugLabels() {
        return this.debugLabels;
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        return this.encoder;
    }

    @Override
    public GpuTexture createTexture(
        @Nullable Supplier<String> label, int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels
    ) {
        return this.createTexture(
            this.debugLabels.exists() && label != null ? label.get() : null, usage, format, width, height, depthOrLayers, mipLevels
        );
    }

    @Override
    public GpuTexture createTexture(
        @Nullable String label, int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels
    ) {
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        } else if (depthOrLayers < 1) {
            throw new IllegalArgumentException("depthOrLayers must be at least 1");
        } else {
            boolean flag = (usage & 16) != 0;
            if (flag) {
                if (width != height) {
                    throw new IllegalArgumentException("Cubemap compatible textures must be square, but size is " + width + "x" + height);
                }

                if (depthOrLayers % 6 != 0) {
                    throw new IllegalArgumentException("Cubemap compatible textures must have a layer count with a multiple of 6, was " + depthOrLayers);
                }

                if (depthOrLayers > 6) {
                    throw new UnsupportedOperationException("Array textures are not yet supported");
                }
            } else if (depthOrLayers > 1) {
                throw new UnsupportedOperationException("Array or 3D textures are not yet supported");
            }

            GlStateManager.clearGlErrors();
            int i = GlStateManager._genTexture();
            if (label == null) {
                label = String.valueOf(i);
            }

            int j;
            if (flag) {
                GL11.glBindTexture(34067, i);
                j = 34067;
            } else {
                GlStateManager._bindTexture(i);
                j = 3553;
            }

            GlStateManager._texParameter(j, 33085, mipLevels - 1);
            GlStateManager._texParameter(j, 33082, 0);
            GlStateManager._texParameter(j, 33083, mipLevels - 1);
            if (format.hasDepthAspect()) {
                GlStateManager._texParameter(j, 34892, 0);
            }

            if (flag) {
                for (int k : GlConst.CUBEMAP_TARGETS) {
                    for (int l = 0; l < mipLevels; l++) {
                        GlStateManager._texImage2D(
                            k,
                            l,
                            GlConst.toGlInternalId(format),
                            width >> l,
                            height >> l,
                            0,
                            GlConst.toGlExternalId(format),
                            GlConst.toGlType(format),
                            null
                        );
                    }
                }
            } else {
                for (int i1 = 0; i1 < mipLevels; i1++) {
                    GlStateManager._texImage2D(
                        j,
                        i1,
                        GlConst.toGlInternalId(format),
                        width >> i1,
                        height >> i1,
                        0,
                        GlConst.toGlExternalId(format),
                        GlConst.toGlType(format),
                        null
                    );
                }
            }

            int j1 = GlStateManager._getError();
            if (j1 == 1285) {
                throw new GpuOutOfMemoryException("Could not allocate texture of " + width + "x" + height + " for " + label);
            } else if (j1 != 0) {
                throw new IllegalStateException("OpenGL error " + j1);
            } else {
                GlTexture gltexture = new GlTexture(usage, label, format, width, height, depthOrLayers, mipLevels, i);
                this.debugLabels.applyLabel(gltexture);
                return gltexture;
            }
        }
    }

    /**
     * Adopt an external OpenGL texture into a GpuTexture.
     * The lifecycle of the OpenGL texture will not be tied to this GpuTexture and the external system is responsible
     * for cleaning up the OpenGL texture. Calling this is a relatively expensive operation since we
     * introspect the texture using OpenGL getters.
     */
    public GpuTexture createExternalTexture(@Nullable String label, int usage, int nativeId) {
        var previousBinding = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, nativeId);

        TextureFormat format = null;
        // Get internal format
        var internalFormat = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_INTERNAL_FORMAT);
        // Find matching TextureFormat. We only care about the internal format here, since we're not going to read/write pixels
        for (var candidate : TextureFormat.values()) {
            if (GlConst.toGlInternalId(candidate) == internalFormat || GlConst.toGlExternalId(candidate) == internalFormat) {
                format = candidate;
                break;
            }
        }

        if (format == null) {
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, previousBinding);
            throw new IllegalArgumentException("Couldn't find a matching vanilla TextureFormat for OpenGL internal format id " + internalFormat);
        }

        // Get width and height
        int width = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH);
        int height = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_HEIGHT);

        // Count mip levels by querying sizes until we get 0
        int mipLevels = 1;
        int mipWidth = width;
        while (mipWidth > 1) {
            int nextWidth = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, mipLevels, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH);
            if (nextWidth == 0) {
                break;
            }
            mipLevels++;
            mipWidth = nextWidth;
        }

        int depthOrLayers = 1;

        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, previousBinding);

        return createExternalTexture(usage, label, format, width, height, depthOrLayers, mipLevels, nativeId);
    }

    /**
     * Create a new GPU texture from an existing texture whose lifecycle is externally managed.
     */
    public GpuTexture createExternalTexture(int usage, @Nullable String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int nativeId) {
        if (label == null) {
            label = String.valueOf(nativeId);
        }
        GlTexture gltexture = new GlTexture(usage, label, format, width, height, depthOrLayers, mipLevels, nativeId, true);
        this.debugLabels.applyLabel(gltexture);
        return gltexture;
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return this.createTextureView(texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        if (texture.isClosed()) {
            throw new IllegalArgumentException("Can't create texture view with closed texture");
        } else if (baseMipLevel >= 0 && baseMipLevel + mipLevels <= texture.getMipLevels()) {
            return new GlTextureView((GlTexture)texture, baseMipLevel, mipLevels);
        } else {
            throw new IllegalArgumentException(
                mipLevels
                    + " mip levels starting from "
                    + baseMipLevel
                    + " would be out of range for texture with only "
                    + texture.getMipLevels()
                    + " mip levels"
            );
        }
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        } else {
            GlStateManager.clearGlErrors();
            GlBuffer glbuffer = this.bufferStorage.createBuffer(this.directStateAccess, label, usage, size);
            int i = GlStateManager._getError();
            if (i == 1285) {
                throw new GpuOutOfMemoryException("Could not allocate buffer of " + size + " for " + label);
            } else if (i != 0) {
                throw new IllegalStateException("OpenGL error " + i);
            } else {
                this.debugLabels.applyLabel(glbuffer);
                return glbuffer;
            }
        }
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, int usage, ByteBuffer data) {
        if (!data.hasRemaining()) {
            throw new IllegalArgumentException("Buffer source must not be empty");
        } else {
            GlStateManager.clearGlErrors();
            long i = data.remaining();
            GlBuffer glbuffer = this.bufferStorage.createBuffer(this.directStateAccess, label, usage, data);
            int j = GlStateManager._getError();
            if (j == 1285) {
                throw new GpuOutOfMemoryException("Could not allocate buffer of " + i + " for " + label);
            } else if (j != 0) {
                throw new IllegalStateException("OpenGL error " + j);
            } else {
                this.debugLabels.applyLabel(glbuffer);
                return glbuffer;
            }
        }
    }

    @Override
    public String getImplementationInformation() {
        return GLFW.glfwGetCurrentContext() == 0L
            ? "NO CONTEXT"
            : GlStateManager._getString(7937) + " GL version " + GlStateManager._getString(7938) + ", " + GlStateManager._getString(7936);
    }

    @Override
    public List<String> getLastDebugMessages() {
        return this.debugLog == null ? Collections.emptyList() : this.debugLog.getLastOpenGlDebugMessages();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return this.debugLog != null;
    }

    @Override
    public String getRenderer() {
        return GlStateManager._getString(7937);
    }

    @Override
    public String getVendor() {
        return GlStateManager._getString(7936);
    }

    @Override
    public String getBackendName() {
        return "OpenGL";
    }

    @Override
    public String getVersion() {
        return GlStateManager._getString(7938);
    }

    private static int getMaxSupportedTextureSize() {
        int i = GlStateManager._getInteger(3379);

        for (int j = Math.max(32768, i); j >= 1024; j >>= 1) {
            GlStateManager._texImage2D(32868, 0, 6408, j, j, 0, 6408, 5121, null);
            int k = GlStateManager._getTexLevelParameter(32868, 0, 4096);
            if (k != 0) {
                return j;
            }
        }

        int l = Math.max(i, 1024);
        LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", l);
        return l;
    }

    @Override
    public int getMaxTextureSize() {
        return this.maxSupportedTextureSize;
    }

    @Override
    public int getUniformOffsetAlignment() {
        return this.uniformOffsetAlignment;
    }

    @Override
    public void clearPipelineCache() {
        for (GlRenderPipeline glrenderpipeline : this.pipelineCache.values()) {
            if (glrenderpipeline.program() != GlProgram.INVALID_PROGRAM) {
                glrenderpipeline.program().close();
            }
        }

        this.pipelineCache.clear();

        for (GlShaderModule glshadermodule : this.shaderCache.values()) {
            if (glshadermodule != GlShaderModule.INVALID_SHADER) {
                glshadermodule.close();
            }
        }

        this.shaderCache.clear();
        String s = GlStateManager._getString(7937);
        if (s.contains("AMD")) {
            sacrificeShaderToOpenGlAndAmd();
        }
    }

    private static void sacrificeShaderToOpenGlAndAmd() {
        int i = GlStateManager.glCreateShader(35633);
        int j = GlStateManager.glCreateProgram();
        GlStateManager.glAttachShader(j, i);
        GlStateManager.glDeleteShader(i);
        GlStateManager.glDeleteProgram(j);
    }

    @Override
    public List<String> getEnabledExtensions() {
        return new ArrayList<>(this.enabledExtensions);
    }

    @Override
    public void close() {
        this.clearPipelineCache();
    }

    public DirectStateAccess directStateAccess() {
        return this.directStateAccess;
    }

    protected GlRenderPipeline getOrCompilePipeline(RenderPipeline pipeline) {
        return this.pipelineCache.computeIfAbsent(pipeline, p_410866_ -> this.compilePipeline(pipeline, this.defaultShaderSource));
    }

    protected GlShaderModule getOrCompileShader(
        ResourceLocation shader, ShaderType type, ShaderDefines defines, BiFunction<ResourceLocation, ShaderType, String> shaderSource
    ) {
        GlDevice.ShaderCompilationKey gldevice$shadercompilationkey = new GlDevice.ShaderCompilationKey(shader, type, defines);
        return this.shaderCache.computeIfAbsent(gldevice$shadercompilationkey, p_410725_ -> this.compileShader(gldevice$shadercompilationkey, shaderSource));
    }

    public GlRenderPipeline precompilePipeline(RenderPipeline renderPipeline, @Nullable BiFunction<ResourceLocation, ShaderType, String> shaderSource) {
        BiFunction<ResourceLocation, ShaderType, String> bifunction = shaderSource == null ? this.defaultShaderSource : shaderSource;
        return this.pipelineCache.computeIfAbsent(renderPipeline, p_410619_ -> this.compilePipeline(renderPipeline, bifunction));
    }

    private GlShaderModule compileShader(GlDevice.ShaderCompilationKey key, BiFunction<ResourceLocation, ShaderType, String> shaderSource) {
        String s = shaderSource.apply(key.id, key.type);
        if (s == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", key.type, key.id);
            return GlShaderModule.INVALID_SHADER;
        } else {
            String s1 = GlslPreprocessor.injectDefines(s, key.defines);
            int i = GlStateManager.glCreateShader(GlConst.toGl(key.type));
            GlStateManager.glShaderSource(i, s1);
            GlStateManager.glCompileShader(i);
            if (GlStateManager.glGetShaderi(i, 35713) == 0) {
                String s2 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
                LOGGER.error("Couldn't compile {} shader ({}): {}", key.type.getName(), key.id, s2);
                return GlShaderModule.INVALID_SHADER;
            } else {
                GlShaderModule glshadermodule = new GlShaderModule(i, key.id, key.type);
                this.debugLabels.applyLabel(glshadermodule);
                return glshadermodule;
            }
        }
    }

    private GlRenderPipeline compilePipeline(RenderPipeline pipeline, BiFunction<ResourceLocation, ShaderType, String> shaderSource) {
        GlShaderModule glshadermodule = this.getOrCompileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
        GlShaderModule glshadermodule1 = this.getOrCompileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
        if (glshadermodule == GlShaderModule.INVALID_SHADER) {
            LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", pipeline.getLocation(), pipeline.getVertexShader());
            return new GlRenderPipeline(pipeline, GlProgram.INVALID_PROGRAM);
        } else if (glshadermodule1 == GlShaderModule.INVALID_SHADER) {
            LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", pipeline.getLocation(), pipeline.getFragmentShader());
            return new GlRenderPipeline(pipeline, GlProgram.INVALID_PROGRAM);
        } else {
            GlProgram glprogram;
            try {
                glprogram = GlProgram.link(glshadermodule, glshadermodule1, pipeline.getVertexFormat(), pipeline.getLocation().toString());
            } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
                LOGGER.error("Couldn't compile program for pipeline {}: {}", pipeline.getLocation(), shadermanager$compilationexception);
                return new GlRenderPipeline(pipeline, GlProgram.INVALID_PROGRAM);
            }

            glprogram.setupUniforms(pipeline.getUniforms(), pipeline.getSamplers());
            this.debugLabels.applyLabel(glprogram);
            return new GlRenderPipeline(pipeline, glprogram);
        }
    }

    public VertexArrayCache vertexArrayCache() {
        return this.vertexArrayCache;
    }

    public BufferStorage getBufferStorage() {
        return this.bufferStorage;
    }

    @OnlyIn(Dist.CLIENT)
    record ShaderCompilationKey(ResourceLocation id, ShaderType type, ShaderDefines defines) {
        @Override
        public String toString() {
            String s = this.id + " (" + this.type + ")";
            return !this.defines.isEmpty() ? s + " with " + this.defines : s;
        }
    }

    @Override
    public net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties deviceProperties() {
        return deviceProperties;
    }

    @Override
    public net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures enabledFeatures() {
        return enabledFeatures;
    }
}
