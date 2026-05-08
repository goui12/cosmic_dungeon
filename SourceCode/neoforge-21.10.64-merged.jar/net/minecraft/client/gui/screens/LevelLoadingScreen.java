package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LevelLoadingScreen extends Screen {
    private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
    private static final Component READY_TO_PLAY_TEXT = Component.translatable("narrator.ready_to_play");
    private static final long NARRATION_DELAY_MS = 2000L;
    private static final int PROGRESS_BAR_WIDTH = 200;
    private LevelLoadTracker loadTracker;
    private float smoothedProgress;
    private long lastNarration = -1L;
    private LevelLoadingScreen.Reason reason;
    @Nullable
    private TextureAtlasSprite cachedNetherPortalSprite;
    private static final Object2IntMap<ChunkStatus> COLORS = Util.make(new Object2IntOpenHashMap<>(), p_280803_ -> {
        p_280803_.defaultReturnValue(0);
        p_280803_.put(ChunkStatus.EMPTY, 5526612);
        p_280803_.put(ChunkStatus.STRUCTURE_STARTS, 10066329);
        p_280803_.put(ChunkStatus.STRUCTURE_REFERENCES, 6250897);
        p_280803_.put(ChunkStatus.BIOMES, 8434258);
        p_280803_.put(ChunkStatus.NOISE, 13750737);
        p_280803_.put(ChunkStatus.SURFACE, 7497737);
        p_280803_.put(ChunkStatus.CARVERS, 3159410);
        p_280803_.put(ChunkStatus.FEATURES, 2213376);
        p_280803_.put(ChunkStatus.INITIALIZE_LIGHT, 13421772);
        p_280803_.put(ChunkStatus.LIGHT, 16769184);
        p_280803_.put(ChunkStatus.SPAWN, 15884384);
        p_280803_.put(ChunkStatus.FULL, 16777215);
    });

    public LevelLoadingScreen(LevelLoadTracker loadTracker, LevelLoadingScreen.Reason reason) {
        super(GameNarrator.NO_TITLE);
        this.loadTracker = loadTracker;
        this.reason = reason;
    }

    public void update(LevelLoadTracker laodTracker, LevelLoadingScreen.Reason reason) {
        this.loadTracker = laodTracker;
        this.reason = reason;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    @Override
    protected void updateNarratedWidget(NarrationElementOutput narrationElementOutput) {
        if (this.loadTracker.hasProgress()) {
            narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("loading.progress", Mth.floor(this.loadTracker.serverProgress() * 100.0F)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.smoothedProgress = this.smoothedProgress + (this.loadTracker.serverProgress() - this.smoothedProgress) * 0.2F;
        if (this.loadTracker.isLevelReady()) {
            this.onClose();
        }
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     * @param mouseX      the x-coordinate of the mouse cursor.
     * @param mouseY      the y-coordinate of the mouse cursor.
     * @param partialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        long i = Util.getMillis();
        if (i - this.lastNarration > 2000L) {
            this.lastNarration = i;
            this.triggerImmediateNarration(true);
        }

        int j = this.width / 2;
        int k = this.height / 2;
        ChunkLoadStatusView chunkloadstatusview = this.loadTracker.statusView();
        int l;
        if (chunkloadstatusview != null) {
            int i1 = 2;
            renderChunks(guiGraphics, j, k, 2, 0, chunkloadstatusview);
            l = k - chunkloadstatusview.radius() * 2 - 9 * 3;
        } else {
            l = k - 50;
        }

        guiGraphics.drawCenteredString(this.font, DOWNLOADING_TERRAIN_TEXT, j, l, -1);
        if (this.loadTracker.hasProgress()) {
            this.drawProgressBar(guiGraphics, j - 100, l + 9 + 3, 200, 2, this.smoothedProgress);
        }
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress) {
        guiGraphics.fill(x, y, x + width, y + height, -16777216);
        guiGraphics.fill(x, y, x + Math.round(progress * width), y + height, -16711936);
    }

    public static void renderChunks(GuiGraphics guiGraphics, int x, int y, int size, int spacing, ChunkLoadStatusView statusView) {
        int i = size + spacing;
        int j = statusView.radius() * 2 + 1;
        int k = j * i - spacing;
        int l = x - k / 2;
        int i1 = y - k / 2;
        if (SharedConstants.DEBUG_CHUNKS) {
            int j1 = i / 2 + 1;
            guiGraphics.fill(x - j1, y - j1, x + j1, y + j1, -65536);
        }

        for (int j2 = 0; j2 < j; j2++) {
            for (int k1 = 0; k1 < j; k1++) {
                ChunkStatus chunkstatus = statusView.get(j2, k1);
                int l1 = l + j2 * i;
                int i2 = i1 + k1 * i;
                guiGraphics.fill(l1, i2, l1 + size, i2 + size, ARGB.opaque(COLORS.getInt(chunkstatus)));
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        switch (this.reason) {
            case NETHER_PORTAL:
                guiGraphics.blitSprite(
                    RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND, this.getNetherPortalSprite(), 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()
                );
                break;
            case END_PORTAL:
                TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
                TextureSetup texturesetup = TextureSetup.doubleTexture(
                    texturemanager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION).getTextureView(),
                    texturemanager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION).getTextureView()
                );
                guiGraphics.fill(RenderPipelines.END_PORTAL, texturesetup, 0, 0, this.width, this.height);
                break;
            case OTHER:
                this.renderPanorama(guiGraphics, partialTick);
                this.renderBlurredBackground(guiGraphics);
                this.renderMenuBackground(guiGraphics);
        }
    }

    private TextureAtlasSprite getNetherPortalSprite() {
        if (this.cachedNetherPortalSprite != null) {
            return this.cachedNetherPortalSprite;
        } else {
            this.cachedNetherPortalSprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
            return this.cachedNetherPortalSprite;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.getNarrator().saySystemNow(READY_TO_PLAY_TEXT);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Reason {
        NETHER_PORTAL,
        END_PORTAL,
        OTHER;
    }
}
