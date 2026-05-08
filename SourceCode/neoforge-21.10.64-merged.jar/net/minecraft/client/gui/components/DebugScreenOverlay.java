package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.DataFixUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debugchart.BandwidthDebugChart;
import net.minecraft.client.gui.components.debugchart.FpsDebugChart;
import net.minecraft.client.gui.components.debugchart.PingDebugChart;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class DebugScreenOverlay {
    private static final float CROSSHAIR_SCALE = 0.01F;
    private static final int CROSHAIR_INDEX_COUNT = 18;
    private static final int COLOR_GREY = -2039584;
    private static final int MARGIN_RIGHT = 2;
    private static final int MARGIN_LEFT = 2;
    private static final int MARGIN_TOP = 2;
    private final Minecraft minecraft;
    private final Font font;
    private final GpuBuffer crosshairBuffer;
    private final RenderSystem.AutoStorageIndexBuffer crosshairIndicies = RenderSystem.getSequentialBuffer(VertexFormat.Mode.LINES);
    @Nullable
    private ChunkPos lastPos;
    @Nullable
    private LevelChunk clientChunk;
    @Nullable
    private CompletableFuture<LevelChunk> serverChunk;
    private boolean renderProfilerChart;
    private boolean renderFpsCharts;
    private boolean renderNetworkCharts;
    private final LocalSampleLogger frameTimeLogger = new LocalSampleLogger(1);
    private final LocalSampleLogger tickTimeLogger = new LocalSampleLogger(TpsDebugDimensions.values().length);
    private final LocalSampleLogger pingLogger = new LocalSampleLogger(1);
    private final LocalSampleLogger bandwidthLogger = new LocalSampleLogger(1);
    private final Map<RemoteDebugSampleType, LocalSampleLogger> remoteSupportingLoggers = Map.of(RemoteDebugSampleType.TICK_TIME, this.tickTimeLogger);
    private final FpsDebugChart fpsChart;
    private final TpsDebugChart tpsChart;
    private final PingDebugChart pingChart;
    private final BandwidthDebugChart bandwidthChart;
    private final ProfilerPieChart profilerPieChart;

    public DebugScreenOverlay(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.font = minecraft.font;
        this.fpsChart = new FpsDebugChart(this.font, this.frameTimeLogger);
        this.tpsChart = new TpsDebugChart(
            this.font, this.tickTimeLogger, () -> minecraft.level == null ? 0.0F : minecraft.level.tickRateManager().millisecondsPerTick()
        );
        this.pingChart = new PingDebugChart(this.font, this.pingLogger);
        this.bandwidthChart = new BandwidthDebugChart(this.font, this.bandwidthLogger);
        this.profilerPieChart = new ProfilerPieChart(this.font);

        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_COLOR_NORMAL.getVertexSize() * 12)) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 1.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 1.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                this.crosshairBuffer = RenderSystem.getDevice().createBuffer(() -> "Crosshair vertex buffer", 32, meshdata.vertexBuffer());
            }
        }
    }

    public void clearChunkCache() {
        this.serverChunk = null;
        this.clientChunk = null;
    }

    public void render(GuiGraphics guiGraphics) {
        if (this.minecraft.isGameLoadFinished() && (!this.minecraft.options.hideGui || this.minecraft.screen != null)) {
            Collection<ResourceLocation> collection = this.minecraft.debugEntries.getCurrentlyEnabled();
            if (!collection.isEmpty()) {
                guiGraphics.nextStratum();
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("debug");
                ChunkPos chunkpos;
                if (this.minecraft.getCameraEntity() != null && this.minecraft.level != null) {
                    BlockPos blockpos = this.minecraft.getCameraEntity().blockPosition();
                    chunkpos = new ChunkPos(blockpos);
                } else {
                    chunkpos = null;
                }

                if (!Objects.equals(this.lastPos, chunkpos)) {
                    this.lastPos = chunkpos;
                    this.clearChunkCache();
                }

                final List<String> list2 = new ArrayList<>();
                final List<String> list = new ArrayList<>();
                final Map<ResourceLocation, Collection<String>> map = new LinkedHashMap<>();
                final List<String> list1 = new ArrayList<>();
                DebugScreenDisplayer debugscreendisplayer = new DebugScreenDisplayer() {
                    @Override
                    public void addPriorityLine(String p_434031_) {
                        if (list2.size() > list.size()) {
                            list.add(p_434031_);
                        } else {
                            list2.add(p_434031_);
                        }
                    }

                    @Override
                    public void addLine(String p_435773_) {
                        list1.add(p_435773_);
                    }

                    @Override
                    public void addToGroup(ResourceLocation p_435639_, Collection<String> p_434033_) {
                        map.computeIfAbsent(p_435639_, p_434304_ -> new ArrayList<>()).addAll(p_434033_);
                    }

                    @Override
                    public void addToGroup(ResourceLocation p_434822_, String p_433985_) {
                        map.computeIfAbsent(p_434822_, p_434009_ -> new ArrayList<>()).add(p_433985_);
                    }
                };
                Level level = this.getLevel();

                for (ResourceLocation resourcelocation : collection) {
                    DebugScreenEntry debugscreenentry = DebugScreenEntries.getEntry(resourcelocation);
                    if (debugscreenentry != null) {
                        debugscreenentry.display(debugscreendisplayer, level, this.getClientChunk(), this.getServerChunk());
                    }
                }

                if (!list2.isEmpty()) {
                    list2.add("");
                }

                if (!list.isEmpty()) {
                    list.add("");
                }

                if (!list1.isEmpty()) {
                    int i = (list1.size() + 1) / 2;
                    list2.addAll(list1.subList(0, i));
                    list.addAll(list1.subList(i, list1.size()));
                    list2.add("");
                    if (i < list1.size()) {
                        list.add("");
                    }
                }

                List<Collection<String>> list3 = new ArrayList<>(map.values());
                if (!list3.isEmpty()) {
                    int j = (list3.size() + 1) / 2;

                    for (int i1 = 0; i1 < list3.size(); i1++) {
                        Collection<String> collection1 = list3.get(i1);
                        if (!collection1.isEmpty()) {
                            if (i1 < j) {
                                list2.addAll(collection1);
                                list2.add("");
                            } else {
                                list.addAll(collection1);
                                list.add("");
                            }
                        }
                    }
                }

                if (this.minecraft.debugEntries.isF3Visible()) {
                    list2.add("");
                    boolean flag = this.minecraft.getSingleplayerServer() != null;
                    list2.add(
                        "Debug charts: [F3+1] Profiler "
                            + (this.renderProfilerChart ? "visible" : "hidden")
                            + "; [F3+2] "
                            + (flag ? "FPS + TPS " : "FPS ")
                            + (this.renderFpsCharts ? "visible" : "hidden")
                            + "; [F3+3] "
                            + (!this.minecraft.isLocalServer() ? "Bandwidth + Ping" : "Ping")
                            + (this.renderNetworkCharts ? " visible" : " hidden")
                    );
                    boolean flag1 = this.minecraft.screen == null || this.minecraft.gui.getChat().isChatFocused();
                    if (this.minecraft.level != null && flag1 && this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
                        list2.add("To edit: press F3 + F6. For help: press F3 + Q");
                    } else {
                        list2.add("To edit: press F3 + F6");
                    }
                }

                this.renderLines(guiGraphics, list2, true);
                this.renderLines(guiGraphics, list, false);
                guiGraphics.nextStratum();
                this.profilerPieChart.setBottomOffset(10);
                if (this.showFpsCharts()) {
                    int k = guiGraphics.guiWidth();
                    int j1 = k / 2;
                    this.fpsChart.drawChart(guiGraphics, 0, this.fpsChart.getWidth(j1));
                    if (this.tickTimeLogger.size() > 0) {
                        int l1 = this.tpsChart.getWidth(j1);
                        this.tpsChart.drawChart(guiGraphics, k - l1, l1);
                    }

                    this.profilerPieChart.setBottomOffset(this.tpsChart.getFullHeight());
                }

                if (this.showNetworkCharts() && this.minecraft.getConnection() != null) {
                    int l = guiGraphics.guiWidth();
                    int k1 = l / 2;
                    if (!this.minecraft.isLocalServer()) {
                        this.bandwidthChart.drawChart(guiGraphics, 0, this.bandwidthChart.getWidth(k1));
                    }

                    int i2 = this.pingChart.getWidth(k1);
                    this.pingChart.drawChart(guiGraphics, l - i2, i2);
                    this.profilerPieChart.setBottomOffset(this.pingChart.getFullHeight());
                }

                if (SharedConstants.DEBUG_CHUNKS) {
                    IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
                    if (integratedserver != null) {
                        ChunkLoadStatusView chunkloadstatusview = integratedserver.createChunkLoadStatusView(16 + ChunkLevel.RADIUS_AROUND_FULL_CHUNK);
                        chunkloadstatusview.moveTo(this.minecraft.player.level().dimension(), this.minecraft.player.chunkPosition());
                        LevelLoadingScreen.renderChunks(guiGraphics, guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() / 2, 4, 1, chunkloadstatusview);
                    }
                }

                try (Zone zone = profilerfiller.zone("profilerPie")) {
                    this.profilerPieChart.render(guiGraphics);
                }

                profilerfiller.pop();
            }
        }
    }

    private void renderLines(GuiGraphics guiGraphics, List<String> lines, boolean leftSide) {
        int i = 9;

        for (int j = 0; j < lines.size(); j++) {
            String s = lines.get(j);
            if (!Strings.isNullOrEmpty(s)) {
                int k = this.font.width(s);
                int l = leftSide ? 2 : guiGraphics.guiWidth() - 2 - k;
                int i1 = 2 + i * j;
                guiGraphics.fill(l - 1, i1 - 1, l + k + 1, i1 + i - 1, -1873784752);
            }
        }

        for (int j1 = 0; j1 < lines.size(); j1++) {
            String s1 = lines.get(j1);
            if (!Strings.isNullOrEmpty(s1)) {
                int k1 = this.font.width(s1);
                int l1 = leftSide ? 2 : guiGraphics.guiWidth() - 2 - k1;
                int i2 = 2 + i * j1;
                guiGraphics.drawString(this.font, s1, l1, i2, -2039584, false);
            }
        }
    }

    @Nullable
    private ServerLevel getServerLevel() {
        if (this.minecraft.level == null) {
            return null;
        } else {
            IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
            return integratedserver != null ? integratedserver.getLevel(this.minecraft.level.dimension()) : null;
        }
    }

    @Nullable
    private Level getLevel() {
        return this.minecraft.level == null
            ? null
            : DataFixUtils.orElse(
                Optional.ofNullable(this.minecraft.getSingleplayerServer())
                    .flatMap(p_428042_ -> Optional.ofNullable(p_428042_.getLevel(this.minecraft.level.dimension()))),
                this.minecraft.level
            );
    }

    @Nullable
    private LevelChunk getServerChunk() {
        if (this.minecraft.level != null && this.lastPos != null) {
            if (this.serverChunk == null) {
                ServerLevel serverlevel = this.getServerLevel();
                if (serverlevel == null) {
                    return null;
                }

                this.serverChunk = serverlevel.getChunkSource()
                    .getChunkFuture(this.lastPos.x, this.lastPos.z, ChunkStatus.FULL, false)
                    .thenApply(p_329714_ -> (LevelChunk)p_329714_.orElse(null));
            }

            return this.serverChunk.getNow(null);
        } else {
            return null;
        }
    }

    @Nullable
    private LevelChunk getClientChunk() {
        if (this.minecraft.level != null && this.lastPos != null) {
            if (this.clientChunk == null) {
                this.clientChunk = this.minecraft.level.getChunk(this.lastPos.x, this.lastPos.z);
            }

            return this.clientChunk;
        } else {
            return null;
        }
    }

    public boolean showDebugScreen() {
        DebugScreenEntryList debugscreenentrylist = this.minecraft.debugEntries;
        return (debugscreenentrylist.isF3Visible() || !debugscreenentrylist.getCurrentlyEnabled().isEmpty())
            && (!this.minecraft.options.hideGui || this.minecraft.screen != null);
    }

    public boolean showProfilerChart() {
        return this.minecraft.debugEntries.isF3Visible() && this.renderProfilerChart;
    }

    public boolean showNetworkCharts() {
        return this.minecraft.debugEntries.isF3Visible() && this.renderNetworkCharts;
    }

    public boolean showFpsCharts() {
        return this.minecraft.debugEntries.isF3Visible() && this.renderFpsCharts;
    }

    public void toggleNetworkCharts() {
        this.renderNetworkCharts = !this.minecraft.debugEntries.isF3Visible() || !this.renderNetworkCharts;
        if (this.renderNetworkCharts) {
            this.minecraft.debugEntries.setF3Visible(true);
            this.renderFpsCharts = false;
        }
    }

    public void toggleFpsCharts() {
        this.renderFpsCharts = !this.minecraft.debugEntries.isF3Visible() || !this.renderFpsCharts;
        if (this.renderFpsCharts) {
            this.minecraft.debugEntries.setF3Visible(true);
            this.renderNetworkCharts = false;
        }
    }

    public void toggleProfilerChart() {
        this.renderProfilerChart = !this.minecraft.debugEntries.isF3Visible() || !this.renderProfilerChart;
        if (this.renderProfilerChart) {
            this.minecraft.debugEntries.setF3Visible(true);
        }
    }

    public void logFrameDuration(long frameDuration) {
        this.frameTimeLogger.logSample(frameDuration);
    }

    public LocalSampleLogger getTickTimeLogger() {
        return this.tickTimeLogger;
    }

    public LocalSampleLogger getPingLogger() {
        return this.pingLogger;
    }

    public LocalSampleLogger getBandwidthLogger() {
        return this.bandwidthLogger;
    }

    public ProfilerPieChart getProfilerPieChart() {
        return this.profilerPieChart;
    }

    public void logRemoteSample(long[] sample, RemoteDebugSampleType sampleType) {
        LocalSampleLogger localsamplelogger = this.remoteSupportingLoggers.get(sampleType);
        if (localsamplelogger != null) {
            localsamplelogger.logFullSample(sample);
        }
    }

    public void reset() {
        this.tickTimeLogger.reset();
        this.pingLogger.reset();
        this.bandwidthLogger.reset();
    }

    public void render3dCrosshair(Camera camera) {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate(0.0F, 0.0F, -1.0F);
        matrix4fstack.rotateX(camera.getXRot() * (float) (Math.PI / 180.0));
        matrix4fstack.rotateY(camera.getYRot() * (float) (Math.PI / 180.0));
        float f = 0.01F * this.minecraft.getWindow().getGuiScale();
        matrix4fstack.scale(-f, f, -f);
        RenderPipeline renderpipeline = RenderPipelines.LINES;
        RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView gputextureview = rendertarget.getColorTextureView();
        GpuTextureView gputextureview1 = rendertarget.getDepthTextureView();
        GpuBuffer gpubuffer = this.crosshairIndicies.getBuffer(18);
        GpuBufferSlice[] agpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransforms(
                new DynamicUniforms.Transform(new Matrix4f(matrix4fstack), new Vector4f(0.0F, 0.0F, 0.0F, 1.0F), new Vector3f(), new Matrix4f(), 4.0F),
                new DynamicUniforms.Transform(new Matrix4f(matrix4fstack), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 2.0F)
            );

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "3d crosshair", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setVertexBuffer(0, this.crosshairBuffer);
            renderpass.setIndexBuffer(gpubuffer, this.crosshairIndicies.type());
            renderpass.setUniform("DynamicTransforms", agpubufferslice[0]);
            renderpass.drawIndexed(0, 0, 18, 1);
            renderpass.setUniform("DynamicTransforms", agpubufferslice[1]);
            renderpass.drawIndexed(0, 0, 18, 1);
        }

        matrix4fstack.popMatrix();
    }
}
