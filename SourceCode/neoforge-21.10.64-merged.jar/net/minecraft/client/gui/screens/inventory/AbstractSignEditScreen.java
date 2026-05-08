package net.minecraft.client.gui.screens.inventory;

import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSignEditScreen extends Screen {
    /**
     * Reference to the sign object.
     */
    protected final SignBlockEntity sign;
    private SignText text;
    private final String[] messages;
    private final boolean isFrontText;
    protected final WoodType woodType;
    /**
     * Counts the number of screen updates.
     */
    private int frame;
    /**
     * The index of the line that is being edited.
     */
    private int line;
    @Nullable
    private TextFieldHelper signField;

    public AbstractSignEditScreen(SignBlockEntity sign, boolean isFrontText, boolean isFiltered) {
        this(sign, isFrontText, isFiltered, Component.translatable("sign.edit"));
    }

    public AbstractSignEditScreen(SignBlockEntity sign, boolean isFrontText, boolean isFiltered, Component title) {
        super(title);
        this.sign = sign;
        this.text = sign.getText(isFrontText);
        this.isFrontText = isFrontText;
        this.woodType = SignBlock.getWoodType(sign.getBlockState().getBlock());
        this.messages = IntStream.range(0, 4)
            .mapToObj(p_277214_ -> this.text.getMessage(p_277214_, isFiltered))
            .map(Component::getString)
            .toArray(String[]::new);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, p_251194_ -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()
        );
        this.signField = new TextFieldHelper(
            () -> this.messages[this.line],
            this::setMessage,
            TextFieldHelper.createClipboardGetter(this.minecraft),
            TextFieldHelper.createClipboardSetter(this.minecraft),
            p_280850_ -> this.minecraft.font.width(p_280850_) <= this.sign.getMaxTextLineWidth()
        );
    }

    @Override
    public void tick() {
        this.frame++;
        if (!this.isValid()) {
            this.onDone();
        }
    }

    private boolean isValid() {
        return this.minecraft != null
            && this.minecraft.player != null
            && !this.sign.isRemoved()
            && !this.sign.playerIsTooFarAwayToEdit(this.minecraft.player.getUUID());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isUp()) {
            this.line = this.line - 1 & 3;
            this.signField.setCursorToEnd();
            return true;
        } else if (event.isDown() || event.isConfirmation()) {
            this.line = this.line + 1 & 3;
            this.signField.setCursorToEnd();
            return true;
        } else {
            return this.signField.keyPressed(event) ? true : super.keyPressed(event);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        this.signField.charTyped(event);
        return true;
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
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 40, -1);
        this.renderSign(guiGraphics);
    }

    @Override
    public void onClose() {
        this.onDone();
    }

    @Override
    public void removed() {
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(
                new ServerboundSignUpdatePacket(
                    this.sign.getBlockPos(), this.isFrontText, this.messages[0], this.messages[1], this.messages[2], this.messages[3]
                )
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    protected abstract void renderSignBackground(GuiGraphics guiGraphics);

    protected abstract Vector3f getSignTextScale();

    protected abstract float getSignYOffset();

    private void renderSign(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.width / 2.0F, this.getSignYOffset());
        guiGraphics.pose().pushMatrix();
        this.renderSignBackground(guiGraphics);
        guiGraphics.pose().popMatrix();
        this.renderSignText(guiGraphics);
        guiGraphics.pose().popMatrix();
    }

    private void renderSignText(GuiGraphics guiGraphics) {
        Vector3f vector3f = this.getSignTextScale();
        guiGraphics.pose().scale(vector3f.x(), vector3f.y());
        int i = this.text.hasGlowingText() ? this.text.getColor().getTextColor() : AbstractSignRenderer.getDarkColor(this.text);
        boolean flag = this.frame / 6 % 2 == 0;
        int j = this.signField.getCursorPos();
        int k = this.signField.getSelectionPos();
        int l = 4 * this.sign.getTextLineHeight() / 2;
        int i1 = this.line * this.sign.getTextLineHeight() - l;

        for (int j1 = 0; j1 < this.messages.length; j1++) {
            String s = this.messages[j1];
            if (s != null) {
                if (this.font.isBidirectional()) {
                    s = this.font.bidirectionalShaping(s);
                }

                int k1 = -this.font.width(s) / 2;
                guiGraphics.drawString(this.font, s, k1, j1 * this.sign.getTextLineHeight() - l, i, false);
                if (j1 == this.line && j >= 0 && flag) {
                    int l1 = this.font.width(s.substring(0, Math.max(Math.min(j, s.length()), 0)));
                    int i2 = l1 - this.font.width(s) / 2;
                    if (j >= s.length()) {
                        guiGraphics.drawString(this.font, "_", i2, i1, i, false);
                    }
                }
            }
        }

        for (int k3 = 0; k3 < this.messages.length; k3++) {
            String s1 = this.messages[k3];
            if (s1 != null && k3 == this.line && j >= 0) {
                int l3 = this.font.width(s1.substring(0, Math.max(Math.min(j, s1.length()), 0)));
                int i4 = l3 - this.font.width(s1) / 2;
                if (flag && j < s1.length()) {
                    guiGraphics.fill(i4, i1 - 1, i4 + 1, i1 + this.sign.getTextLineHeight(), ARGB.opaque(i));
                }

                if (k != j) {
                    int j4 = Math.min(j, k);
                    int j2 = Math.max(j, k);
                    int k2 = this.font.width(s1.substring(0, j4)) - this.font.width(s1) / 2;
                    int l2 = this.font.width(s1.substring(0, j2)) - this.font.width(s1) / 2;
                    int i3 = Math.min(k2, l2);
                    int j3 = Math.max(k2, l2);
                    guiGraphics.textHighlight(i3, i1, j3, i1 + this.sign.getTextLineHeight());
                }
            }
        }
    }

    private void setMessage(String message) {
        this.messages[this.line] = message;
        this.text = this.text.setMessage(this.line, Component.literal(message));
        this.sign.setText(this.text, this.isFrontText);
    }

    private void onDone() {
        this.minecraft.setScreen(null);
    }
}
