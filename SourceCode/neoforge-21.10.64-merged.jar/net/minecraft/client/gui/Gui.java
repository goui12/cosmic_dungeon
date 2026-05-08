package net.minecraft.client.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.Window;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.minecraft.client.gui.contextualbar.JumpableVehicleBarRenderer;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
// Neo: Exceptionally add a static wildcard import to make the patch bearable, and comments to avoid the detection by spotless rules.
/* space for import change */ import static net.neoforged.neoforge.client.gui.VanillaGuiLayers.* /* space for wildcard import */;

@OnlyIn(Dist.CLIENT)
public class Gui {
    private static final ResourceLocation CROSSHAIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair");
    private static final ResourceLocation CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_full");
    private static final ResourceLocation CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace(
        "hud/crosshair_attack_indicator_background"
    );
    private static final ResourceLocation CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace(
        "hud/crosshair_attack_indicator_progress"
    );
    private static final ResourceLocation EFFECT_BACKGROUND_AMBIENT_SPRITE = ResourceLocation.withDefaultNamespace("hud/effect_background_ambient");
    private static final ResourceLocation EFFECT_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/effect_background");
    private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final ResourceLocation HOTBAR_OFFHAND_LEFT_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final ResourceLocation HOTBAR_OFFHAND_RIGHT_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_right");
    private static final ResourceLocation HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace(
        "hud/hotbar_attack_indicator_background"
    );
    private static final ResourceLocation HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace(
        "hud/hotbar_attack_indicator_progress"
    );
    private static final ResourceLocation ARMOR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation FOOD_EMPTY_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty_hunger");
    private static final ResourceLocation FOOD_HALF_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_half_hunger");
    private static final ResourceLocation FOOD_FULL_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full_hunger");
    private static final ResourceLocation FOOD_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation FOOD_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation AIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/air");
    private static final ResourceLocation AIR_POPPING_SPRITE = ResourceLocation.withDefaultNamespace("hud/air_bursting");
    private static final ResourceLocation AIR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/air_empty");
    private static final ResourceLocation HEART_VEHICLE_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/vehicle_container");
    private static final ResourceLocation HEART_VEHICLE_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/vehicle_full");
    private static final ResourceLocation HEART_VEHICLE_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/vehicle_half");
    private static final ResourceLocation VIGNETTE_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    public static final ResourceLocation NAUSEA_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/nausea.png");
    public static final ResourceLocation SPYGLASS_SCOPE_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png");
    private static final ResourceLocation POWDER_SNOW_OUTLINE_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static final Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER = Comparator.comparing(PlayerScoreEntry::value)
        .reversed()
        .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
    private static final Component DEMO_EXPIRED_TEXT = Component.translatable("demo.demoExpired");
    private static final Component SAVING_TEXT = Component.translatable("menu.savingLevel");
    private static final float MIN_CROSSHAIR_ATTACK_SPEED = 5.0F;
    private static final int EXPERIENCE_BAR_DISPLAY_TICKS = 100;
    private static final int NUM_HEARTS_PER_ROW = 10;
    private static final int LINE_HEIGHT = 10;
    private static final String SPACER = ": ";
    private static final float PORTAL_OVERLAY_ALPHA_MIN = 0.2F;
    private static final int HEART_SIZE = 9;
    private static final int HEART_SEPARATION = 8;
    private static final int NUM_AIR_BUBBLES = 10;
    private static final int AIR_BUBBLE_SIZE = 9;
    private static final int AIR_BUBBLE_SEPERATION = 8;
    private static final int AIR_BUBBLE_POPPING_DURATION = 2;
    private static final int EMPTY_AIR_BUBBLE_DELAY_DURATION = 1;
    private static final float AIR_BUBBLE_POP_SOUND_VOLUME_BASE = 0.5F;
    private static final float AIR_BUBBLE_POP_SOUND_VOLUME_INCREMENT = 0.1F;
    private static final float AIR_BUBBLE_POP_SOUND_PITCH_BASE = 1.0F;
    private static final float AIR_BUBBLE_POP_SOUND_PITCH_INCREMENT = 0.1F;
    private static final int NUM_AIR_BUBBLE_POPPED_BEFORE_SOUND_VOLUME_INCREASE = 3;
    private static final int NUM_AIR_BUBBLE_POPPED_BEFORE_SOUND_PITCH_INCREASE = 5;
    private static final float AUTOSAVE_FADE_SPEED_FACTOR = 0.2F;
    private static final int SAVING_INDICATOR_WIDTH_PADDING_RIGHT = 5;
    private static final int SAVING_INDICATOR_HEIGHT_PADDING_BOTTOM = 5;
    private final RandomSource random = RandomSource.create();
    private final Minecraft minecraft;
    private final ChatComponent chat;
    private int tickCount;
    @Nullable
    private Component overlayMessageString;
    private int overlayMessageTime;
    private boolean animateOverlayMessageColor;
    private boolean chatDisabledByPlayerShown;
    public float vignetteBrightness = 1.0F;
    private int toolHighlightTimer;
    private ItemStack lastToolHighlight = ItemStack.EMPTY;
    private final DebugScreenOverlay debugOverlay;
    private final SubtitleOverlay subtitleOverlay;
    /**
     * The spectator GUI for this in-game GUI instance
     */
    private final SpectatorGui spectatorGui;
    private final PlayerTabOverlay tabList;
    private final BossHealthOverlay bossOverlay;
    /**
     * A timer for the current title and subtitle displayed
     */
    private int titleTime;
    /**
     * The current title displayed
     */
    @Nullable
    private Component title;
    /**
     * The current sub-title displayed
     */
    @Nullable
    private Component subtitle;
    /**
     * The time that the title take to fade in
     */
    private int titleFadeInTime;
    /**
     * The time that the title is display
     */
    private int titleStayTime;
    /**
     * The time that the title take to fade out
     */
    private int titleFadeOutTime;
    private int lastHealth;
    private int displayHealth;
    /**
     * The last recorded system time
     */
    private long lastHealthTime;
    /**
     * Used with updateCounter to make the heart bar flash
     */
    private long healthBlinkTime;
    private int lastBubblePopSoundPlayed;
    @Nullable
    private Runnable deferredSubtitles;
    private float autosaveIndicatorValue;
    private float lastAutosaveIndicatorValue;
    private Pair<Gui.ContextualInfo, ContextualBarRenderer> contextualInfoBar = Pair.of(Gui.ContextualInfo.EMPTY, ContextualBarRenderer.EMPTY);
    private final Map<Gui.ContextualInfo, Supplier<ContextualBarRenderer>> contextualInfoBarRenderers;
    private float scopeScale;
    private final net.neoforged.neoforge.client.gui.GuiLayerManager layerManager = new net.neoforged.neoforge.client.gui.GuiLayerManager();
    /**
     * Neo: This variable controls the height of overlays on the left of the hotbar (e.g. health, armor).
     */
    public int leftHeight;
    /**
     * Neo: This variable controls the height of overlays on the right of the hotbar (e.g. food, vehicle health, air).
     */
    public int rightHeight;

    public Gui(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.debugOverlay = new DebugScreenOverlay(minecraft);
        this.spectatorGui = new SpectatorGui(minecraft);
        this.chat = new ChatComponent(minecraft);
        this.tabList = new PlayerTabOverlay(minecraft, this);
        this.bossOverlay = new BossHealthOverlay(minecraft);
        this.subtitleOverlay = new SubtitleOverlay(minecraft);
        this.contextualInfoBarRenderers = ImmutableMap.of(
            Gui.ContextualInfo.EMPTY,
            () -> ContextualBarRenderer.EMPTY,
            Gui.ContextualInfo.EXPERIENCE,
            () -> new ExperienceBarRenderer(minecraft),
            Gui.ContextualInfo.LOCATOR,
            () -> new LocatorBarRenderer(minecraft),
            Gui.ContextualInfo.JUMPABLE_VEHICLE,
            () -> new JumpableVehicleBarRenderer(minecraft)
        );
        this.resetTitleTimes();
        this.registerVanillaLayers();
    }

