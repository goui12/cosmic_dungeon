package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.client.SkillKeybindClient;
import net.goui.cosmicdungeon.client.screen.skills.SkillBindingLabels;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient;
import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** A native, focusable action in the reclaimed class area; carried items retain inventory input. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class InventoryResurrectButton extends Button {
    private final AbstractContainerScreen<?> parent;
    private Component description = Component.empty();
    private InventoryResurrectButton(AbstractContainerScreen<?> parent) {
        super(parent.getGuiLeft() + 98, parent.getGuiTop() + 49, 73, 20,
                Component.literal("Resurrect..."), button -> ((InventoryResurrectButton)button).openPicker(), DEFAULT_NARRATION);
        this.parent = parent; active = false;
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (ClassInventoryPresentation.supports(event.getScreen()))
            event.addListener(new InventoryResurrectButton((AbstractContainerScreen<?>)event.getScreen()));
    }
    private boolean available() {
        var mc = Minecraft.getInstance();
        return mc.screen == parent && mc.player != null && mc.player.isAlive()
                && mc.player.containerMenu == parent.getMenu() && parent.getMenu().getCarried().isEmpty()
                && ((net.goui.cosmicdungeon.mixin.client.InventoryDragAccess)parent).cosmicdungeon$draggingItem().isEmpty()
                && ClassInventoryPresentation.theme().classId().equals("theurgist") && TheurgistClient.canOpenPicker()
                && !SkillsPanelClient.ownsInput(parent, getX() + width / 2.0, getY() + height / 2.0);
    }
    private void openPicker() {
        if (available()) Minecraft.getInstance().setScreen(new ResurrectPickerScreen(parent));
    }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
        // Keep rendering eligibility checks so late class/run sync can reveal the action.
        active = available();
        if (!ClassInventoryPresentation.theme().classId().equals("theurgist")) { setTooltip(null); description = Component.empty(); return; }
        Component current = SkillBindingLabels.tooltip("theurgist_resurrect", "Resurrect", TheurgistClient.resurrectionTooltip());
        Component message = Component.literal("Resurrect... ").append(SkillKeybindClient.hint("theurgist_resurrect"));
        if (!message.equals(getMessage())) setMessage(message);
        if (!description.equals(current)) { description = current; setTooltip(Tooltip.create(current)); }
        super.renderWidget(g, mx, my, partialTick);
    }
    @Override public void renderString(GuiGraphics g, Font font, int color) {
        SkillBindingLabels.draw(g, font, "theurgist_resurrect", "Resurrect...",
                new Rect(getX(), getY(), getWidth(), getHeight()), color);
    }
}
