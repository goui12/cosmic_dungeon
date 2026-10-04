package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.custom.D1_Class_Selector_Block;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/** One small personal class label, attached to the block outline actually selected this frame. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class ClassSelectorHoverLabel {
    private static final ClassSelectorHoverLabel INSTANCE = new ClassSelectorHoverLabel();
    private final ClassSelectorHoverSelection selection = new ClassSelectorHoverSelection();

    private ClassSelectorHoverLabel() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void extract(ExtractBlockOutlineRenderStateEvent event) {
        INSTANCE.capture(event, Minecraft.getInstance());
    }

    private void capture(ExtractBlockOutlineRenderStateEvent event, Minecraft client) {
        if (!(event.getBlockState().getBlock() instanceof D1_Class_Selector_Block)
                || client.player == null || client.level != event.getLevel() || client.screen != null
                || client.options.hideGui || client.getCameraEntity() != client.player
                || client.player.isSpectator()) return;
        float partialTick = event.getCamera().getPartialTickTime();
        if (!selection.canShow(event.getBlockPos(), event.getHitResult(),
                client.player.getEyePosition(partialTick), client.player.getViewVector(partialTick),
                client.player.blockInteractionRange())) return;
        var text = selection.label(ClassData.getClassId(client.player)).getVisualOrderText();
        event.addCustomRenderer(new Label(client.font, text, client.font.width(text)));
    }

    /** Capture only this frame's text/font; no player, level, block entity or mutable live state. */
    private record Label(Font font, FormattedCharSequence text, int width) implements CustomBlockOutlineRenderer {
        @Override
        public boolean render(BlockOutlineRenderState outline, MultiBufferSource.BufferSource buffer,
                              PoseStack pose, boolean translucentPass, LevelRenderState levelState) {
            if (outline.isTranslucent() != translucentPass) return false;
            var pos = outline.pos();
            var camera = levelState.cameraRenderState;
            pose.pushPose();
            try {
                pose.translate(pos.getX() + 0.5 - camera.pos.x,
                        pos.getY() + 2.3 - camera.pos.y, pos.getZ() + 0.5 - camera.pos.z);
                pose.mulPose(camera.orientation);
                pose.scale(0.018F, -0.018F, 0.018F);
                font.drawInBatch(text, -width / 2.0F, 0, 0xFFE4DAFF, true, pose.last().pose(),
                        buffer, Font.DisplayMode.NORMAL, 0x990B0C12, LightTexture.FULL_BRIGHT);
            } finally {
                pose.popPose();
            }
            return false; // Preserve the native fitted highlight.
        }
    }
}
