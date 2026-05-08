package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ShaderManager extends SimplePreparableReloadListener<ShaderManager.Configs> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_LOG_LENGTH = 32768;
    public static final String SHADER_PATH = "shaders";
    private static final String SHADER_INCLUDE_PATH = "shaders/include/";
    private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
    final TextureManager textureManager;
    private final Consumer<Exception> recoveryHandler;
    private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);
    final CachedOrthoProjectionMatrixBuffer postChainProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("post", 0.1F, 1000.0F, false);

    public ShaderManager(TextureManager textureManager, Consumer<Exception> recoveryHandler) {
        this.textureManager = textureManager;
        this.recoveryHandler = recoveryHandler;
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected ShaderManager.Configs prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Builder<ShaderManager.ShaderSourceKey, String> builder = ImmutableMap.builder();
        Map<ResourceLocation, Resource> map = resourceManager.listResources("shaders", ShaderManager::isShader);

        for (Entry<ResourceLocation, Resource> entry : map.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ShaderType shadertype = ShaderType.byLocation(resourcelocation);
            if (shadertype != null) {
                loadShader(resourcelocation, entry.getValue(), shadertype, map, builder);
            }
        }

        Builder<ResourceLocation, PostChainConfig> builder1 = ImmutableMap.builder();

        for (Entry<ResourceLocation, Resource> entry1 : POST_CHAIN_ID_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            loadPostChain(entry1.getKey(), entry1.getValue(), builder1);
        }

        return new ShaderManager.Configs(builder.build(), builder1.build());
    }

    private static void loadShader(
        ResourceLocation location,
        Resource shader,
        ShaderType type,
        Map<ResourceLocation, Resource> shaderResources,
        Builder<ShaderManager.ShaderSourceKey, String> output
    ) {
        ResourceLocation resourcelocation = type.idConverter().fileToId(location);
        GlslPreprocessor glslpreprocessor = createPreprocessor(shaderResources, location);

        try (Reader reader = shader.openAsReader()) {
            String s = IOUtils.toString(reader);
            output.put(new ShaderManager.ShaderSourceKey(resourcelocation, type), String.join("", glslpreprocessor.process(s)));
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load shader source at {}", location, ioexception);
        }
    }

    private static GlslPreprocessor createPreprocessor(final Map<ResourceLocation, Resource> shaderResources, ResourceLocation shaderLocation) {
        final ResourceLocation resourcelocation = shaderLocation.withPath(FileUtil::getFullResourcePath);
        return new GlslPreprocessor() {
            private final Set<ResourceLocation> importedLocations = new ObjectArraySet<>();

            @Override
            public String applyImport(boolean p_366551_, String p_366739_) {
                ResourceLocation resourcelocation1;
                try {
                    if (p_366551_) {
                        resourcelocation1 = resourcelocation.withPath(p_366623_ -> FileUtil.normalizeResourcePath(p_366623_ + p_366739_));
                    } else {
                        resourcelocation1 = ResourceLocation.parse(p_366739_).withPrefix("shaders/include/");
                    }
                } catch (ResourceLocationException resourcelocationexception) {
                    ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", p_366739_, resourcelocationexception.getMessage());
                    return "#error " + resourcelocationexception.getMessage();
                }

                if (!this.importedLocations.add(resourcelocation1)) {
                    return null;
                } else {
                    try {
                        String s;
                        try (Reader reader = shaderResources.get(resourcelocation1).openAsReader()) {
                            s = IOUtils.toString(reader);
                        }

                        return s;
                    } catch (IOException ioexception) {
                        ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", resourcelocation1, ioexception.getMessage());
                        return "#error " + ioexception.getMessage();
                    }
                }
            }
        };
    }

    private static void loadPostChain(ResourceLocation location, Resource postChain, Builder<ResourceLocation, PostChainConfig> output) {
        ResourceLocation resourcelocation = POST_CHAIN_ID_CONVERTER.fileToId(location);

        try (Reader reader = postChain.openAsReader()) {
            JsonElement jsonelement = StrictJsonParser.parse(reader);
            output.put(resourcelocation, PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new));
        } catch (JsonParseException | IOException ioexception) {
            LOGGER.error("Failed to parse post chain at {}", location, ioexception);
        }
    }

    private static boolean isShader(ResourceLocation location) {
        return ShaderType.byLocation(location) != null || location.getPath().endsWith(".glsl");
    }

    protected void apply(ShaderManager.Configs object, ResourceManager resourceManager, ProfilerFiller profiler) {
        ShaderManager.CompilationCache shadermanager$compilationcache = new ShaderManager.CompilationCache(object);
        Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getStaticPipelines());
        List<ResourceLocation> list = new ArrayList<>();
        GpuDevice gpudevice = RenderSystem.getDevice();
        gpudevice.clearPipelineCache();

        for (RenderPipeline renderpipeline : set) {
            CompiledRenderPipeline compiledrenderpipeline = gpudevice.precompilePipeline(renderpipeline, shadermanager$compilationcache::getShaderSource);
            if (!compiledrenderpipeline.isValid()) {
                list.add(renderpipeline.getLocation());
            }
        }

        if (!list.isEmpty()) {
            gpudevice.clearPipelineCache();
            throw new RuntimeException(
                "Failed to load required shader programs:\n" + list.stream().map(p_409066_ -> " - " + p_409066_).collect(Collectors.joining("\n"))
            );
        } else {
            this.compilationCache.close();
            this.compilationCache = shadermanager$compilationcache;
        }
    }

    @Override
    public String getName() {
        return "Shader Loader";
    }

    private void tryTriggerRecovery(Exception exception) {
        if (!this.compilationCache.triggeredRecovery) {
            this.recoveryHandler.accept(exception);
            this.compilationCache.triggeredRecovery = true;
        }
    }

    @Nullable
    public PostChain getPostChain(ResourceLocation id, Set<ResourceLocation> externalTargets) {
        try {
            return this.compilationCache.getOrLoadPostChain(id, externalTargets);
        } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
            LOGGER.error("Failed to load post chain: {}", id, shadermanager$compilationexception);
            this.compilationCache.postChains.put(id, Optional.empty());
            this.tryTriggerRecovery(shadermanager$compilationexception);
            return null;
        }
    }

    @Override
    public void close() {
        this.compilationCache.close();
        this.postChainProjectionMatrixBuffer.close();
    }

    public String getShader(ResourceLocation id, ShaderType type) {
        return this.compilationCache.getShaderSource(id, type);
    }

    @OnlyIn(Dist.CLIENT)
    class CompilationCache implements AutoCloseable {
        private final ShaderManager.Configs configs;
        final Map<ResourceLocation, Optional<PostChain>> postChains = new HashMap<>();
        boolean triggeredRecovery;

        CompilationCache(ShaderManager.Configs configs) {
            this.configs = configs;
        }

        @Nullable
        public PostChain getOrLoadPostChain(ResourceLocation name, Set<ResourceLocation> externalTargets) throws ShaderManager.CompilationException {
            Optional<PostChain> optional = this.postChains.get(name);
            if (optional != null) {
                return optional.orElse(null);
            } else {
                PostChain postchain = this.loadPostChain(name, externalTargets);
                this.postChains.put(name, Optional.of(postchain));
                return postchain;
            }
        }

        private PostChain loadPostChain(ResourceLocation name, Set<ResourceLocation> externalTargets) throws ShaderManager.CompilationException {
            PostChainConfig postchainconfig = this.configs.postChains.get(name);
            if (postchainconfig == null) {
                throw new ShaderManager.CompilationException("Could not find post chain with id: " + name);
            } else {
                return PostChain.load(
                    postchainconfig, ShaderManager.this.textureManager, externalTargets, name, ShaderManager.this.postChainProjectionMatrixBuffer
                );
            }
        }

        @Override
        public void close() {
            this.postChains.values().forEach(p_418047_ -> p_418047_.ifPresent(PostChain::close));
            this.postChains.clear();
        }

        public String getShaderSource(ResourceLocation id, ShaderType type) {
            return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(id, type));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<ResourceLocation, PostChainConfig> postChains) {
        public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
    }

    @OnlyIn(Dist.CLIENT)
    record ShaderSourceKey(ResourceLocation id, ShaderType type) {
        @Override
        public String toString() {
            return this.id + " (" + this.type + ")";
        }
    }
}
