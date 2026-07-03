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
    private static final int CLASS_GRID_Y = 40;
    private static final int CLASS_GRID_ROW_GAP = 8;
    private static final int TEXT_COLOR = 0xFFEFE6D0;
    private static final int TITLE_COLOR = 0xFFFFD98A;
    private static final int BODY_COLOR = 0xFFD8D0C0;

    private static final String MAIN_SUMMARY = "A dungeon-focused adventure pack with classes, progression, trading, quests, and codex guidance.";

    private HelpView currentView = HelpView.MAIN;
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

        if (this.currentView == HelpView.MAIN) {
            renderMainView(graphics);
        } else if (this.currentView == HelpView.CLASSES) {
            renderClassesView(graphics);
        } else {
            renderTextView(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMainView(GuiGraphics graphics) {
        HelpView[] children = this.currentView.children();
        for (int i = 0; i < children.length; i++) {
            renderTextButton(graphics, children[i].label, mainButtonX(), mainButtonY(i));
        }

        drawWrappedText(graphics, "Cosmic Dungeon", menuLeft + RIGHT_PANEL_X, menuTop + 24, RIGHT_PANEL_W, TITLE_COLOR);
        drawWrappedText(graphics, MAIN_SUMMARY, menuLeft + RIGHT_PANEL_X, menuTop + 44, RIGHT_PANEL_W, BODY_COLOR);
    }

    private void renderClassesView(GuiGraphics graphics) {
        renderBackButton(graphics);
        graphics.drawString(this.font, this.currentView.label, backButtonX() + NAV_BUTTON_SIZE + 8, backButtonY() + 10, TITLE_COLOR, false);

        HelpView[] classes = this.currentView.children();
        for (int i = 0; i < classes.length; i++) {
            int column = i % 2;
            int row = i / 2;
            renderTextButton(graphics, classes[i].label, classButtonX(column), classButtonY(row));
        }
    }

    private void renderTextView(GuiGraphics graphics) {
        renderBackButton(graphics);
        graphics.drawString(this.font, this.currentView.label, backButtonX() + NAV_BUTTON_SIZE + 8, backButtonY() + 10, TITLE_COLOR, false);
        drawWrappedText(graphics, this.currentView.body, menuLeft + LEFT_MARGIN, menuTop + 58, MENU_W - (LEFT_MARGIN * 2), BODY_COLOR);
    }

    private void renderTextButton(GuiGraphics graphics, String label, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXT_BUTTON, x, y, 0, 0, TEXT_BUTTON_W, TEXT_BUTTON_H, TEXT_BUTTON_W, TEXT_BUTTON_H);
        graphics.drawCenteredString(this.font, label, x + TEXT_BUTTON_W / 2, y + (TEXT_BUTTON_H - this.font.lineHeight) / 2, TEXT_COLOR);
    }

    private void renderBackButton(GuiGraphics graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, LEFT_BUTTON, backButtonX(), backButtonY(), 0, 0, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE, NAV_BUTTON_SIZE);
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
            if (this.currentView == HelpView.MAIN) {
                return clickMainButton(mouseX, mouseY) || super.mouseClicked(event, isDoubleClick);
            }
            if (this.currentView == HelpView.CLASSES) {
                return clickBackButton(mouseX, mouseY) || clickClassButton(mouseX, mouseY) || super.mouseClicked(event, isDoubleClick);
            }
            return clickBackButton(mouseX, mouseY) || super.mouseClicked(event, isDoubleClick);
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean clickMainButton(double mouseX, double mouseY) {
        HelpView[] children = HelpView.MAIN.children();
        for (int i = 0; i < children.length; i++) {
            if (isInside(mouseX, mouseY, mainButtonX(), mainButtonY(i), TEXT_BUTTON_W, TEXT_BUTTON_H)) {
                this.currentView = children[i];
                return true;
            }
        }
        return false;
    }

    private boolean clickClassButton(double mouseX, double mouseY) {
        HelpView[] classes = HelpView.CLASSES.children();
        for (int i = 0; i < classes.length; i++) {
            int column = i % 2;
            int row = i / 2;
            if (isInside(mouseX, mouseY, classButtonX(column), classButtonY(row), TEXT_BUTTON_W, TEXT_BUTTON_H)) {
                this.currentView = classes[i];
                return true;
            }
        }
        return false;
    }

    private boolean clickBackButton(double mouseX, double mouseY) {
        if (isInside(mouseX, mouseY, backButtonX(), backButtonY(), NAV_BUTTON_SIZE, NAV_BUTTON_SIZE)) {
            this.currentView = this.currentView.parent();
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
        int stackH = (TEXT_BUTTON_H * HelpView.MAIN.children().length) + (BUTTON_GAP * (HelpView.MAIN.children().length - 1));
        return menuTop + MENU_H - BOTTOM_MARGIN - stackH + (index * (TEXT_BUTTON_H + BUTTON_GAP));
    }

    private int classButtonX(int column) {
        return menuLeft + (column * TEXT_BUTTON_W);
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

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum HelpView {
        MAIN("Main", ""),
        GET_STARTED("Get Started", "Get Started help is a stub for first-session guidance, setup steps, and early progression tips."),
        DUNGEONS("Dungeons", "Dungeons help is a stub for party flow, doors, locks, region rules, objectives, and encounter guidance."),
        CLASSES("Classes", ""),
        CODEX("Codex", "Codex help is a stub for commands, discoveries, progression references, and developer-facing notes."),
        PYROCLAST("Pyroclast", "Pyroclast class help will become a text-only guide for role, gear, strengths, and progression notes."),
        BOGATYR("Bogatyr", "Bogatyr class help will become a text-only guide for role, gear, strengths, and progression notes."),
        DRAGOON("Dragoon", "Dragoon class help will become a text-only guide for role, gear, strengths, and progression notes."),
        METALMANCER("Metalmancer", "Metalmancer class help will become a text-only guide for role, gear, strengths, and progression notes."),
        THEURGIST("Theurgist", "Theurgist class help will become a text-only guide for role, gear, strengths, and progression notes."),
        VENEFEX("Venefex", "Venefex class help will become a text-only guide for role, gear, strengths, and progression notes."),
        DEADEYE("Deadeye", "Deadeye class help will become a text-only guide for role, gear, strengths, and progression notes."),
        JUDICATOR("Judicator", "Judicator class help will become a text-only guide for role, gear, strengths, and progression notes.");

        private static final HelpView[] MAIN_CHILDREN = {GET_STARTED, DUNGEONS, CLASSES, CODEX};
        private static final HelpView[] CLASS_CHILDREN = {PYROCLAST, BOGATYR, DRAGOON, METALMANCER, THEURGIST, VENEFEX, DEADEYE, JUDICATOR};

        private final String label;
        private final String body;

        HelpView(String label, String body) {
            this.label = label;
            this.body = body;
        }

        private HelpView[] children() {
            return switch (this) {
                case MAIN -> MAIN_CHILDREN;
                case CLASSES -> CLASS_CHILDREN;
                default -> new HelpView[0];
            };
        }

        private HelpView parent() {
            return switch (this) {
                case PYROCLAST, BOGATYR, DRAGOON, METALMANCER, THEURGIST, VENEFEX, DEADEYE, JUDICATOR -> CLASSES;
                default -> MAIN;
            };
        }
    }
}
