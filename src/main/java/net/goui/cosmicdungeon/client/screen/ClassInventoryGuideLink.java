package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Native focusable/narrated hyperlink; opening help preserves the exact parent menu. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class ClassInventoryGuideLink extends Button {
    private static final Tooltip GUIDE_TOOLTIP = Tooltip.create(Component.translatable("gui.cosmicdungeon.inventory.class_guide"));
    private final AbstractContainerScreen<?> parent;

    private ClassInventoryGuideLink(AbstractContainerScreen<?> parent) {
        super(parent.getGuiLeft() + ClassInventoryPresentation.LINK_X,
                parent.getGuiTop() + ClassInventoryPresentation.LINK_Y,
                ClassInventoryPresentation.LINK_WIDTH, ClassInventoryPresentation.LINK_HEIGHT,
                Component.empty(), button -> ((ClassInventoryGuideLink)button).openGuide(), DEFAULT_NARRATION);
        this.parent = parent;
        updateState();
    }
    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (ClassInventoryPresentation.supports(event.getScreen()))
            event.addListener(new ClassInventoryGuideLink((AbstractContainerScreen<?>)event.getScreen()));
    }
    private boolean available() {
        return Minecraft.getInstance().screen == parent && parent.getMenu().getCarried().isEmpty()
                && ((net.goui.cosmicdungeon.mixin.client.InventoryDragAccess)parent).cosmicdungeon$draggingItem().isEmpty()
                && ClassInventoryPresentation.theme().hasClass()
                && !SkillsPanelClient.ownsInput(parent, getX() + width / 2.0, getY() + height / 2.0);
    }
    private void updateState() {
        var theme = ClassInventoryPresentation.theme();
        visible = true; // Keep refreshing through a late class-sync or a class change.
        active = available();
        setTooltip(active ? GUIDE_TOOLTIP : null);
        setMessage(Component.translatable("playerclass.cosmicdungeon." + theme.classId()));
    }
    private void openGuide() {
        if (available())
            Minecraft.getInstance().setScreen(HelpMenuScreen.forClass(parent, ClassInventoryPresentation.theme().classId()));
    }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
        updateState();
        if (!ClassInventoryPresentation.theme().hasClass()) return;
        var font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(getMessage().getString(), width);
        int textWidth = font.width(label);
        boolean highlight = active && (isHoveredOrFocused());
        g.drawString(font, label, getX(), getY() + 1, highlight ? 0xFF73BBFF : 0xFFF2F4F7, false);
        if (highlight) g.fill(getX(), getY() + 10, getX() + textWidth, getY() + 11, 0xFF73BBFF);
    }
}
