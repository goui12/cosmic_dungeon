package net.minecraft.client.gui.screens;

import com.mojang.text2speech.Narrator;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOnboardingScreen extends Screen {
    private static final Component TITLE = Component.translatable("accessibility.onboarding.screen.title");
    private static final Component ONBOARDING_NARRATOR_MESSAGE = Component.translatable("accessibility.onboarding.screen.narrator");
    private static final int PADDING = 4;
    private static final int TITLE_PADDING = 16;
    private static final float FADE_OUT_TIME = 1000.0F;
    private final LogoRenderer logoRenderer;
    private final Options options;
    private final boolean narratorAvailable;
    private boolean hasNarrated;
    private float timer;
    private final Runnable onClose;
    @Nullable
    private FocusableTextWidget textWidget;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, this.initTitleYPos(), 33);
    private float fadeInStart;
    private boolean fadingIn = true;
    private float fadeOutStart;

    public AccessibilityOnboardingScreen(Options options, Runnable onClose) {
        super(TITLE);
        this.options = options;
        this.onClose = onClose;
        this.logoRenderer = new LogoRenderer(true);
        this.narratorAvailable = Minecraft.getInstance().getNarrator().isActive();
    }

    @Override
    public void init() {
        LinearLayout linearlayout = this.layout.addToContents(LinearLayout.vertical());
        linearlayout.defaultCellSetting().alignHorizontallyCenter().padding(4);
        this.textWidget = linearlayout.addChild(new FocusableTextWidget(this.width, this.title, this.font), p_329717_ -> p_329717_.padding(8));
        if (this.options.narrator().createButton(this.options) instanceof CycleButton cyclebutton) {
            this.narratorButton = cyclebutton;
            this.narratorButton.active = this.narratorAvailable;
            linearlayout.addChild(this.narratorButton);
        }

        linearlayout.addChild(
            CommonButtons.accessibility(150, p_344155_ -> this.closeAndSetScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), false)
        );
        linearlayout.addChild(
            CommonButtons.language(
                150, p_344154_ -> this.closeAndSetScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), false
            )
        );
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_CONTINUE, p_267841_ -> this.onClose()).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.textWidget != null) {
            this.textWidget.containWithin(this.width);
        }

        this.layout.arrangeElements();
    }

    @Override
    protected void setInitialFocus() {
        if (this.narratorAvailable && this.narratorButton != null) {
            this.setInitialFocus(this.narratorButton);
        } else {
            super.setInitialFocus();
        }
    }

    private int initTitleYPos() {
        return 90;
    }

    @Override
    public void onClose() {
        if (this.fadeOutStart == 0.0F) {
            this.fadeOutStart = (float)Util.getMillis();
        }
    }

    private void closeAndSetScreen(Screen screen) {
        this.close(false, () -> this.minecraft.setScreen(screen));
    }

    private void close(boolean markAsFinished, Runnable onClose) {
        if (markAsFinished) {
            this.options.onboardingAccessibilityFinished();
        }

        Narrator.getNarrator().clear();
        onClose.run();
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
        this.handleInitialNarrationDelay();
        if (this.fadeInStart == 0.0F && this.fadingIn) {
            this.fadeInStart = (float)Util.getMillis();
        }

        if (this.fadeInStart > 0.0F) {
            float f = ((float)Util.getMillis() - this.fadeInStart) / 2000.0F;
            float f1 = 1.0F;
            if (f >= 1.0F) {
                this.fadingIn = false;
                this.fadeInStart = 0.0F;
            } else {
                f = Mth.clamp(f, 0.0F, 1.0F);
                f1 = Mth.clampedMap(f, 0.5F, 1.0F, 0.0F, 1.0F);
            }

            this.fadeWidgets(f1);
        }

        if (this.fadeOutStart > 0.0F) {
            float f2 = 1.0F - ((float)Util.getMillis() - this.fadeOutStart) / 1000.0F;
            float f3 = 0.0F;
            if (f2 <= 0.0F) {
                this.fadeOutStart = 0.0F;
                this.close(true, this.onClose);
            } else {
                f2 = Mth.clamp(f2, 0.0F, 1.0F);
                f3 = Mth.clampedMap(f2, 0.5F, 1.0F, 0.0F, 1.0F);
            }

            this.fadeWidgets(f3);
        }

        this.logoRenderer.renderLogo(guiGraphics, this.width, 1.0F);
    }

    @Override
    protected boolean panoramaShouldSpin() {
        return false;
    }

    private void handleInitialNarrationDelay() {
        if (!this.hasNarrated && this.narratorAvailable) {
            if (this.timer < 40.0F) {
                this.timer++;
            } else if (this.minecraft.isWindowActive()) {
                Narrator.getNarrator().say(ONBOARDING_NARRATOR_MESSAGE.getString(), true, this.minecraft.options.getFinalSoundSourceVolume(SoundSource.VOICE));
                this.hasNarrated = true;
            }
        }
    }
}
