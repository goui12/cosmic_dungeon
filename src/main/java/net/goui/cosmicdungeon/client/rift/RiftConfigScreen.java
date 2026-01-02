package net.goui.cosmicdungeon.client.rift;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.RiftPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server-authoritative config screen:
 *  - Always requests data from server on open
 *  - Never uses client cache
 *  - Save waits for server result
 *
 * Destination is an inline "spinner" dropdown (NOT a new screen).
 */
public final class RiftConfigScreen extends Screen {
    private static BlockPos pendingClickedTile;

    public static void openForClickedTile(BlockPos clickedTilePos) {
        pendingClickedTile = clickedTilePos;
        Minecraft.getInstance().setScreen(new RiftConfigScreen());
    }

    private static final ResourceLocation GUI_TEX =
            ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/rift_gui.png");

    private static final int GUI_W = 230, GUI_H = 230;

    private static final int NAME_X = 22, NAME_Y = 46, NAME_W = 186, NAME_H = 36;
    private static final int SAVE_X = 38, SAVE_Y = 176, SAVE_W = 78, SAVE_H = 32;
    private static final int CANCEL_X = 124, CANCEL_Y = 176, CANCEL_W = 78, CANCEL_H = 32;

    // Destination box (GUI-relative coords you gave)
    private static final int DEST_BOX_X1 = 20;
    private static final int DEST_BOX_Y1 = 105;
    private static final int DEST_BOX_X2 = 210;
    private static final int DEST_BOX_Y2 = 138;
    private static final int DEST_BOX_W = (DEST_BOX_X2 - DEST_BOX_X1);
    private static final int DEST_BOX_H = (DEST_BOX_Y2 - DEST_BOX_Y1);

    private static final int PAD = 4;
    private static final int DROPDOWN_BTN_W = 18;

    // Dropdown sizing
    private static final int ROW_H = 12;
    private static final int MAX_ROWS = 6;

    private int guiLeft, guiTop;

    private EditBox nameField;
    private EditBox destinationField;
    private Button destinationDropdownBtn;
    private Button saveBtn;
    private Button cancelBtn;

    private boolean loading = true;
    private boolean saving = false;

    private BlockPos clickedTile;
    private BlockPos anchorPos;

    private String currentDestination = "";

    // Server-provided destination list
    private final List<String> allDestinations = new ArrayList<>();
    private final List<String> filteredDestinations = new ArrayList<>();

    private boolean destinationDropdownOpen = false;
    private int destinationDropdownScroll = 0;

    public RiftConfigScreen() {
        super(Component.literal("Rift Configuration"));
    }

