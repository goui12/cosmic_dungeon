package net.goui.cosmicdungeon.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.HelpMenuKeybindClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class HelpMenuScreen extends Screen {
    private static final ResourceLocation BACKGROUND = texture("background");
    private static final ResourceLocation TEXT_BUTTON = texture("text_button");
    private static final ResourceLocation LEFT_BUTTON = texture("left_button");
    private static final ResourceLocation UP_BUTTON = texture("up_button");
    private static final ResourceLocation DOWN_BUTTON = texture("down_button");

    private static final int MENU_W = 256;
    private static final int MENU_H = 192;
    private static final int TEXT_BUTTON_W = 128;
    private static final int TEXT_BUTTON_H = 32;
    private static final int NAV_BUTTON_SIZE = 28;
    private static final int LEFT_MARGIN = 16;
    private static final int TOP_MARGIN = 16;
    private static final int BOTTOM_MARGIN = 16;
    private static final int BUTTON_GAP = 16;
    private static final int RIGHT_PANEL_X = 154;
    private static final int RIGHT_PANEL_W = 86;
    private static final int CLASS_GRID_Y = 52;
    private static final int CLASS_GRID_ROW_GAP = 8;
    private static final int TEXT_COLOR = 0xFFEFE6D0;
    private static final int TITLE_COLOR = 0xFFFFD98A;
    private static final int BODY_COLOR = 0xFFD8D0C0;
    private static final int DISABLED_TEXT_COLOR = 0xFF8A8479;
    private static final int DISABLED_OVERLAY_COLOR = 0xAA1A1A1A;

    private HelpMenuContent.Page currentView = HelpMenuContent.MAIN;
    private int classPageStart;
    private int menuLeft;
    private int menuTop;

    public HelpMenuScreen() {
        super(Component.translatable("screen.cosmicdungeon.help_menu"));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/menu/" + name + ".png");
    }

    @Override
    protected void init() {
        super.init();
        this.menuLeft = (this.width - MENU_W) / 2;
        this.menuTop = (this.height - MENU_H) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, menuLeft, menuTop, 0, 0, MENU_W, MENU_H, MENU_W, MENU_H);

        if (this.currentView == HelpMenuContent.MAIN) {
            renderMainView(graphics);
        } else if (this.currentView == HelpMenuContent.CLASSES) {
            renderClassesView(graphics);
        } else {
            renderTextView(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMainView(GuiGraphics graphics) {
        HelpMenuContent.Page[] children = HelpMenuContent.MAIN_CHILDREN;
        for (int i = 0; i < children.length; i++) {
            renderTextButton(graphics, children[i].title(), mainButtonX(), mainButtonY(i));
        }

        drawWrappedText(graphics, this.currentView.title(), menuLeft + RIGHT_PANEL_X, menuTop + 24, RIGHT_PANEL_W, TITLE_COLOR);
        drawWrappedText(graphics, this.currentView.body(), menuLeft + RIGHT_PANEL_X, menuTop + 44, RIGHT_PANEL_W, BODY_COLOR);
    }

    private void renderClassesView(GuiGraphics graphics) {
        renderBackButton(graphics);
        renderCycleButton(graphics, true);
        renderCycleButton(graphics, false);
        graphics.drawString(this.font, this.currentView.title(), backButtonX() + NAV_BUTTON_SIZE + 8, backButtonY() + 10, TITLE_COLOR, false);

        HelpMenuContent.ClassEntry[] classes = HelpMenuContent.CLASS_ENTRIES;
        for (int i = 0; i < HelpMenuContent.CLASS_PAGE_SIZE; i++) {
            HelpMenuContent.ClassEntry classEntry = classes[(classPageStart + i) % classes.length];
            renderTextButton(graphics, classEntry.page().title(), classButtonX(), classButtonY(i), classEntry.enabled());
        }
    }

    private void renderTextView(GuiGraphics graphics) {
        renderBackButton(graphics);
        graphics.drawString(this.font, this.currentView.title(), backButtonX() + NAV_BUTTON_SIZE + 8, backButtonY() + 10, TITLE_COLOR, false);
        drawWrappedText(graphics, this.currentView.body(), menuLeft + LEFT_MARGIN, menuTop + 58, MENU_W - (LEFT_MARGIN * 2), BODY_COLOR);
    }

    private void renderTextButton(GuiGraphics graphics, String label, int x, int y) {
        renderTextButton(graphics, label, x, y, true);
    }

    private void renderTextButton(GuiGraphics graphics, String label, int x, int y, boolean enabled) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXT_BUTTON, x, y, 0, 0, TEXT_BUTTON_W, TEXT_BUTTON_H, TEXT_BUTTON_W, TEXT_BUTTON_H);
        if (!enabled) {
            graphics.fill(x, y, x + TEXT_BUTTON_W, y + TEXT_BUTTON_H, DISABLED_OVERLAY_COLOR);
        }
        graphics.drawCenteredString(this.font, label, x + TEXT_BUTTON_W / 2, y + (TEXT_BUTTON_H - this.font.lineHeight) / 2, enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR);
    }

    private void renderBackButton(GuiGraphics graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, LEFT_BUTTON, backButtonX(), backButtonY(), 0, 0, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE);
    }

    private void renderCycleButton(GuiGraphics graphics, boolean up) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, up ? UP_BUTTON : DOWN_BUTTON, cycleButtonX(), up ? cycleUpButtonY() : cycleDownButtonY(), 0, 0, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE || HelpMenuKeybindClient.matchesHelpMenuKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            if (this.currentView == HelpMenuContent.MAIN) {
                return clickMainButton(mouseX, mouseY) || super.mouseClicked(event, isDoubleClick);
            }
            if (this.currentView == HelpMenuContent.CLASSES) {
                return clickBackButton(mouseX, mouseY) || clickCycleButton(mouseX, mouseY) || clickClassButton(mouseX, mouseY) || super.mouseClicked(event, isDoubleClick);
            }
            return clickBackButton(mouseX, mouseY) || super.mouseClicked(event, isDoubleClick);
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean clickMainButton(double mouseX, double mouseY) {
        HelpMenuContent.Page[] children = HelpMenuContent.MAIN_CHILDREN;
        for (int i = 0; i < children.length; i++) {
            if (isInside(mouseX, mouseY, mainButtonX(), mainButtonY(i), TEXT_BUTTON_W, TEXT_BUTTON_H)) {
                this.currentView = children[i];
                return true;
            }
        }
        return false;
    }

    private boolean clickClassButton(double mouseX, double mouseY) {
        HelpMenuContent.ClassEntry[] classes = HelpMenuContent.CLASS_ENTRIES;
        for (int i = 0; i < HelpMenuContent.CLASS_PAGE_SIZE; i++) {
            if (isInside(mouseX, mouseY, classButtonX(), classButtonY(i), TEXT_BUTTON_W, TEXT_BUTTON_H)) {
                HelpMenuContent.ClassEntry classEntry = classes[(classPageStart + i) % classes.length];
                if (classEntry.enabled()) {
                    this.currentView = classEntry.page();
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickCycleButton(double mouseX, double mouseY) {
        if (isInside(mouseX, mouseY, cycleButtonX(), cycleUpButtonY(), NAV_BUTTON_SIZE, NAV_BUTTON_SIZE)) {
            cycleClasses(-HelpMenuContent.CLASS_PAGE_SIZE);
            return true;
        }
        if (isInside(mouseX, mouseY, cycleButtonX(), cycleDownButtonY(), NAV_BUTTON_SIZE, NAV_BUTTON_SIZE)) {
            cycleClasses(HelpMenuContent.CLASS_PAGE_SIZE);
            return true;
        }
        return false;
    }

    private void cycleClasses(int amount) {
        classPageStart = Math.floorMod(classPageStart + amount, HelpMenuContent.CLASS_ENTRIES.length);
    }

    private boolean clickBackButton(double mouseX, double mouseY) {
        if (isInside(mouseX, mouseY, backButtonX(), backButtonY(), NAV_BUTTON_SIZE, NAV_BUTTON_SIZE)) {
            this.currentView = HelpMenuContent.isClassPage(this.currentView) ? HelpMenuContent.CLASSES : HelpMenuContent.MAIN;
            return true;
        }
        return false;
    }

    private void drawWrappedText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        for (String line : wrapText(text, maxWidth)) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += this.font.lineHeight + 2;
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (this.font.width(candidate) <= maxWidth || currentLine.isEmpty()) {
                currentLine.setLength(0);
                currentLine.append(candidate);
            } else {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private int mainButtonX() {
        return menuLeft + LEFT_MARGIN;
    }

    private int mainButtonY(int index) {
        int stackH = (TEXT_BUTTON_H * HelpMenuContent.MAIN_CHILDREN.length) + (BUTTON_GAP * (HelpMenuContent.MAIN_CHILDREN.length - 1));
        return menuTop + MENU_H - BOTTOM_MARGIN - stackH + (index * (TEXT_BUTTON_H + BUTTON_GAP));
    }

    private int classButtonX() {
        return menuLeft + (MENU_W - TEXT_BUTTON_W) / 2;
    }

    private int classButtonY(int row) {
        return menuTop + CLASS_GRID_Y + (row * (TEXT_BUTTON_H + CLASS_GRID_ROW_GAP));
    }

    private int backButtonX() {
        return menuLeft + LEFT_MARGIN;
    }

    private int backButtonY() {
        return menuTop + TOP_MARGIN;
    }

    private int cycleButtonX() {
        return menuLeft + MENU_W - LEFT_MARGIN - NAV_BUTTON_SIZE;
    }

    private int cycleUpButtonY() {
        return menuTop + TOP_MARGIN;
    }

    private int cycleDownButtonY() {
        return menuTop + MENU_H - BOTTOM_MARGIN - NAV_BUTTON_SIZE;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
