package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeatureCountTracker;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class KeyboardHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DEBUG_CRASH_TIME = 10000;
    private final Minecraft minecraft;
    private final ClipboardManager clipboardManager = new ClipboardManager();
    private long debugCrashKeyTime = -1L;
    private long debugCrashKeyReportedTime = -1L;
    private long debugCrashKeyReportedCount = -1L;
    private boolean handledDebugKey;

    public KeyboardHandler(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    private boolean handleChunkDebugKeys(KeyEvent event) {
        switch (event.key()) {
            case 69:
                if (this.minecraft.player == null) {
                    return false;
                }

                boolean flag = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_PATHS);
                this.debugFeedbackFormatted("SectionPath: {0}", flag ? "shown" : "hidden");
                return true;
            case 70:
                boolean flag2 = FogRenderer.toggleFog();
                this.debugFeedbackFormatted("Fog: {0}", flag2 ? "enabled" : "disabled");
                return true;
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 77:
            case 78:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            default:
                return false;
            case 76:
                this.minecraft.smartCull = !this.minecraft.smartCull;
                this.debugFeedbackFormatted("SmartCull: {0}", this.minecraft.smartCull ? "enabled" : "disabled");
                return true;
            case 79:
                if (this.minecraft.player == null) {
                    return false;
                }

                boolean flag1 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_OCTREE);
                this.debugFeedbackFormatted("Frustum culling Octree: {0}", flag1 ? "enabled" : "disabled");
                return true;
            case 85:
                if (event.hasShiftDown()) {
                    this.minecraft.levelRenderer.killFrustum();
                    this.debugFeedbackFormatted("Killed frustum");
                } else {
                    this.minecraft.levelRenderer.captureFrustum();
                    this.debugFeedbackFormatted("Captured frustum");
                }

                return true;
            case 86:
                if (this.minecraft.player == null) {
                    return false;
                }

                boolean flag3 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
                this.debugFeedbackFormatted("SectionVisibility: {0}", flag3 ? "enabled" : "disabled");
                return true;
            case 87:
                this.minecraft.wireframe = !this.minecraft.wireframe;
                this.debugFeedbackFormatted("WireFrame: {0}", this.minecraft.wireframe ? "enabled" : "disabled");
                return true;
        }
    }

    private void showDebugChat(Component message) {
        this.minecraft.gui.getChat().addMessage(message);
        this.minecraft.getNarrator().saySystemQueued(message);
    }

    private static Component decorateDebugComponent(ChatFormatting formatting, Component component) {
        return Component.empty()
            .append(Component.translatable("debug.prefix").withStyle(formatting, ChatFormatting.BOLD))
            .append(CommonComponents.SPACE)
            .append(component);
    }

    private void debugWarningComponent(Component message) {
        this.showDebugChat(decorateDebugComponent(ChatFormatting.RED, message));
    }

    private void debugFeedbackComponent(Component message) {
        this.showDebugChat(decorateDebugComponent(ChatFormatting.YELLOW, message));
    }

    private void debugFeedbackTranslated(String message) {
        this.debugFeedbackComponent(Component.translatable(message));
    }

    private void debugFeedbackFormatted(String message, Object... args) {
        this.debugFeedbackComponent(Component.literal(MessageFormat.format(message, args)));
    }

    private boolean handleDebugKeys(KeyEvent event) {
        if (this.debugCrashKeyTime > 0L && this.debugCrashKeyTime < Util.getMillis() - 100L) {
            return true;
        } else if (SharedConstants.DEBUG_HOTKEYS && this.handleChunkDebugKeys(event)) {
            return true;
        } else {
            if (SharedConstants.DEBUG_FEATURE_COUNT) {
                switch (event.key()) {
                    case 76:
                        FeatureCountTracker.logCounts();
                        return true;
                    case 82:
                        FeatureCountTracker.clearCounts();
                        return true;
                }
            }

            switch (event.key()) {
                case 49:
                    this.minecraft.getDebugOverlay().toggleProfilerChart();
                    return true;
                case 50:
                    this.minecraft.getDebugOverlay().toggleFpsCharts();
                    return true;
                case 51:
                    this.minecraft.getDebugOverlay().toggleNetworkCharts();
                    return true;
                case 65:
                    this.minecraft.levelRenderer.allChanged();
                    this.debugFeedbackTranslated("debug.reload_chunks.message");
                    return true;
                case 66:
                    if (this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
                        boolean flag = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.ENTITY_HITBOXES);
                        this.debugFeedbackTranslated(flag ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
                        return true;
                    }

                    return false;
                case 67:
                    if (this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
                        ClientPacketListener clientpacketlistener = this.minecraft.player.connection;
                        if (clientpacketlistener == null) {
                            return false;
                        }

                        this.debugFeedbackTranslated("debug.copy_location.message");
                        this.setClipboard(
                            String.format(
                                Locale.ROOT,
                                "/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
                                this.minecraft.player.level().dimension().location(),
                                this.minecraft.player.getX(),
                                this.minecraft.player.getY(),
                                this.minecraft.player.getZ(),
                                this.minecraft.player.getYRot(),
                                this.minecraft.player.getXRot()
                            )
                        );
                        return true;
                    }

                    return false;
                case 68:
                    if (this.minecraft.gui != null) {
                        this.minecraft.gui.getChat().clearMessages(false);
                    }

                    return true;
                case 71:
                    if (this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
                        boolean flag1 = this.minecraft.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_BORDERS);
                        this.debugFeedbackTranslated(flag1 ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
                        return true;
                    }

                    return false;
                case 72:
                    this.minecraft.options.advancedItemTooltips = !this.minecraft.options.advancedItemTooltips;
                    this.debugFeedbackTranslated(this.minecraft.options.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
                    this.minecraft.options.save();
                    return true;
                case 73:
                    if (this.minecraft.player != null && !this.minecraft.player.isReducedDebugInfo()) {
                        this.copyRecreateCommand(this.minecraft.player.hasPermissions(2), !event.hasShiftDown());
                    }

                    return true;
                case 76:
                    if (this.minecraft.debugClientMetricsStart(this::debugFeedbackComponent)) {
                        this.debugFeedbackComponent(Component.translatable("debug.profiling.start", 10));
                    }

                    return true;
                case 78:
                    if (this.minecraft.player == null || !this.minecraft.player.hasPermissions(2)) {
                        this.debugFeedbackTranslated("debug.creative_spectator.error");
                    } else if (!this.minecraft.player.isSpectator()) {
                        this.minecraft.player.connection.send(new ServerboundChangeGameModePacket(GameType.SPECTATOR));
                    } else {
                        GameType gametype = MoreObjects.firstNonNull(this.minecraft.gameMode.getPreviousPlayerMode(), GameType.CREATIVE);
                        this.minecraft.player.connection.send(new ServerboundChangeGameModePacket(gametype));
                    }

                    return true;
                case 80:
                    this.minecraft.options.pauseOnLostFocus = !this.minecraft.options.pauseOnLostFocus;
                    this.minecraft.options.save();
                    this.debugFeedbackTranslated(this.minecraft.options.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
                    return true;
                case 81:
                    this.debugFeedbackTranslated("debug.help.message");
                    this.showDebugChat(Component.translatable("debug.reload_chunks.help"));
                    this.showDebugChat(Component.translatable("debug.show_hitboxes.help"));
                    this.showDebugChat(Component.translatable("debug.copy_location.help"));
                    this.showDebugChat(Component.translatable("debug.clear_chat.help"));
                    this.showDebugChat(Component.translatable("debug.chunk_boundaries.help"));
                    this.showDebugChat(Component.translatable("debug.advanced_tooltips.help"));
                    this.showDebugChat(Component.translatable("debug.inspect.help"));
                    this.showDebugChat(Component.translatable("debug.profiling.help"));
                    this.showDebugChat(Component.translatable("debug.creative_spectator.help"));
                    this.showDebugChat(Component.translatable("debug.pause_focus.help"));
                    this.showDebugChat(Component.translatable("debug.help.help"));
                    this.showDebugChat(Component.translatable("debug.dump_dynamic_textures.help"));
                    this.showDebugChat(Component.translatable("debug.reload_resourcepacks.help"));
                    this.showDebugChat(Component.translatable("debug.version.help"));
                    this.showDebugChat(Component.translatable("debug.pause.help"));
                    this.showDebugChat(Component.translatable("debug.gamemodes.help"));
                    this.showDebugChat(Component.translatable("debug.options.help"));
                    return true;
                case 83:
                    Path path = this.minecraft.gameDirectory.toPath().toAbsolutePath();
                    Path path1 = TextureUtil.getDebugTexturePath(path);
                    this.minecraft.getTextureManager().dumpAllSheets(path1);
                    Component component = Component.literal(path.relativize(path1).toString())
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(p_392486_ -> p_392486_.withClickEvent(new ClickEvent.OpenFile(path1)));
                    this.debugFeedbackComponent(Component.translatable("debug.dump_dynamic_textures", component));
                    return true;
                case 84:
                    this.debugFeedbackTranslated("debug.reload_resourcepacks.message");
                    this.minecraft.reloadResourcePacks();
                    return true;
                case 86:
                    this.debugFeedbackTranslated("debug.version.header");
                    VersionCommand.dumpVersion(this::showDebugChat);
                    return true;
                case 293:
                    if (!this.minecraft.canSwitchGameMode() || !this.minecraft.player.hasPermissions(2)) {
                        this.debugFeedbackTranslated("debug.gamemodes.error");
                    } else if (!(this.minecraft.screen instanceof WinScreen)) {
                        this.minecraft.setScreen(new GameModeSwitcherScreen());
                    }

                    return true;
                case 295:
                    if (this.minecraft.screen instanceof DebugOptionsScreen) {
                        this.minecraft.screen.onClose();
                    } else if (this.minecraft.canInterruptScreen()) {
                        if (this.minecraft.screen != null) {
                            this.minecraft.screen.onClose();
                        }

                        this.minecraft.setScreen(new DebugOptionsScreen());
                    }

                    return true;
                default:
                    return false;
            }
        }
    }

    private void copyRecreateCommand(boolean privileged, boolean askServer) {
        HitResult hitresult = this.minecraft.hitResult;
        if (hitresult != null) {
            switch (hitresult.getType()) {
                case BLOCK:
                    BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
                    Level level = this.minecraft.player.level();
                    BlockState blockstate = level.getBlockState(blockpos);
                    if (privileged) {
                        if (askServer) {
                            this.minecraft.player.connection.getDebugQueryHandler().queryBlockEntityTag(blockpos, p_414879_ -> {
                                this.copyCreateBlockCommand(blockstate, blockpos, p_414879_);
                                this.debugFeedbackTranslated("debug.inspect.server.block");
                            });
                        } else {
                            BlockEntity blockentity = level.getBlockEntity(blockpos);
                            CompoundTag compoundtag = blockentity != null ? blockentity.saveWithoutMetadata(level.registryAccess()) : null;
                            this.copyCreateBlockCommand(blockstate, blockpos, compoundtag);
                            this.debugFeedbackTranslated("debug.inspect.client.block");
                        }
                    } else {
                        this.copyCreateBlockCommand(blockstate, blockpos, null);
                        this.debugFeedbackTranslated("debug.inspect.client.block");
                    }
                    break;
                case ENTITY:
                    Entity entity = ((EntityHitResult)hitresult).getEntity();
                    ResourceLocation resourcelocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    if (privileged) {
                        if (askServer) {
                            this.minecraft.player.connection.getDebugQueryHandler().queryEntityTag(entity.getId(), p_414882_ -> {
                                this.copyCreateEntityCommand(resourcelocation, entity.position(), p_414882_);
                                this.debugFeedbackTranslated("debug.inspect.server.entity");
                            });
                        } else {
                            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
                                    entity.problemPath(), LOGGER
                                )) {
                                TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, entity.registryAccess());
                                entity.saveWithoutId(tagvalueoutput);
                                this.copyCreateEntityCommand(resourcelocation, entity.position(), tagvalueoutput.buildResult());
                            }

                            this.debugFeedbackTranslated("debug.inspect.client.entity");
                        }
                    } else {
                        this.copyCreateEntityCommand(resourcelocation, entity.position(), null);
                        this.debugFeedbackTranslated("debug.inspect.client.entity");
                    }
            }
        }
    }

    private void copyCreateBlockCommand(BlockState state, BlockPos pos, @Nullable CompoundTag compound) {
        StringBuilder stringbuilder = new StringBuilder(BlockStateParser.serialize(state));
        if (compound != null) {
            stringbuilder.append(compound);
        }

        String s = String.format(Locale.ROOT, "/setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), stringbuilder);
        this.setClipboard(s);
    }

    private void copyCreateEntityCommand(ResourceLocation entityId, Vec3 pos, @Nullable CompoundTag compound) {
        String s;
        if (compound != null) {
            compound.remove("UUID");
            compound.remove("Pos");
            String s1 = NbtUtils.toPrettyComponent(compound).getString();
            s = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", entityId, pos.x, pos.y, pos.z, s1);
        } else {
            s = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", entityId, pos.x, pos.y, pos.z);
        }

        this.setClipboard(s);
    }

    private void keyPress(long p_window, int action, KeyEvent event) {
        Window window = this.minecraft.getWindow();
        if (p_window == window.handle()) {
            this.minecraft.getFramerateLimitTracker().onInputReceived();
            boolean flag = InputConstants.isKeyDown(window, 292);
            if (this.debugCrashKeyTime > 0L) {
                if (!InputConstants.isKeyDown(window, 67) || !flag) {
                    this.debugCrashKeyTime = -1L;
                }
            } else if (InputConstants.isKeyDown(window, 67) && flag) {
                this.handledDebugKey = true;
                this.debugCrashKeyTime = Util.getMillis();
                this.debugCrashKeyReportedTime = Util.getMillis();
                this.debugCrashKeyReportedCount = 0L;
            }

            Screen screen = this.minecraft.screen;
            if (screen != null) {
                switch (event.key()) {
                    case 258:
                        this.minecraft.setLastInputType(InputType.KEYBOARD_TAB);
                    case 259:
                    case 260:
                    case 261:
                    default:
                        break;
                    case 262:
                    case 263:
                    case 264:
                    case 265:
                        this.minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
                }
            }

            if (action == 1 && (!(this.minecraft.screen instanceof KeyBindsScreen) || ((KeyBindsScreen)screen).lastKeySelection <= Util.getMillis() - 20L)) {
                if (this.minecraft.options.keyFullscreen.matches(event)) {
                    window.toggleFullScreen();
                    boolean flag3 = window.isFullscreen();
                    this.minecraft.options.fullscreen().set(flag3);
                    this.minecraft.options.save();
                    if (this.minecraft.screen instanceof VideoSettingsScreen videosettingsscreen) {
                        videosettingsscreen.updateFullscreenButton(flag3);
                    }

                    return;
                }

                if (this.minecraft.options.keyScreenshot.matches(event)) {
                    if (event.hasControlDown() && SharedConstants.DEBUG_PANORAMA_SCREENSHOT) {
                        this.showDebugChat(this.minecraft.grabPanoramixScreenshot(this.minecraft.gameDirectory));
                    } else {
                        Screenshot.grab(
                            this.minecraft.gameDirectory,
                            this.minecraft.getMainRenderTarget(),
                            p_90917_ -> this.minecraft.execute(() -> this.showDebugChat(p_90917_))
                        );
                    }

                    return;
                }
            }

            if (action != 0) {
                boolean flag1 = screen == null || !(screen.getFocused() instanceof EditBox) || !((EditBox)screen.getFocused()).canConsumeInput();
                if (flag1) {
                    if (event.hasControlDown()
                        && event.key() == 66
                        && this.minecraft.getNarrator().isActive()
                        && this.minecraft.options.narratorHotkey().get()) {
                        boolean flag2 = this.minecraft.options.narrator().get() == NarratorStatus.OFF;
                        this.minecraft.options.narrator().set(NarratorStatus.byId(this.minecraft.options.narrator().get().getId() + 1));
                        this.minecraft.options.save();
                        if (screen != null) {
                            screen.updateNarratorStatus(flag2);
                        }
                    }

                    LocalPlayer localplayer = this.minecraft.player;
                }
            }

            if (screen != null) {
                try {
                    if (action != 1 && action != 2) {
                        if (action == 0 && (
                            net.neoforged.neoforge.client.ClientHooks.onScreenKeyReleasedPre(screen, event) ||
                            screen.keyReleased(event) ||
                            net.neoforged.neoforge.client.ClientHooks.onScreenKeyReleasedPost(screen, event))
                        ) {
                            return;
                        }
                    } else {
                        screen.afterKeyboardAction();
                        if (
                            net.neoforged.neoforge.client.ClientHooks.onScreenKeyPressedPre(screen, event) ||
                            screen.keyPressed(event) ||
                            net.neoforged.neoforge.client.ClientHooks.onScreenKeyPressedPost(screen, event)
                        ) {
                            if (this.minecraft.screen == null) {
                                InputConstants.Key inputconstants$key = InputConstants.getKey(event);
                                KeyMapping.set(inputconstants$key, false);
                            }

                            return;
                        }
                    }
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "keyPressed event handler");
                    screen.fillCrashDetails(crashreport);
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Key");
                    crashreportcategory.setDetail("Key", event.key());
                    crashreportcategory.setDetail("Scancode", event.scancode());
                    crashreportcategory.setDetail("Mods", event.modifiers());
                    throw new ReportedException(crashreport);
                }
            }

            InputConstants.Key inputconstants$key1 = InputConstants.getKey(event);
            boolean flag4 = this.minecraft.screen == null;
            boolean flag5 = flag4
                || this.minecraft.screen instanceof PauseScreen pausescreen && !pausescreen.showsPauseMenu()
                || this.minecraft.screen instanceof GameModeSwitcherScreen;
            if (action == 0) {
                KeyMapping.set(inputconstants$key1, false);
                if (event.key() == 292) {
                    if (this.handledDebugKey) {
                        this.handledDebugKey = false;
                    } else {
                        this.minecraft.debugEntries.toggleF3Visible();
                    }
                }
            } else {
                boolean flag6 = false;
                if (flag5 && event.isEscape()) {
                    this.minecraft.pauseGame(flag);
                    flag6 = flag;
                } else if (flag) {
                    flag6 = this.handleDebugKeys(event);
                } else if (flag5 && event.key() == 290) {
                    this.minecraft.options.hideGui = !this.minecraft.options.hideGui;
                } else if (flag5 && event.key() == 293) {
                    this.minecraft.gameRenderer.togglePostEffect();
                }

                this.handledDebugKey |= flag6;
                if (this.minecraft.getDebugOverlay().showProfilerChart() && !flag) {
                    int i = event.getDigit();
                    if (i != -1) {
                        this.minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(i);
                    }
                }

                if (flag4) {
                    if (flag6) {
                        KeyMapping.set(inputconstants$key1, false);
                    } else {
                        KeyMapping.set(inputconstants$key1, true);
                        KeyMapping.click(inputconstants$key1);
                    }
                }
            }
            net.neoforged.neoforge.client.ClientHooks.onKeyInput(event, action);
        }
    }

    private void charTyped(long window, CharacterEvent event) {
        if (window == this.minecraft.getWindow().handle()) {
            Screen screen = this.minecraft.screen;
            if (screen != null && this.minecraft.getOverlay() == null) {
                try {
                    if (net.neoforged.neoforge.client.ClientHooks.onScreenCharTypedPre(screen, event)) return;
                    if (screen.charTyped(event)) return;
                    net.neoforged.neoforge.client.ClientHooks.onScreenCharTypedPost(screen, event);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "charTyped event handler");
                    screen.fillCrashDetails(crashreport);
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Key");
                    crashreportcategory.setDetail("Codepoint", event.codepoint());
                    crashreportcategory.setDetail("Mods", event.modifiers());
                    throw new ReportedException(crashreport);
                }
            }
        }
    }

    public void setup(Window window) {
        InputConstants.setupKeyboardCallbacks(window, (p_445137_, p_445138_, p_445139_, p_445140_, p_445141_) -> {
            KeyEvent keyevent = new KeyEvent(p_445138_, p_445139_, p_445141_);
            this.minecraft.execute(() -> this.keyPress(p_445137_, p_445140_, keyevent));
        }, (p_445132_, p_445133_, p_445134_) -> {
            CharacterEvent characterevent = new CharacterEvent(p_445133_, p_445134_);
            this.minecraft.execute(() -> this.charTyped(p_445132_, characterevent));
        });
    }

    public String getClipboard() {
        return this.clipboardManager.getClipboard(this.minecraft.getWindow(), (p_90878_, p_90879_) -> {
            if (p_90878_ != 65545) {
                this.minecraft.getWindow().defaultErrorCallback(p_90878_, p_90879_);
            }
        });
    }

    public void setClipboard(String string) {
        if (!string.isEmpty()) {
            this.clipboardManager.setClipboard(this.minecraft.getWindow(), string);
        }
    }

    public void tick() {
        if (this.debugCrashKeyTime > 0L) {
            long i = Util.getMillis();
            long j = 10000L - (i - this.debugCrashKeyTime);
            long k = i - this.debugCrashKeyReportedTime;
            if (j < 0L) {
                if (this.minecraft.hasControlDown()) {
                    Blaze3D.youJustLostTheGame();
                }

                String s = "Manually triggered debug crash";
                CrashReport crashreport = new CrashReport("Manually triggered debug crash", new Throwable("Manually triggered debug crash"));
                CrashReportCategory crashreportcategory = crashreport.addCategory("Manual crash details");
                NativeModuleLister.addCrashSection(crashreportcategory);
                throw new ReportedException(crashreport);
            }

            if (k >= 1000L) {
                if (this.debugCrashKeyReportedCount == 0L) {
                    this.debugFeedbackTranslated("debug.crash.message");
                } else {
                    this.debugWarningComponent(Component.translatable("debug.crash.warning", Mth.ceil((float)j / 1000.0F)));
                }

                this.debugCrashKeyReportedTime = i;
                this.debugCrashKeyReportedCount++;
            }
        }
    }
}
