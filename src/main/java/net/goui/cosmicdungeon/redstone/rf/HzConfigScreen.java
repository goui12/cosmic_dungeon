package net.goui.cosmicdungeon.redstone.rf;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class HzConfigScreen extends Screen {
    /* -------- open helpers -------- */
    private static BlockPos pendingPos;
    private static boolean pendingIsReceiver;

    public static void openForTransmitter(BlockPos pos) {
        pendingPos = pos;
        pendingIsReceiver = false;
        Minecraft.getInstance().setScreen(new HzConfigScreen());
    }

    public static void openForReceiver(BlockPos pos) {
        pendingPos = pos;
        pendingIsReceiver = true;
        Minecraft.getInstance().setScreen(new HzConfigScreen());
    }

    /* -------- art / exact layout from PNG -------- */
    private static final ResourceLocation GUI_TEX_TX =
            ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/redstone_hz_gui_tx.png");
    private static final ResourceLocation GUI_TEX_RX =
            ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/redstone_hz_gui_rx.png");

    // The PNG provided is exactly 230x230
    private static final int GUI_W = 230, GUI_H = 230;

    // Exact inner cutouts (x,y,w,h) measured from the PNG
    // Current Hz display box
    private static final int CURR_X = 21, CURR_Y = 86, CURR_W = 193, CURR_H = 23;
    // New Hz input box
    private static final int NEW_X  = 21, NEW_Y  = 147, NEW_W  = 193, NEW_H  = 25;
    // Buttons (left = Save, right = Close)
    private static final int SAVE_X = 19,  SAVE_Y = 186, SAVE_W = 88, SAVE_H = 28;
    private static final int CLOSE_X = 119, CLOSE_Y = 186, CLOSE_W = 93, CLOSE_H = 28;

    // Minor padding so the MC widgets don’t touch the bevel
    private static final int INSET_X = 2;
    private static final int INSET_Y = 2;

    /* -------- state -------- */
    private EditBox newHzField;
    private int guiLeft, guiTop;
    private int currentHz = 1;

    public HzConfigScreen() {
        super(Component.literal("Redstone RF Config"));
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - GUI_W) / 2;
        this.guiTop  = (this.height - GUI_H) / 2;

        var mc = Minecraft.getInstance();
        Integer cached = RfNet.ClientCache.getHz(pendingPos);

        if (cached != null) {
            currentHz = clampHz(cached);
        } else {
            // request from server
            if (mc.getConnection() != null && pendingPos != null) {
                mc.getConnection().send(new ServerboundCustomPayloadPacket(new RfNet.C2S_RequestHz(pendingPos)));
                // show interim feedback
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            Component.literal("Requesting current Hz...").withStyle(ChatFormatting.YELLOW),
                            true
                    );
                }
            } else {
                currentHz = -1; // fallback
            }
        }

        // create New Hz input field
        int fieldH = 20;
        int fieldY = guiTop + NEW_Y + (NEW_H - fieldH) / 2;
        newHzField = new EditBox(
                this.font,
                guiLeft + NEW_X + INSET_X,
                fieldY,
                NEW_W - (INSET_X * 2),
                fieldH,
                Component.literal("Hz"));
        newHzField.setMaxLength(3);
        newHzField.setFilter(s -> s.matches("\\d{0,3}"));
        newHzField.setValue(currentHz > 0 ? String.valueOf(currentHz) : "");
        addRenderableWidget(newHzField);
        setInitialFocus(newHzField);

        // buttons
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> sendHz(false))
                .bounds(guiLeft + SAVE_X, guiTop + SAVE_Y, SAVE_W, SAVE_H)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(guiLeft + CLOSE_X, guiTop + CLOSE_Y, CLOSE_W, CLOSE_H)
                .build());

        // announce loaded Hz or NULL
        if (mc.player != null) {
            String msg = (currentHz > 0)
                    ? "Current Hz: " + currentHz
                    : "Current Hz: NULL";
            mc.player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA), true);
        }
    }


    private static int clampHz(int v) {
        return v < 1 ? 1 : Math.min(v, 999);
    }


    private void sendHz(boolean closeAfter) {
        if (pendingPos == null) return;

        int v;
        try {
            v = Integer.parseInt(newHzField.getValue());
        } catch (Exception ignored) {
            v = currentHz;
        }
        v = clampHz(v);

        // 1) Update UI/cache right away
        currentHz = v;
        newHzField.setValue(String.valueOf(v)); // keep field in sync/clean
        RfNet.ClientCache.putHz(pendingPos, v);

        // 2) Show immediate client-side confirmation (overlay line)
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("Hz set to " + v + " (sending to server)")
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }

        // 3) Send to server
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundCustomPayloadPacket(new RfNet.C2S_SetHz(pendingPos, v)));
        }

        if (closeAfter) onClose();
    }


    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float dt) {
        // choose background by block type
        ResourceLocation bg = pendingIsReceiver ? GUI_TEX_RX : GUI_TEX_TX;

        // draw panel
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                bg, guiLeft, guiTop, 0, 0, GUI_W, GUI_H, GUI_W, GUI_H);

        // draw current Hz text (opaque color!)
        String text = (currentHz > 0) ? currentHz + " Hz" : "NULL";
        int textW = this.font.width(text);
        int textX = guiLeft + CURR_X + (CURR_W - textW) / 2;
        int textY = guiTop + CURR_Y + (CURR_H - 9) / 2;
        g.drawString(this.font, text, textX, textY, 0xFF222222, false);

        super.render(g, mouseX, mouseY, dt);
    }


    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float dt) {
        // Single blur layer; avoids “blur twice” issues
        this.renderTransparentBackground(g);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            sendHz(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    boolean isForPos(BlockPos pos) {
        return pendingPos != null && pendingPos.equals(pos);
    }

    void acceptServerHz(int hz) {
        this.currentHz = clampHz(hz);
        if (this.newHzField != null) { // name accordingly
            // Leave "New Hz" as the user is typing; we only update the read-only display
        }
    }
    // Called from the client packet handler when S2C_HzSync arrives
    public static void onServerHz(BlockPos pos, int hz) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof HzConfigScreen screen && screen.isForPos(pos)) {
            screen.currentHz = hz > 0 ? hz : -1; // -1 means NULL
        }
    }

}