    public void resetTitleTimes() {
        this.titleFadeInTime = 10;
        this.titleStayTime = 70;
        this.titleFadeOutTime = 20;
    }

    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!(this.minecraft.screen instanceof LevelLoadingScreen)) {
            leftHeight = 39;
            rightHeight = 39;
            updateContextualBarRenderer();
            layerManager.render(guiGraphics, deltaTracker);
        }
    }

    private void registerVanillaLayers() {
        java.util.function.BooleanSupplier guiVisible = () -> !this.minecraft.options.hideGui;
        java.util.function.BooleanSupplier survivalVisible = () -> this.minecraft.gameMode.canHurtPlayer() && guiVisible.getAsBoolean();
        layerManager.add(CAMERA_OVERLAYS, this::renderCameraOverlays, guiVisible);
        layerManager.add(CROSSHAIR, this::renderCrosshair, guiVisible);
        layerManager.add(AFTER_CAMERA_DECORATIONS, (guiGraphics, deltaTracker) -> guiGraphics.nextStratum(), guiVisible);
        // TODO: Split this up again into its pieces
        layerManager.add(HOTBAR, this::renderHotbar, guiVisible);
        layerManager.add(PLAYER_HEALTH, (guiGraphics, partialTick) -> renderHealthLevel(guiGraphics), survivalVisible);
        layerManager.add(ARMOR_LEVEL, (guiGraphics, partialTick) -> renderArmorLevel(guiGraphics), survivalVisible);
        layerManager.add(FOOD_LEVEL, (guiGraphics, partialTick) -> renderFoodLevel(guiGraphics), survivalVisible);
        layerManager.add(VEHICLE_HEALTH, (guiGraphics, partialTick) -> renderVehicleHealth(guiGraphics), guiVisible);
        layerManager.add(AIR_LEVEL, (guiGraphics, partialTick) -> renderAirLevel(guiGraphics), survivalVisible);
        layerManager.add(CONTEXTUAL_INFO_BAR_BACKGROUND, this::renderContextualInfoBarBackground, guiVisible);
        layerManager.add(EXPERIENCE_LEVEL, this::renderExperienceLevel, guiVisible);
        layerManager.add(CONTEXTUAL_INFO_BAR, this::renderContextualInfoBar, guiVisible);
        layerManager.add(SELECTED_ITEM_NAME, this::maybeRenderSelectedItemName, guiVisible);
        layerManager.add(SPECTATOR_TOOLTIP, this::maybeRenderSpectatorTooltip, guiVisible);
        layerManager.add(EFFECTS, this::renderEffects, guiVisible);
        layerManager.add(BOSS_OVERLAY, this::renderBossOverlay, guiVisible);
        layerManager.add(SLEEP_OVERLAY, this::renderSleepOverlay);
        layerManager.add(DEMO_OVERLAY, this::renderDemoOverlay, guiVisible);
        layerManager.add(SCOREBOARD_SIDEBAR, this::renderScoreboardSidebar, guiVisible);
        layerManager.add(OVERLAY_MESSAGE, this::renderOverlayMessage, guiVisible);
        layerManager.add(TITLE, this::renderTitle, guiVisible);
        layerManager.add(CHAT, this::renderChat, guiVisible);
        layerManager.add(TAB_LIST, this::renderTabList, guiVisible);
        layerManager.add(SUBTITLE_OVERLAY, (graphics, deltaTracker) -> {
            if (!this.minecraft.options.hideGui) {
                this.renderSubtitleOverlay(graphics, this.minecraft.screen == null || this.minecraft.screen.isInGameUi());
            } else if (this.minecraft.screen != null && this.minecraft.screen.isInGameUi()) {
                this.renderSubtitleOverlay(graphics, true);
            }
        });
    }

    private void renderBossOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        this.bossOverlay.render(guiGraphics);
    }

    public void renderDebugOverlay(GuiGraphics guiGraphics) {
        this.debugOverlay.render(guiGraphics);
    }

    private void renderSubtitleOverlay(GuiGraphics guiGraphics, boolean deferred) {
        if (deferred) {
            this.deferredSubtitles = () -> this.subtitleOverlay.render(guiGraphics);
        } else {
            this.deferredSubtitles = null;
            this.subtitleOverlay.render(guiGraphics);
        }
    }

    public void renderDeferredSubtitles() {
        if (this.deferredSubtitles != null) {
            this.deferredSubtitles.run();
            this.deferredSubtitles = null;
        }
    }

    private void renderCameraOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (Minecraft.useFancyGraphics()) {
            this.renderVignette(guiGraphics, this.minecraft.getCameraEntity());
        }

        LocalPlayer localplayer = this.minecraft.player;
        float f = deltaTracker.getGameTimeDeltaTicks();
        this.scopeScale = Mth.lerp(0.5F * f, this.scopeScale, 1.125F);
        if (this.minecraft.options.getCameraType().isFirstPerson()) {
            if (localplayer.isScoping()) {
                this.renderSpyglassOverlay(guiGraphics, this.scopeScale);
            } else {
                this.scopeScale = 0.5F;

                for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
                    ItemStack itemstack = localplayer.getItemBySlot(equipmentslot);
                    Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
                    if (equippable != null && equippable.slot() == equipmentslot) {
                        if (equippable.cameraOverlay().isPresent()) {
                            this.renderTextureOverlay(guiGraphics, equippable.cameraOverlay().get().withPath(p_380782_ -> "textures/" + p_380782_ + ".png"), 1.0F);
                        }

                        net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemstack).renderFirstPersonOverlay(itemstack, equipmentslot, this.minecraft.player, guiGraphics, deltaTracker);
                    }
                }
            }
        }

        if (localplayer.getTicksFrozen() > 0) {
            this.renderTextureOverlay(guiGraphics, POWDER_SNOW_OUTLINE_LOCATION, localplayer.getPercentFrozen());
        }

        float f1 = deltaTracker.getGameTimeDeltaPartialTick(false);
        float f2 = Mth.lerp(f1, localplayer.oPortalEffectIntensity, localplayer.portalEffectIntensity);
        float f3 = localplayer.getEffectBlendFactor(MobEffects.NAUSEA, f1);
        if (f2 > 0.0F) {
            this.renderPortalOverlay(guiGraphics, f2);
        } else if (f3 > 0.0F) {
            float f4 = this.minecraft.options.screenEffectScale().get().floatValue();
            if (f4 < 1.0F) {
                float f5 = f3 * (1.0F - f4);
                this.renderConfusionOverlay(guiGraphics, f5);
            }
        }
    }

    private void renderSleepOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (this.minecraft.player.getSleepTimer() > 0) {
            Profiler.get().push("sleep");
            guiGraphics.nextStratum();
            float f = this.minecraft.player.getSleepTimer();
            float f1 = f / 100.0F;
            if (f1 > 1.0F) {
                f1 = 1.0F - (f - 100.0F) / 10.0F;
            }

            int i = (int)(220.0F * f1) << 24 | 1052704;
            guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), i);
            Profiler.get().pop();
        }
    }

    private void renderOverlayMessage(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Font font = this.getFont();
        if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
            Profiler.get().push("overlayMessage");
            float f = this.overlayMessageTime - deltaTracker.getGameTimeDeltaPartialTick(false);
            int i = (int)(f * 255.0F / 20.0F);
            if (i > 255) {
                i = 255;
            }

            if (i > 0) {
                guiGraphics.nextStratum();
                guiGraphics.pose().pushMatrix();
                // Include a shift based on the bar height plus the difference between the height that renderSelectedItemName
                // renders at (59) and the height that the overlay/status bar renders at (68) by default
                int yShift = Math.max(leftHeight, rightHeight) + (68 - 59);
                // If y shift is less than the default y level, just render it at the base y level
                guiGraphics.pose().translate(guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() - Math.max(yShift, 68));
                int j;
                if (this.animateOverlayMessageColor) {
                    j = Mth.hsvToArgb(f / 50.0F, 0.7F, 0.6F, i);
                } else {
                    j = ARGB.color(i, -1);
                }

                int k = font.width(this.overlayMessageString);
                guiGraphics.drawStringWithBackdrop(font, this.overlayMessageString, -k / 2, -4, k, j);
                guiGraphics.pose().popMatrix();
            }

            Profiler.get().pop();
        }
    }

    private void renderTitle(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (this.title != null && this.titleTime > 0) {
            Font font = this.getFont();
            Profiler.get().push("titleAndSubtitle");
            float f = this.titleTime - deltaTracker.getGameTimeDeltaPartialTick(false);
            int i = 255;
            if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
                float f1 = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime - f;
                i = (int)(f1 * 255.0F / this.titleFadeInTime);
            }

            if (this.titleTime <= this.titleFadeOutTime) {
                i = (int)(f * 255.0F / this.titleFadeOutTime);
            }

            i = Mth.clamp(i, 0, 255);
            if (i > 0) {
                guiGraphics.nextStratum();
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() / 2);
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().scale(4.0F, 4.0F);
                int l = font.width(this.title);
                int j = ARGB.color(i, -1);
                guiGraphics.drawStringWithBackdrop(font, this.title, -l / 2, -10, l, j);
                guiGraphics.pose().popMatrix();
                if (this.subtitle != null) {
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().scale(2.0F, 2.0F);
                    int k = font.width(this.subtitle);
                    guiGraphics.drawStringWithBackdrop(font, this.subtitle, -k / 2, 5, k, j);
                    guiGraphics.pose().popMatrix();
                }

                guiGraphics.pose().popMatrix();
            }

            Profiler.get().pop();
        }
    }

    private void renderChat(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!this.chat.isChatFocused()) {
            Window window = this.minecraft.getWindow();
            // Neo: Allow customizing position of chat component
            var chatBottomMargin = 40; // See ChatComponent#BOTTOM_MARGIN (used in translate calls in ChatComponent#render)
            var event = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.Chat(window, guiGraphics, deltaTracker, 0, guiGraphics.guiHeight() - chatBottomMargin)
            );
            // The event is given the absolute Y position; account for chat component's own offsetting here
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(event.getPosX(), (event.getPosY() - guiGraphics.guiHeight() + chatBottomMargin) / (float) this.chat.getScale());
            int i = Mth.floor(this.minecraft.mouseHandler.getScaledXPos(window));
            int j = Mth.floor(this.minecraft.mouseHandler.getScaledYPos(window));
            guiGraphics.nextStratum();
            this.chat.render(guiGraphics, this.tickCount, i, j, false);
            guiGraphics.pose().popMatrix();
        }
    }

    private void renderScoreboardSidebar(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = null;
        PlayerTeam playerteam = scoreboard.getPlayersTeam(this.minecraft.player.getScoreboardName());
        if (playerteam != null) {
            DisplaySlot displayslot = DisplaySlot.teamColorToSlot(playerteam.getColor());
            if (displayslot != null) {
                objective = scoreboard.getDisplayObjective(displayslot);
            }
        }

        Objective objective1 = objective != null ? objective : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective1 != null) {
            guiGraphics.nextStratum();
            this.displayScoreboardSidebar(guiGraphics, objective1);
        }
    }

    private void renderTabList(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (!this.minecraft.options.keyPlayerList.isDown()
            || this.minecraft.isLocalServer() && this.minecraft.player.connection.getListedOnlinePlayers().size() <= 1 && objective == null) {
            this.tabList.setVisible(false);
        } else {
            this.tabList.setVisible(true);
            guiGraphics.nextStratum();
            this.tabList.render(guiGraphics, guiGraphics.guiWidth(), scoreboard, objective);
        }
    }

    private void renderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Options options = this.minecraft.options;
        if (options.getCameraType().isFirstPerson()) {
            if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
                if (!this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
                    guiGraphics.nextStratum();
                    int i = 15;
                    guiGraphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_SPRITE, (guiGraphics.guiWidth() - 15) / 2, (guiGraphics.guiHeight() - 15) / 2, 15, 15);
                    if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                        float f = this.minecraft.player.getAttackStrengthScale(0.0F);
                        boolean flag = false;
                        if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= 1.0F) {
                            flag = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                            flag &= this.minecraft.crosshairPickEntity.isAlive();
                        }

                        int j = guiGraphics.guiHeight() / 2 - 7 + 16;
                        int k = guiGraphics.guiWidth() / 2 - 8;
                        if (flag) {
                            guiGraphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE, k, j, 16, 16);
                        } else if (f < 1.0F) {
                            int l = (int)(f * 17.0F);
                            guiGraphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE, k, j, 16, 4);
                            guiGraphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE, 16, 4, 0, 0, k, j, l, 4);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the crosshair can be rendered for a spectator based on the provided {@link HitResult}.
     * <p>
     * @return {@code true} if the crosshair can be rendered for a spectator, {@code false} otherwise.
     *
     * @param rayTrace the result of a ray trace operation.
     */
    private boolean canRenderCrosshairForSpectator(@Nullable HitResult rayTrace) {
        if (rayTrace == null) {
            return false;
        } else if (rayTrace.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult)rayTrace).getEntity() instanceof MenuProvider;
        } else if (rayTrace.getType() == HitResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)rayTrace).getBlockPos();
            Level level = this.minecraft.level;
            return level.getBlockState(blockpos).getMenuProvider(level, blockpos) != null;
        } else {
            return false;
        }
    }

    private void renderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (!collection.isEmpty() && (this.minecraft.screen == null || !this.minecraft.screen.showsActiveEffects())) {
            int i = 0;
            int j = 0;

            for (MobEffectInstance mobeffectinstance : Ordering.natural().reverse().sortedCopy(collection)) {
                var renderer = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(mobeffectinstance);
                if (!renderer.isVisibleInGui(mobeffectinstance)) continue;
                Holder<MobEffect> holder = mobeffectinstance.getEffect();
                if (mobeffectinstance.showIcon()) {
                    int k = guiGraphics.guiWidth();
                    int l = 1;
                    if (this.minecraft.isDemo()) {
                        l += 15;
                    }

                    if (holder.value().isBeneficial()) {
                        i++;
                        k -= 25 * i;
                    } else {
                        j++;
                        k -= 25 * j;
                        l += 26;
                    }

                    float f = 1.0F;
                    if (mobeffectinstance.isAmbient()) {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_AMBIENT_SPRITE, k, l, 24, 24);
                    } else {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_SPRITE, k, l, 24, 24);
                        if (mobeffectinstance.endsWithin(200)) {
                            int i1 = mobeffectinstance.getDuration();
                            int j1 = 10 - i1 / 20;
                            f = Mth.clamp(i1 / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                                + Mth.cos(i1 * (float) Math.PI / 5.0F) * Mth.clamp(j1 / 10.0F * 0.25F, 0.0F, 0.25F);
                            f = Mth.clamp(f, 0.0F, 1.0F);
                        }
                    }

                    if (renderer.renderGuiIcon(mobeffectinstance, this, guiGraphics, k, l, 0, f)) continue;
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, getMobEffectSprite(holder), k + 3, l + 3, 18, 18, ARGB.white(f));
                }
            }
        }
    }

    public static ResourceLocation getMobEffectSprite(Holder<MobEffect> effect) {
        return effect.unwrapKey()
            .map(ResourceKey::location)
            .map(p_423152_ -> p_423152_.withPrefix("mob_effect/"))
            .orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    // Neo: Split off into separate method to wrap in a layer
    private void renderHotbar(GuiGraphics p_316628_, DeltaTracker p_348543_) {
        if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            this.spectatorGui.renderHotbar(p_316628_);
        } else {
            this.renderItemHotbar(p_316628_, p_348543_);
        }
    }

    // Neo: Wrap in a method to call it when re-rendering
    private void updateContextualBarRenderer() {
        Gui.ContextualInfo gui$contextualinfo = this.nextContextualInfoState();
        if (gui$contextualinfo != this.contextualInfoBar.getKey()) {
            this.contextualInfoBar = Pair.of(gui$contextualinfo, this.contextualInfoBarRenderers.get(gui$contextualinfo).get());
        }
    }

    // Neo: Split off into separate method to wrap in a layer
    private void renderContextualInfoBarBackground(GuiGraphics p_316628_, DeltaTracker p_348543_)
    {
        this.contextualInfoBar.getValue().renderBackground(p_316628_, p_348543_);
    }

    // Neo: Split off into separate method to wrap in a layer
    private void renderExperienceLevel(GuiGraphics p_316628_, DeltaTracker p_348543_) {
        if (this.minecraft.gameMode.hasExperience() && this.minecraft.player.experienceLevel > 0) {
            ContextualBarRenderer.renderExperienceLevel(p_316628_, this.minecraft.font, this.minecraft.player.experienceLevel);
        }
    }

    // Neo: Split off into separate method to wrap in a layer
    private void renderContextualInfoBar(GuiGraphics p_316628_, DeltaTracker p_348543_) {
        this.contextualInfoBar.getValue().render(p_316628_, p_348543_);
    }

    // Neo: Split off into separate method to wrap in a layer
    private void maybeRenderSelectedItemName(GuiGraphics p_316628_, DeltaTracker p_348543_) {
        if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.renderSelectedItemName(p_316628_, Math.max(this.leftHeight, this.rightHeight));
        }
    }

    // Neo: Split off into separate method to wrap in a layer
    private void maybeRenderSpectatorTooltip(GuiGraphics p_316628_, DeltaTracker p_348543_) {
        // Neo: Note this is originally mutually exclusive with the condition in maybeRenderSelectedItemName
        if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR && this.minecraft.player.isSpectator()) {
            this.spectatorGui.renderAction(p_316628_);
        }
    }

    private void renderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            ItemStack itemstack = player.getOffhandItem();
            HumanoidArm humanoidarm = player.getMainArm().getOpposite();
            int i = guiGraphics.guiWidth() / 2;
            int j = 182;
            int k = 91;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, i - 91, guiGraphics.guiHeight() - 22, 182, 22);
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_SELECTION_SPRITE,
                i - 91 - 1 + player.getInventory().getSelectedSlot() * 20,
                guiGraphics.guiHeight() - 22 - 1,
                24,
                23
            );
            if (!itemstack.isEmpty()) {
                if (humanoidarm == HumanoidArm.LEFT) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, i - 91 - 29, guiGraphics.guiHeight() - 23, 29, 24);
                } else {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_SPRITE, i + 91, guiGraphics.guiHeight() - 23, 29, 24);
                }
            }

            int l = 1;

            for (int i1 = 0; i1 < 9; i1++) {
                int j1 = i - 90 + i1 * 20 + 2;
                int k1 = guiGraphics.guiHeight() - 16 - 3;
                this.renderSlot(guiGraphics, j1, k1, deltaTracker, player, player.getInventory().getItem(i1), l++);
            }

            if (!itemstack.isEmpty()) {
                int i2 = guiGraphics.guiHeight() - 16 - 3;
                if (humanoidarm == HumanoidArm.LEFT) {
                    this.renderSlot(guiGraphics, i - 91 - 26, i2, deltaTracker, player, itemstack, l++);
                } else {
                    this.renderSlot(guiGraphics, i + 91 + 10, i2, deltaTracker, player, itemstack, l++);
                }
            }

            if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
                float f = this.minecraft.player.getAttackStrengthScale(0.0F);
                if (f < 1.0F) {
                    int j2 = guiGraphics.guiHeight() - 20;
                    int k2 = i + 91 + 6;
                    if (humanoidarm == HumanoidArm.RIGHT) {
                        k2 = i - 91 - 22;
                    }

                    int l1 = (int)(f * 19.0F);
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE, k2, j2, 18, 18);
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE, 18, 18, 0, 18 - l1, k2, j2 + 18 - l1, 18, l1);
                }
            }
        }
    }

    /**
     * Renders the name of the selected item on the screen using the provided GuiGraphics object.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     */
    private void renderSelectedItemName(GuiGraphics guiGraphics) {
        renderSelectedItemName(guiGraphics, 0);
    }

    /**
     * Renders the name of the selected item on the screen using the provided GuiGraphics object.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     */
    private void renderSelectedItemName(GuiGraphics guiGraphics, int yShift) {
        Profiler.get().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            MutableComponent mutablecomponent = Component.empty()
                .append(this.lastToolHighlight.getHoverName())
                .withStyle(this.lastToolHighlight.getRarity().getStyleModifier());
            if (this.lastToolHighlight.has(DataComponents.CUSTOM_NAME)) {
                mutablecomponent.withStyle(ChatFormatting.ITALIC);
            }

            Component highlightTip = this.lastToolHighlight.getHighlightTip(mutablecomponent);
            int i = this.getFont().width(highlightTip);
            int j = (guiGraphics.guiWidth() - i) / 2;
            int k = guiGraphics.guiHeight() - Math.max(yShift, 59);
            if (!this.minecraft.gameMode.canHurtPlayer()) {
                k += 14;
            }

            int l = (int)(this.toolHighlightTimer * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                Font font = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(lastToolHighlight).getFont(lastToolHighlight, net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext.SELECTED_ITEM_NAME);
                if (font == null) {
                    guiGraphics.drawStringWithBackdrop(this.getFont(), highlightTip, j, k, i, ARGB.color(l, -1));
                } else {
                    j = (guiGraphics.guiWidth() - font.width(highlightTip)) / 2;
                    guiGraphics.drawStringWithBackdrop(font, highlightTip, j, k, i, ARGB.color(l, -1));
                }
            }
        }

        Profiler.get().pop();
    }

    private void renderDemoOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (this.minecraft.isDemo() && !this.getDebugOverlay().showDebugScreen()) { // Neo: Hide demo timer when F3 debug overlay is open; fixes MC-271166
            Profiler.get().push("demo");
            guiGraphics.nextStratum();
            Component component;
            if (this.minecraft.level.getGameTime() >= 120500L) {
                component = DEMO_EXPIRED_TEXT;
            } else {
                component = Component.translatable(
                    "demo.remainingTime",
                    StringUtil.formatTickDuration((int)(120500L - this.minecraft.level.getGameTime()), this.minecraft.level.tickRateManager().tickrate())
                );
            }

            int i = this.getFont().width(component);
            int j = guiGraphics.guiWidth() - i - 10;
            int k = 5;
            guiGraphics.drawStringWithBackdrop(this.getFont(), component, j, 5, i, -1);
            Profiler.get().pop();
        }
    }

    /**
     * Displays the scoreboard sidebar on the screen using the provided GuiGraphics object and objective.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     * @param objective   the objective representing the scoreboard sidebar.
     */
    private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective) {
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberformat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

        @OnlyIn(Dist.CLIENT)
        record DisplayEntry(Component name, Component score, int scoreWidth) {
        }

        DisplayEntry[] agui$1displayentry = scoreboard.listPlayerScores(objective)
            .stream()
            .filter(p_313419_ -> !p_313419_.isHidden())
            .sorted(SCORE_DISPLAY_ORDER)
            .limit(15L)
            .map(p_313418_ -> {
                PlayerTeam playerteam = scoreboard.getPlayersTeam(p_313418_.owner());
                Component component1 = p_313418_.ownerName();
                Component component2 = PlayerTeam.formatNameForTeam(playerteam, component1);
                Component component3 = p_313418_.formatValue(numberformat);
                int k3 = this.getFont().width(component3);
                return new DisplayEntry(component2, component3, k3);
            })
            .toArray(DisplayEntry[]::new);
        Component component = objective.getDisplayName();
        int i = this.getFont().width(component);
        int j = i;
        int k = this.getFont().width(": ");

        for (DisplayEntry gui$1displayentry : agui$1displayentry) {
            j = Math.max(j, this.getFont().width(gui$1displayentry.name) + (gui$1displayentry.scoreWidth > 0 ? k + gui$1displayentry.scoreWidth : 0));
        }

        int l2 = agui$1displayentry.length;
        int i3 = l2 * 9;
        int j3 = guiGraphics.guiHeight() / 2 + i3 / 3;
        int l = 3;
        int i1 = guiGraphics.guiWidth() - j - 3;
        int j1 = guiGraphics.guiWidth() - 3 + 2;
        int k1 = this.minecraft.options.getBackgroundColor(0.3F);
        int l1 = this.minecraft.options.getBackgroundColor(0.4F);
        int i2 = j3 - l2 * 9;
        guiGraphics.fill(i1 - 2, i2 - 9 - 1, j1, i2 - 1, l1);
        guiGraphics.fill(i1 - 2, i2 - 1, j1, j3, k1);
        guiGraphics.drawString(this.getFont(), component, i1 + j / 2 - i / 2, i2 - 9, -1, false);

        for (int j2 = 0; j2 < l2; j2++) {
            DisplayEntry gui$1displayentry1 = agui$1displayentry[j2];
            int k2 = j3 - (l2 - j2) * 9;
            guiGraphics.drawString(this.getFont(), gui$1displayentry1.name, i1, k2, -1, false);
            guiGraphics.drawString(this.getFont(), gui$1displayentry1.score, j1 - gui$1displayentry1.scoreWidth, k2, -1, false);
        }
    }

    @Nullable
    private Player getCameraPlayer() {
        return this.minecraft.getCameraEntity() instanceof Player player ? player : null;
    }

    @Nullable
    private LivingEntity getPlayerVehicleWithHealth() {
        Player player = this.getCameraPlayer();
        if (player != null) {
            Entity entity = player.getVehicle();
            if (entity == null) {
                return null;
            }

            if (entity instanceof LivingEntity) {
                return (LivingEntity)entity;
            }
        }

        return null;
    }

    /**
     * Retrieves the maximum number of hearts representing the vehicle's health for the given mount entity.
     * <p>
     * @return the maximum number of hearts representing the vehicle's health, or 0 if the mount entity is null or does not show vehicle health.
     *
     * @param vehicle the living entity representing the vehicle.
     */
    private int getVehicleMaxHearts(@Nullable LivingEntity vehicle) {
        if (vehicle != null && vehicle.showVehicleHealth()) {
            float f = vehicle.getMaxHealth();
            int i = (int)(f + 0.5F) / 2;
            if (i > 30) {
                i = 30;
            }

            return i;
        } else {
            return 0;
        }
    }

    /**
     * Retrieves the number of rows of visible hearts needed to represent the given mount health.
     * <p>
     * @return the number of rows of visible hearts needed to represent the mount health.
     *
     * @param vehicleHealth the health of the mount entity.
     */
    private int getVisibleVehicleHeartRows(int vehicleHealth) {
        return (int)Math.ceil(vehicleHealth / 10.0);
    }

    /**
     * Renders the player's health, armor, food, and air bars on the screen.
     *
     * @param guiGraphics the graphics object used for rendering.
     */
    @Deprecated // Neo: Split up into different layers
    private void renderPlayerHealth(GuiGraphics guiGraphics) {
        renderHealthLevel(guiGraphics);
        renderArmorLevel(guiGraphics);
        renderFoodLevel(guiGraphics);
        renderAirLevel(guiGraphics);
    }

    private void renderHealthLevel(GuiGraphics p_283143_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            int i = Mth.ceil(player.getHealth());
            boolean flag = this.healthBlinkTime > this.tickCount && (this.healthBlinkTime - this.tickCount) / 3L % 2L == 1L;
            long j = Util.getMillis();
            if (i < this.lastHealth && player.invulnerableTime > 0) {
                this.lastHealthTime = j;
                this.healthBlinkTime = this.tickCount + 20;
            } else if (i > this.lastHealth && player.invulnerableTime > 0) {
                this.lastHealthTime = j;
                this.healthBlinkTime = this.tickCount + 10;
            }

            if (j - this.lastHealthTime > 1000L) {
                this.displayHealth = i;
                this.lastHealthTime = j;
            }

            this.lastHealth = i;
            int k = this.displayHealth;
            this.random.setSeed(this.tickCount * 312871);
            int l = p_283143_.guiWidth() / 2 - 91;
            int i1 = p_283143_.guiWidth() / 2 + 91;
            int j1 = p_283143_.guiHeight() - leftHeight;
            float f = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(k, i));
            int k1 = Mth.ceil(player.getAbsorptionAmount());
            int l1 = Mth.ceil((f + k1) / 2.0F / 10.0F);
            int i2 = Math.max(10 - (l1 - 2), 3);
            int j2 = j1 - 10;
            leftHeight += (l1 - 1) * i2 + 10;
            int k2 = -1;
            if (player.hasEffect(MobEffects.REGENERATION)) {
                k2 = this.tickCount % Mth.ceil(f + 5.0F);
            }
            Profiler.get().push("health");
            this.renderHearts(p_283143_, player, l, j1, i2, k2, f, i, k, k1, flag);
            Profiler.get().pop();
        }
    }

    private void renderArmorLevel(GuiGraphics p_283143_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            int l = p_283143_.guiWidth() / 2 - 91;
            Profiler.get().push("armor");
            renderArmor(p_283143_, player, p_283143_.guiHeight() - leftHeight + 10, 1, 0, l);
            Profiler.get().pop();
            if (player.getArmorValue() > 0) {
                leftHeight += 10;
            }
        }
    }

    private void renderFoodLevel(GuiGraphics p_283143_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            LivingEntity livingentity = this.getPlayerVehicleWithHealth();
            int l2 = this.getVehicleMaxHearts(livingentity);
            if (l2 == 0) {
                Profiler.get().push("food");
                int i1 = p_283143_.guiWidth() / 2 + 91;
                int j1 = p_283143_.guiHeight() - rightHeight;
                this.renderFood(p_283143_, player, j1, i1);
                rightHeight += 10;
                Profiler.get().pop();
            }
        }
    }

    private void renderAirLevel(GuiGraphics p_283143_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            int i1 = p_283143_.guiWidth() / 2 + 91;
            int j2 = p_283143_.guiHeight() - rightHeight;
            Profiler.get().push("air");
            this.renderAirBubbles(p_283143_, player, 10, j2, i1);
            Profiler.get().pop();
        }
    }

    private static void renderArmor(GuiGraphics guiGraphics, Player player, int y, int heartRows, int height, int x) {
        int i = player.getArmorValue();
        if (i > 0) {
            int j = y - (heartRows - 1) * height - 10;

            for (int k = 0; k < 10; k++) {
                int l = x + k * 8;
                if (k * 2 + 1 < i) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_SPRITE, l, j, 9, 9);
                }

                if (k * 2 + 1 == i) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_SPRITE, l, j, 9, 9);
                }

                if (k * 2 + 1 > i) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_SPRITE, l, j, 9, 9);
                }
            }
        }
    }

    /**
     * Renders the player's hearts, including health, absorption, and highlight hearts, on the screen.
     *
     * @param guiGraphics      the graphics object used for rendering.
     * @param player           the player entity.
     * @param x                the x-coordinate of the hearts' position.
     * @param y                the y-coordinate of the hearts' position.
     * @param height           the height of each heart.
     * @param offsetHeartIndex the index of the offset heart.
     * @param maxHealth        the maximum health of the player.
     * @param currentHealth    the current health of the player.
     * @param displayHealth    the displayed health of the player.
     * @param absorptionAmount the absorption amount of the player.
     * @param renderHighlight  determines whether to render the highlight hearts.
     */
    private void renderHearts(
        GuiGraphics guiGraphics,
        Player player,
        int x,
        int y,
        int height,
        int offsetHeartIndex,
        float maxHealth,
        int currentHealth,
        int displayHealth,
        int absorptionAmount,
        boolean renderHighlight
    ) {
        Gui.HeartType gui$hearttype = Gui.HeartType.forPlayer(player);
        boolean flag = player.level().getLevelData().isHardcore();
        int i = Mth.ceil(maxHealth / 2.0);
        int j = Mth.ceil(absorptionAmount / 2.0);
        int k = i * 2;

        for (int l = i + j - 1; l >= 0; l--) {
            int i1 = l / 10;
            int j1 = l % 10;
            int k1 = x + j1 * 8;
            int l1 = y - i1 * height;
            if (currentHealth + absorptionAmount <= 4) {
                l1 += this.random.nextInt(2);
            }

            if (l < i && l == offsetHeartIndex) {
                l1 -= 2;
            }

            this.renderHeart(guiGraphics, Gui.HeartType.CONTAINER, k1, l1, flag, renderHighlight, false);
            int i2 = l * 2;
            boolean flag1 = l >= i;
            if (flag1) {
                int j2 = i2 - k;
                if (j2 < absorptionAmount) {
                    boolean flag2 = j2 + 1 == absorptionAmount;
                    this.renderHeart(guiGraphics, gui$hearttype == Gui.HeartType.WITHERED ? gui$hearttype : Gui.HeartType.ABSORBING, k1, l1, flag, false, flag2);
                }
            }

            if (renderHighlight && i2 < displayHealth) {
                boolean flag3 = i2 + 1 == displayHealth;
                this.renderHeart(guiGraphics, gui$hearttype, k1, l1, flag, true, flag3);
            }

            if (i2 < currentHealth) {
                boolean flag4 = i2 + 1 == currentHealth;
                this.renderHeart(guiGraphics, gui$hearttype, k1, l1, flag, false, flag4);
            }
        }
    }

    private void renderHeart(
        GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean hardcore, boolean halfHeart, boolean blinking
    ) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, heartType.getSprite(hardcore, blinking, halfHeart), x, y, 9, 9);
    }

    private void renderAirBubbles(GuiGraphics guiGraphics, Player player, int vehicleMaxHealth, int y, int x) {
        int i = player.getMaxAirSupply();
        int j = Math.clamp((long)player.getAirSupply(), 0, i);
        boolean flag = player.isEyeInFluid(FluidTags.WATER);
        if (flag || j < i) {
            y = this.getAirBubbleYLine(vehicleMaxHealth, y);
            int k = getCurrentAirSupplyBubble(j, i, -2);
            int l = getCurrentAirSupplyBubble(j, i, 0);
            int i1 = 10 - getCurrentAirSupplyBubble(j, i, getEmptyBubbleDelayDuration(j, flag));
            boolean flag1 = k != l;
            if (!flag) {
                this.lastBubblePopSoundPlayed = 0;
            }

            for (int j1 = 1; j1 <= 10; j1++) {
                int k1 = x - (j1 - 1) * 8 - 9;
                if (j1 <= k) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_SPRITE, k1, y, 9, 9);
                } else if (flag1 && j1 == l && flag) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_POPPING_SPRITE, k1, y, 9, 9);
                    this.playAirBubblePoppedSound(j1, player, i1);
                } else if (j1 > 10 - i1) {
                    int l1 = i1 == 10 && this.tickCount % 2 == 0 ? this.random.nextInt(2) : 0;
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_EMPTY_SPRITE, k1, y + l1, 9, 9);
                }
            }

            rightHeight += 10;
        }
    }

    private int getAirBubbleYLine(int vehicleMaxHealth, int startX) {
        int i = this.getVisibleVehicleHeartRows(vehicleMaxHealth) - 1;
        return startX - i * 10;
    }

    private static int getCurrentAirSupplyBubble(int currentAirSupply, int maxAirSupply, int offset) {
        return Mth.ceil((float)((currentAirSupply + offset) * 10) / maxAirSupply);
    }

    private static int getEmptyBubbleDelayDuration(int airSupply, boolean inWater) {
        return airSupply != 0 && inWater ? 1 : 0;
    }

    private void playAirBubblePoppedSound(int bubble, Player player, int pitch) {
        if (this.lastBubblePopSoundPlayed != bubble) {
            float f = 0.5F + 0.1F * Math.max(0, pitch - 3 + 1);
            float f1 = 1.0F + 0.1F * Math.max(0, pitch - 5 + 1);
            player.playSound(SoundEvents.BUBBLE_POP, f, f1);
            this.lastBubblePopSoundPlayed = bubble;
        }
    }

    private void renderFood(GuiGraphics guiGraphics, Player player, int y, int x) {
        FoodData fooddata = player.getFoodData();
        int i = fooddata.getFoodLevel();

        for (int j = 0; j < 10; j++) {
            int k = y;
            ResourceLocation resourcelocation;
            ResourceLocation resourcelocation1;
            ResourceLocation resourcelocation2;
            if (player.hasEffect(MobEffects.HUNGER)) {
                resourcelocation = FOOD_EMPTY_HUNGER_SPRITE;
                resourcelocation1 = FOOD_HALF_HUNGER_SPRITE;
                resourcelocation2 = FOOD_FULL_HUNGER_SPRITE;
            } else {
                resourcelocation = FOOD_EMPTY_SPRITE;
                resourcelocation1 = FOOD_HALF_SPRITE;
                resourcelocation2 = FOOD_FULL_SPRITE;
            }

            if (player.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (i * 3 + 1) == 0) {
                k = y + (this.random.nextInt(3) - 1);
            }

            int l = x - j * 8 - 9;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, l, k, 9, 9);
            if (j * 2 + 1 < i) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, l, k, 9, 9);
            }

            if (j * 2 + 1 == i) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation1, l, k, 9, 9);
            }
        }
    }

    /**
     * Renders the health of the player's vehicle on the screen.
     *
     * @param guiGraphics the graphics object used for rendering.
     */
    private void renderVehicleHealth(GuiGraphics guiGraphics) {
        LivingEntity livingentity = this.getPlayerVehicleWithHealth();
        if (livingentity != null) {
            int i = this.getVehicleMaxHearts(livingentity);
            if (i != 0) {
                int j = (int)Math.ceil(livingentity.getHealth());
                Profiler.get().popPush("mountHealth");
                int k = guiGraphics.guiHeight() - rightHeight;
                int l = guiGraphics.guiWidth() / 2 + 91;
                int i1 = k;

                for (int j1 = 0; i > 0; j1 += 20) {
                    int k1 = Math.min(i, 10);
                    i -= k1;

                    for (int l1 = 0; l1 < k1; l1++) {
                        int i2 = l - l1 * 8 - 9;
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_CONTAINER_SPRITE, i2, i1, 9, 9);
                        if (l1 * 2 + 1 + j1 < j) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_FULL_SPRITE, i2, i1, 9, 9);
                        }

                        if (l1 * 2 + 1 + j1 == j) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_HALF_SPRITE, i2, i1, 9, 9);
                        }
                    }

                    i1 -= 10;
                    rightHeight += 10;
                }
            }
        }
    }

    /**
     * Renders a texture overlay on the screen with the specified shader location and alpha value.
     *
     * @param guiGraphics    the graphics object used for rendering.
     * @param shaderLocation the location of the shader texture.
     * @param alpha          the alpha value to apply to the overlay.
     */
    private void renderTextureOverlay(GuiGraphics guiGraphics, ResourceLocation shaderLocation, float alpha) {
        int i = ARGB.white(alpha);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            shaderLocation,
            0,
            0,
            0.0F,
            0.0F,
            guiGraphics.guiWidth(),
            guiGraphics.guiHeight(),
            guiGraphics.guiWidth(),
            guiGraphics.guiHeight(),
            i
        );
    }

    /**
     * Renders the overlay for the spyglass effect.
     *
     * @param guiGraphics the graphics object used for rendering.
     * @param scopeScale  the scale factor for the spyglass scope.
     */
    private void renderSpyglassOverlay(GuiGraphics guiGraphics, float scopeScale) {
        float f = Math.min(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        float f1 = Math.min(guiGraphics.guiWidth() / f, guiGraphics.guiHeight() / f) * scopeScale;
        int i = Mth.floor(f * f1);
        int j = Mth.floor(f * f1);
        int k = (guiGraphics.guiWidth() - i) / 2;
        int l = (guiGraphics.guiHeight() - j) / 2;
        int i1 = k + i;
        int j1 = l + j;
        ItemStack useItem = this.minecraft.player.getUseItem();
        var clientItemExtensions = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(useItem);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, clientItemExtensions.getScopeOverlayTexture(useItem), k, l, 0.0F, 0.0F, i, j, i, j);
        guiGraphics.fill(RenderPipelines.GUI, 0, j1, guiGraphics.guiWidth(), guiGraphics.guiHeight(), -16777216);
        guiGraphics.fill(RenderPipelines.GUI, 0, 0, guiGraphics.guiWidth(), l, -16777216);
        guiGraphics.fill(RenderPipelines.GUI, 0, l, k, j1, -16777216);
        guiGraphics.fill(RenderPipelines.GUI, i1, l, guiGraphics.guiWidth(), j1, -16777216);
    }

    /**
     * Updates the brightness of the vignette effect based on the brightness of the given entity's position.
     *
     * @param entity the entity used to determine the brightness.
     */
    private void updateVignetteBrightness(Entity entity) {
        BlockPos blockpos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        float f = LightTexture.getBrightness(entity.level().dimensionType(), entity.level().getMaxLocalRawBrightness(blockpos));
        float f1 = Mth.clamp(1.0F - f, 0.0F, 1.0F);
        this.vignetteBrightness = this.vignetteBrightness + (f1 - this.vignetteBrightness) * 0.01F;
    }

    /**
     * Renders the vignette effect on the screen based on the distance to the world border and the entity's position.
     *
     * @param guiGraphics the graphics object used for rendering.
     * @param entity      the entity used to determine the distance to the world
     *                    border.
     */
    private void renderVignette(GuiGraphics guiGraphics, @Nullable Entity entity) {
        WorldBorder worldborder = this.minecraft.level.getWorldBorder();
        float f = 0.0F;
        if (entity != null) {
            float f1 = (float)worldborder.getDistanceToBorder(entity);
            double d0 = Math.min(
                worldborder.getLerpSpeed() * worldborder.getWarningTime() * 1000.0, Math.abs(worldborder.getLerpTarget() - worldborder.getSize())
            );
            double d1 = Math.max((double)worldborder.getWarningBlocks(), d0);
            if (f1 < d1) {
                f = 1.0F - (float)(f1 / d1);
            }
        }

        int i;
        if (f > 0.0F) {
            f = Mth.clamp(f, 0.0F, 1.0F);
            i = ARGB.colorFromFloat(1.0F, 0.0F, f, f);
        } else {
            float f2 = this.vignetteBrightness;
            f2 = Mth.clamp(f2, 0.0F, 1.0F);
            i = ARGB.colorFromFloat(1.0F, f2, f2, f2);
        }

        guiGraphics.blit(
            RenderPipelines.VIGNETTE,
            VIGNETTE_LOCATION,
            0,
            0,
            0.0F,
            0.0F,
            guiGraphics.guiWidth(),
            guiGraphics.guiHeight(),
            guiGraphics.guiWidth(),
            guiGraphics.guiHeight(),
            i
        );
    }

    /**
     * Renders the portal overlay effect on the screen with the specified alpha value.
     *
     * @param guiGraphics the graphics object used for rendering.
     * @param alpha       the alpha value of the overlay.
     */
    private void renderPortalOverlay(GuiGraphics guiGraphics, float alpha) {
        if (alpha < 1.0F) {
            alpha *= alpha;
            alpha *= alpha;
            alpha = alpha * 0.8F + 0.2F;
        }

        int i = ARGB.white(alpha);
        TextureAtlasSprite textureatlassprite = this.minecraft
            .getBlockRenderer()
            .getBlockModelShaper()
            .getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, textureatlassprite, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), i);
    }

    private void renderConfusionOverlay(GuiGraphics guiGraphics, float intensity) {
        int i = guiGraphics.guiWidth();
        int j = guiGraphics.guiHeight();
        guiGraphics.pose().pushMatrix();
        float f = Mth.lerp(intensity, 2.0F, 1.0F);
        guiGraphics.pose().translate(i / 2.0F, j / 2.0F);
        guiGraphics.pose().scale(f, f);
        guiGraphics.pose().translate(-i / 2.0F, -j / 2.0F);
        float f1 = 0.2F * intensity;
        float f2 = 0.4F * intensity;
        float f3 = 0.2F * intensity;
        guiGraphics.blit(RenderPipelines.GUI_NAUSEA_OVERLAY, NAUSEA_LOCATION, 0, 0, 0.0F, 0.0F, i, j, i, j, ARGB.colorFromFloat(1.0F, f1, f2, f3));
        guiGraphics.pose().popMatrix();
    }

    private void renderSlot(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed) {
        if (!stack.isEmpty()) {
            float f = stack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
            if (f > 0.0F) {
                float f1 = 1.0F + f / 5.0F;
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(x + 8, y + 12);
                guiGraphics.pose().scale(1.0F / f1, (f1 + 1.0F) / 2.0F);
                guiGraphics.pose().translate(-(x + 8), -(y + 12));
            }

            guiGraphics.renderItem(player, stack, x, y, seed);
            if (f > 0.0F) {
                guiGraphics.pose().popMatrix();
            }

            guiGraphics.renderItemDecorations(this.minecraft.font, stack, x, y);
        }
    }

    /**
     * Advances the tick for the autosave indicator and optionally ticks the object if not paused.
     */
    public void tick(boolean pause) {
        this.tickAutosaveIndicator();
        if (!pause) {
            this.tick();
        }
    }

    private void tick() {
        if (this.overlayMessageTime > 0) {
            this.overlayMessageTime--;
        }

        if (this.titleTime > 0) {
            this.titleTime--;
            if (this.titleTime <= 0) {
                this.title = null;
                this.subtitle = null;
            }
        }

        this.tickCount++;
        Entity entity = this.minecraft.getCameraEntity();
        if (entity != null) {
            this.updateVignetteBrightness(entity);
        }

        if (this.minecraft.player != null) {
            ItemStack itemstack = this.minecraft.player.getInventory().getSelectedItem();
            if (itemstack.isEmpty()) {
                this.toolHighlightTimer = 0;
            } else if (this.lastToolHighlight.isEmpty()
                || !itemstack.is(this.lastToolHighlight.getItem())
                || (!itemstack.getHoverName().equals(this.lastToolHighlight.getHoverName()) || !itemstack.getHighlightTip(itemstack.getHoverName()).equals(this.lastToolHighlight.getHighlightTip(this.lastToolHighlight.getHoverName())))) {
                this.toolHighlightTimer = (int)(40.0 * this.minecraft.options.notificationDisplayTime().get());
            } else if (this.toolHighlightTimer > 0) {
                this.toolHighlightTimer--;
            }

            this.lastToolHighlight = itemstack;
        }

        this.chat.tick();
    }

    private void tickAutosaveIndicator() {
        MinecraftServer minecraftserver = this.minecraft.getSingleplayerServer();
        boolean flag = minecraftserver != null && minecraftserver.isCurrentlySaving();
        this.lastAutosaveIndicatorValue = this.autosaveIndicatorValue;
        this.autosaveIndicatorValue = Mth.lerp(0.2F, this.autosaveIndicatorValue, flag ? 1.0F : 0.0F);
    }

    /**
     * Sets the currently playing record display name and updates the overlay message.
     *
     * @param displayName the display name of the currently playing record.
     */
    public void setNowPlaying(Component displayName) {
        Component component = Component.translatable("record.nowPlaying", displayName);
        this.setOverlayMessage(component, true);
        this.minecraft.getNarrator().saySystemNow(component);
    }

    /**
     * Sets the overlay message to be displayed on the screen.
     *
     * @param component    the {@link Component} representing the overlay message.
     * @param animateColor a boolean indicating whether to animate the color of the
     *                     overlay message.
     */
    public void setOverlayMessage(Component component, boolean animateColor) {
        this.setChatDisabledByPlayerShown(false);
        this.overlayMessageString = component;
        this.overlayMessageTime = 60;
        this.animateOverlayMessageColor = animateColor;
    }

    /**
     * {@return {@code true} if the chat is disabled, {@code false} if chat is enabled}
     */
    public void setChatDisabledByPlayerShown(boolean chatDisabledByPlayerShown) {
        this.chatDisabledByPlayerShown = chatDisabledByPlayerShown;
    }

    public boolean isShowingChatDisabledByPlayer() {
        return this.chatDisabledByPlayerShown && this.overlayMessageTime > 0;
    }

    /**
     * Sets the fade-in, stay, and fade-out times for the title display.
     *
     * @param titleFadeInTime  the fade-in time for the title message in ticks.
     * @param titleStayTime    the stay time for the title message in ticks.
     * @param titleFadeOutTime the fade-out time for the title message in ticks.
     */
    public void setTimes(int titleFadeInTime, int titleStayTime, int titleFadeOutTime) {
        if (titleFadeInTime >= 0) {
            this.titleFadeInTime = titleFadeInTime;
        }

        if (titleStayTime >= 0) {
            this.titleStayTime = titleStayTime;
        }

        if (titleFadeOutTime >= 0) {
            this.titleFadeOutTime = titleFadeOutTime;
        }

        if (this.titleTime > 0) {
            this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
        }
    }

    /**
     * Sets the subtitle to be displayed in the title screen.
     *
     * @param subtitle the subtitle {@link Component} to be displayed.
     */
    public void setSubtitle(Component subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Sets the title to be displayed in the title screen.
     *
     * @param title the title {@link Component} to be displayed.
     */
    public void setTitle(Component title) {
        this.title = title;
        this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
    }

    public void clearTitles() {
        this.title = null;
        this.subtitle = null;
        this.titleTime = 0;
    }

    public ChatComponent getChat() {
        return this.chat;
    }

    public int getGuiTicks() {
        return this.tickCount;
    }

    public Font getFont() {
        return this.minecraft.font;
    }

    public SpectatorGui getSpectatorGui() {
        return this.spectatorGui;
    }

    public PlayerTabOverlay getTabList() {
        return this.tabList;
    }

    public void onDisconnected() {
        this.tabList.reset();
        this.bossOverlay.reset();
        this.minecraft.getToastManager().clear();
        this.debugOverlay.reset();
        this.chat.clearMessages(true);
        this.clearTitles();
        this.resetTitleTimes();
    }

    public BossHealthOverlay getBossOverlay() {
        return this.bossOverlay;
    }

    public DebugScreenOverlay getDebugOverlay() {
        return this.debugOverlay;
    }

    public void clearCache() {
        this.debugOverlay.clearChunkCache();
    }

    public void renderSavingIndicator(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (this.minecraft.options.showAutosaveIndicator().get() && (this.autosaveIndicatorValue > 0.0F || this.lastAutosaveIndicatorValue > 0.0F)) {
            int i = Mth.floor(
                255.0F * Mth.clamp(Mth.lerp(deltaTracker.getRealtimeDeltaTicks(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0F, 1.0F)
            );
            if (i > 0) {
                Font font = this.getFont();
                int j = font.width(SAVING_TEXT);
                int k = ARGB.color(i, -1);
                int l = guiGraphics.guiWidth() - j - 5;
                int i1 = guiGraphics.guiHeight() - 9 - 5;
                guiGraphics.nextStratum();
                guiGraphics.drawStringWithBackdrop(font, SAVING_TEXT, l, i1, j, k);
            }
        }
    }

    private boolean willPrioritizeExperienceInfo() {
        return this.minecraft.player.experienceDisplayStartTick + 100 > this.minecraft.player.tickCount;
    }

    private boolean willPrioritizeJumpInfo() {
        return this.minecraft.player.getJumpRidingScale() > 0.0F
            || Optionull.mapOrDefault(this.minecraft.player.jumpableVehicle(), PlayerRideableJumping::getJumpCooldown, 0) > 0;
    }

    private Gui.ContextualInfo nextContextualInfoState() {
        boolean flag = this.minecraft.player.connection.getWaypointManager().hasWaypoints();
        boolean flag1 = this.minecraft.player.jumpableVehicle() != null;
        boolean flag2 = this.minecraft.gameMode.hasExperience();
        if (flag) {
            if (flag1 && this.willPrioritizeJumpInfo()) {
                return Gui.ContextualInfo.JUMPABLE_VEHICLE;
            } else {
                return flag2 && this.willPrioritizeExperienceInfo() ? Gui.ContextualInfo.EXPERIENCE : Gui.ContextualInfo.LOCATOR;
            }
        } else if (flag1) {
            return Gui.ContextualInfo.JUMPABLE_VEHICLE;
        } else {
            return flag2 ? Gui.ContextualInfo.EXPERIENCE : Gui.ContextualInfo.EMPTY;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum ContextualInfo {
        EMPTY,
        EXPERIENCE,
        LOCATOR,
        JUMPABLE_VEHICLE;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum HeartType implements net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
        CONTAINER(
            ResourceLocation.withDefaultNamespace("hud/heart/container"),
            ResourceLocation.withDefaultNamespace("hud/heart/container_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/container"),
            ResourceLocation.withDefaultNamespace("hud/heart/container_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore"),
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore"),
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore_blinking")
        ),
        NORMAL(
            ResourceLocation.withDefaultNamespace("hud/heart/full"),
            ResourceLocation.withDefaultNamespace("hud/heart/full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/half"),
            ResourceLocation.withDefaultNamespace("hud/heart/half_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/hardcore_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/hardcore_half_blinking")
        ),
        POISIONED(
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_half_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_hardcore_half_blinking")
        ),
        WITHERED(
            ResourceLocation.withDefaultNamespace("hud/heart/withered_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_half_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/withered_hardcore_half_blinking")
        ),
        ABSORBING(
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_half_blinking")
        ),
        FROZEN(
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_half_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_full"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_full_blinking"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_half"),
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_hardcore_half_blinking")
        );

        private final ResourceLocation full;
        private final ResourceLocation fullBlinking;
        private final ResourceLocation half;
        private final ResourceLocation halfBlinking;
        private final ResourceLocation hardcoreFull;
        private final ResourceLocation hardcoreFullBlinking;
        private final ResourceLocation hardcoreHalf;
        private final ResourceLocation hardcoreHalfBlinking;

        private HeartType(
            ResourceLocation full,
            ResourceLocation fullBlinking,
            ResourceLocation half,
            ResourceLocation halfBlinking,
            ResourceLocation hardcoreFull,
            ResourceLocation hardcoreBlinking,
            ResourceLocation hardcoreHalf,
            ResourceLocation hardcoreHalfBlinking
        ) {
            this.full = full;
            this.fullBlinking = fullBlinking;
            this.half = half;
            this.halfBlinking = halfBlinking;
            this.hardcoreFull = hardcoreFull;
            this.hardcoreFullBlinking = hardcoreBlinking;
            this.hardcoreHalf = hardcoreHalf;
            this.hardcoreHalfBlinking = hardcoreHalfBlinking;
        }

        public ResourceLocation getSprite(boolean hardcore, boolean halfHeart, boolean blinking) {
            if (!hardcore) {
                if (halfHeart) {
                    return blinking ? this.halfBlinking : this.half;
                } else {
                    return blinking ? this.fullBlinking : this.full;
                }
            } else if (halfHeart) {
                return blinking ? this.hardcoreHalfBlinking : this.hardcoreHalf;
            } else {
                return blinking ? this.hardcoreFullBlinking : this.hardcoreFull;
            }
        }

        /**
         * Returns the {@link HeartType} based on the player's status effects.
         * <p>
         * @return the {@link HeartType} based on the player's status effects.
         *
         * @param player the player for which to determine the HeartType.
         */
        static Gui.HeartType forPlayer(Player player) {
            Gui.HeartType gui$hearttype;
            if (player.hasEffect(MobEffects.POISON)) {
                gui$hearttype = POISIONED;
            } else if (player.hasEffect(MobEffects.WITHER)) {
                gui$hearttype = WITHERED;
            } else if (player.isFullyFrozen()) {
                gui$hearttype = FROZEN;
            } else {
                gui$hearttype = NORMAL;
            }

            gui$hearttype = net.neoforged.neoforge.client.ClientHooks.firePlayerHeartTypeEvent(player, gui$hearttype);

            return gui$hearttype;
        }

        public static net.neoforged.fml.common.asm.enumextension.ExtensionInfo getExtensionInfo() {
            return net.neoforged.fml.common.asm.enumextension.ExtensionInfo.nonExtended(Gui.HeartType.class);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface RenderFunction {
        void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public void initModdedOverlays() {
        this.layerManager.initModdedLayers();
    }

    public int getLayerCount() {
        return this.layerManager.getLayerCount();
    }
}
