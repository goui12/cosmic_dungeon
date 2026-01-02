package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class HotbarOverlay {
    private HotbarOverlay() {}

    // Vanilla 18x18 slot frame from the GUI atlas (no custom texture needed)
    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");

    /** Scratch container for rendering (rebuilt each frame from client PD). */
    private static final SimpleContainer EXTRA_VIEW = new SimpleContainer(3);

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)
            return; // don’t paint over containers

        // Gate by current class (client-side copy cached in player persistent data)
        if (!ClassNbtUtil.isMetalmancer(p)) return;

        // Pull the latest client-side copy of the extra inventory from PD
        EXTRA_VIEW.clearContent();
        CompoundTag root = p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        CompoundTag extraTag = root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);

        NonNullList<ItemStack> list = NonNullList.withSize(EXTRA_VIEW.getContainerSize(), ItemStack.EMPTY);
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, p.level().registryAccess(), extraTag);
        ContainerHelper.loadAllItems(input, list);
        for (int i = 0; i < list.size(); i++) {
            EXTRA_VIEW.setItem(i, list.get(i));
        }

        GuiGraphics g = e.getGuiGraphics();

        // Layout: draw to the right of the vanilla hotbar (which ends at mid+91)
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int baseX   = screenW / 2 + 91 + 10; // small gap
        int baseY   = screenH - 24;          // align with hotbar row

        // Draw three slots + items
        for (int i = 0; i < EXTRA_VIEW.getContainerSize(); i++) {
            int x = baseX + i * 20;
            int y = baseY;

            // 18x18 vanilla slot frame from GUI atlas
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);

            ItemStack stack = EXTRA_VIEW.getItem(i);
            if (!stack.isEmpty()) {
                // Nudge in by 1–2 px so it sits nicely inside the frame
                g.renderItem(stack, x + 1, y + 1);
                g.renderItemDecorations(mc.font, stack, x + 1, y + 1);
            }
        }
    }
}