    /**
     * Button that refuses hover while the dropdown is open.
     * This prevents:
     *  - highlight animation
     *  - "not allowed" cursor when inactive
     *  - any mouse-over affordances
     */
    private final class NoHoverWhenDropdownButton extends Button {
        private NoHoverWhenDropdownButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (destinationDropdownOpen) return false;
            return super.isMouseOver(mouseX, mouseY);
        }
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - GUI_W) / 2;
        this.guiTop = (this.height - GUI_H) / 2;

        this.clickedTile = pendingClickedTile;
        this.anchorPos = pendingClickedTile; // placeholder until server returns

        // Name field
        int fieldH = 20;
        int nameFieldY = guiTop + NAME_Y + (NAME_H - fieldH) / 2;

        this.nameField = new EditBox(
                this.font,
                guiLeft + NAME_X + 4,
                nameFieldY,
                NAME_W - 8,
                fieldH,
                Component.literal("Rift Name")
        );
        this.nameField.setMaxLength(48);
        this.nameField.setValue("");
        this.nameField.setEditable(false);
        addRenderableWidget(this.nameField);

        // Destination field (inline "spinner")
        int destFieldH = 20;
        int destBoxX = guiLeft + DEST_BOX_X1;
        int destBoxY = guiTop + DEST_BOX_Y1;
        int destFieldY = destBoxY + (DEST_BOX_H - destFieldH) / 2;

        int destFieldX = destBoxX + PAD;
        int destFieldW = DEST_BOX_W - (PAD * 2) - DROPDOWN_BTN_W;

        this.destinationField = new EditBox(
                this.font,
                destFieldX,
                destFieldY,
                destFieldW,
                destFieldH,
                Component.literal("Destination")
        );
        this.destinationField.setMaxLength(64);
        this.destinationField.setValue("");
        this.destinationField.setEditable(false);

        // Typing filters list and opens dropdown
        this.destinationField.setResponder(text -> {
            if (loading || saving) return;
            this.currentDestination = text == null ? "" : text;
            rebuildDestinationFilter(this.currentDestination);
            this.destinationDropdownOpen = true;
            this.destinationDropdownScroll = 0;
        });
        addRenderableWidget(this.destinationField);

        // Dropdown button
        int btnX = destFieldX + destFieldW;
        int btnY = destFieldY;

        this.destinationDropdownBtn = Button.builder(
                        Component.literal("▼"),
                        b -> {
                            if (loading || saving) return;
                            this.destinationDropdownOpen = !this.destinationDropdownOpen;
                            rebuildDestinationFilter(this.destinationField.getValue());
                            this.destinationDropdownScroll = 0;
                        }
                )
                .bounds(btnX, btnY, DROPDOWN_BTN_W, destFieldH)
                .build();
        addRenderableWidget(this.destinationDropdownBtn);

        // Save / Cancel (no-hover while dropdown open)
        this.saveBtn = addRenderableWidget(new NoHoverWhenDropdownButton(
                guiLeft + SAVE_X, guiTop + SAVE_Y, SAVE_W, SAVE_H,
                Component.literal("Save"),
                b -> onSave()
        ));
        this.saveBtn.active = false;

        this.cancelBtn = addRenderableWidget(new NoHoverWhenDropdownButton(
                guiLeft + CANCEL_X, guiTop + CANCEL_Y, CANCEL_W, CANCEL_H,
                Component.literal("Cancel"),
                b -> onClose()
        ));

        requestConfigFromServer();
    }

    private void requestConfigFromServer() {
        this.loading = true;
        this.saving = false;

        this.nameField.setEditable(false);
        this.destinationField.setEditable(false);
        this.saveBtn.active = false;

        this.destinationDropdownOpen = false;
        this.destinationDropdownScroll = 0;
        this.filteredDestinations.clear();

        ModNetwork.sendToServer(new RiftPayloads.C2S_RequestRiftConfig(this.clickedTile));
    }

    private void onSave() {
        if (loading || saving) return;

        this.saving = true;
        this.saveBtn.active = false;
        this.destinationDropdownOpen = false;

        String name = this.nameField.getValue() == null ? "" : this.nameField.getValue();
        String dest = this.destinationField.getValue() == null ? "" : this.destinationField.getValue();

        ModNetwork.sendToServer(new RiftPayloads.C2S_SaveRiftConfig(this.anchorPos, name, dest));
    }

    private void rebuildDestinationFilter(String typed) {
        String q = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);

        this.filteredDestinations.clear();
        for (String s : this.allDestinations) {
            if (s == null) continue;
            String sl = s.toLowerCase(Locale.ROOT);
            if (q.isEmpty() || sl.contains(q)) {
                this.filteredDestinations.add(s);
            }
        }

        int maxScroll = Math.max(0, this.filteredDestinations.size() - MAX_ROWS);
        if (this.destinationDropdownScroll > maxScroll) this.destinationDropdownScroll = maxScroll;
        if (this.destinationDropdownScroll < 0) this.destinationDropdownScroll = 0;
    }

    private boolean isMouseOverDestinationField(double mouseX, double mouseY) {
        return mouseX >= this.destinationField.getX()
                && mouseX < (this.destinationField.getX() + this.destinationField.getWidth())
                && mouseY >= this.destinationField.getY()
                && mouseY < (this.destinationField.getY() + this.destinationField.getHeight());
    }

    private int dropdownListX() { return this.guiLeft + DEST_BOX_X1; }
    private int dropdownListY() { return this.guiTop + DEST_BOX_Y2 + 2; }
    private int dropdownListW() { return DEST_BOX_W; }

    private boolean isMouseOverDropdown(double mouseX, double mouseY) {
        if (!this.destinationDropdownOpen) return false;

        int listX = dropdownListX();
        int listY = dropdownListY();
        int listW = dropdownListW();

        int total = this.filteredDestinations.size();
        int visible = Math.min(MAX_ROWS, total);
        int listH = (visible * ROW_H) + 4;

        return mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float dt) {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                GUI_TEX, guiLeft, guiTop, 0, 0, GUI_W, GUI_H, GUI_W, GUI_H);

        // Server-authoritative: lock while loading/saving
        this.nameField.setEditable(!loading && !saving);
        this.destinationField.setEditable(!loading && !saving);

        // Keep buttons logically usable, but prevent hover by isMouseOver override.
        // Also: while dropdown is open, don’t allow Save click-through by disabling it.
        this.saveBtn.active = !loading && !saving && !this.destinationDropdownOpen;
        this.cancelBtn.active = !this.destinationDropdownOpen; // still clickable when not dropdown

        // Footer status
        if (saving) {
            g.drawString(this.font, "Saving…", guiLeft + 12, guiTop + GUI_H - 14, 0xFF444444, false);
        } else if (loading) {
            g.drawString(this.font, "Contacting server…", guiLeft + 12, guiTop + GUI_H - 14, 0xFF444444, false);
        }

        super.render(g, mouseX, mouseY, dt);

        // Dropdown AFTER widgets so it overlays
        if (this.destinationDropdownOpen) {
            int listX = dropdownListX();
            int listY = dropdownListY();
            int listW = dropdownListW();

            int total = this.filteredDestinations.size();
            int visible = Math.min(MAX_ROWS, total);

            g.fill(listX, listY, listX + listW, listY + (visible * ROW_H) + 4, 0xAA000000);

            for (int i = 0; i < visible; i++) {
                int idx = i + this.destinationDropdownScroll;
                if (idx < 0 || idx >= total) continue;

                int ry1 = listY + 2 + (i * ROW_H);
                int ry2 = ry1 + ROW_H;

                boolean hovered = (mouseX >= listX && mouseX < listX + listW && mouseY >= ry1 && mouseY < ry2);
                if (hovered) {
                    g.fill(listX + 1, ry1, listX + listW - 1, ry2, 0xAA333333);
                }

                g.drawString(this.font, this.filteredDestinations.get(idx), listX + 4, ry1 + 2, 0xFFFFFFFF, false);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float dt) {
        this.renderTransparentBackground(g);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0) {
            if (this.destinationDropdownOpen) {
                // Click inside dropdown selects.
                if (isMouseOverDropdown(mouseX, mouseY)) {
                    int listY = dropdownListY();

                    int total = this.filteredDestinations.size();
                    int visible = Math.min(MAX_ROWS, total);

                    int relY = (int) mouseY - (listY + 2);
                    int row = relY / ROW_H;
                    int idx = row + this.destinationDropdownScroll;

                    if (row >= 0 && row < visible && idx >= 0 && idx < total) {
                        String picked = this.filteredDestinations.get(idx);
                        this.destinationField.setValue(picked);
                        this.currentDestination = picked;
                        this.destinationDropdownOpen = false;
                        return true;
                    }

                    return true; // eat clicks in panel
                }

                // Click outside dropdown: close it unless click is on destination box/button.
                if (!isMouseOverDestinationField(mouseX, mouseY) && !this.destinationDropdownBtn.isMouseOver(mouseX, mouseY)) {
                    this.destinationDropdownOpen = false;
                    return true; // eat so Save/Cancel don't get clicked through
                }
                // else: allow click to focus field / hit dropdown button
            } else {
                // click on the destination field opens dropdown
                if (!loading && !saving && isMouseOverDestinationField(mouseX, mouseY)) {
                    rebuildDestinationFilter(this.destinationField.getValue());
                    this.destinationDropdownOpen = true;
                    this.destinationDropdownScroll = 0;
                    // still allow EditBox focus behavior
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.destinationDropdownOpen && isMouseOverDropdown(mouseX, mouseY) && !this.filteredDestinations.isEmpty()) {
            int maxScroll = Math.max(0, this.filteredDestinations.size() - MAX_ROWS);

            if (scrollY < 0) this.destinationDropdownScroll = Math.min(maxScroll, this.destinationDropdownScroll + 1);
            if (scrollY > 0) this.destinationDropdownScroll = Math.max(0, this.destinationDropdownScroll - 1);

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }

        // Enter while dropdown open => autocomplete first visible result
        if (!loading && !saving && this.destinationDropdownOpen
                && (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER)) {
            if (!this.filteredDestinations.isEmpty()) {
                String picked = this.filteredDestinations.get(Math.min(this.destinationDropdownScroll, this.filteredDestinations.size() - 1));
                this.destinationField.setValue(picked);
                this.currentDestination = picked;
                this.destinationDropdownOpen = false;
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* -------------------- Client packet entry points -------------------- */

    public static void onServerConfig(RiftPayloads.S2C_RiftConfig payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof RiftConfigScreen screen)) return;
        if (screen.clickedTile == null || !screen.clickedTile.equals(payload.clickedTilePos())) return;

        screen.loading = false;
        screen.saving = false;

        screen.anchorPos = payload.anchorPos();

        String riftName = payload.riftName() == null ? "" : payload.riftName();
        screen.nameField.setValue(riftName);
        screen.nameField.setEditable(true);

        String dest = payload.destinationName() == null ? "" : payload.destinationName();
        screen.currentDestination = dest;
        screen.destinationField.setValue(dest);
        screen.destinationField.setEditable(true);

        screen.allDestinations.clear();
        if (payload.allDestinations() != null) screen.allDestinations.addAll(payload.allDestinations());

        screen.rebuildDestinationFilter(screen.destinationField.getValue());
        screen.saveBtn.active = true;
    }

    public static void onServerSaveResult(RiftPayloads.S2C_SaveResult payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof RiftConfigScreen screen)) return;
        if (screen.anchorPos == null || !screen.anchorPos.equals(payload.anchorPos())) return;

        screen.saving = false;

        if (mc.player != null) {
            ChatFormatting color = payload.ok() ? ChatFormatting.GREEN : ChatFormatting.RED;
            mc.player.displayClientMessage(Component.literal(payload.message()).withStyle(color), true);
        }

        if (payload.ok()) {
            screen.requestConfigFromServer();
        }
    }
}
