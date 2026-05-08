package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ClientTooltipComponent {
    static ClientTooltipComponent create(FormattedCharSequence text) {
        return new ClientTextTooltip(text);
    }

    static ClientTooltipComponent create(TooltipComponent visualTooltipComponent) {
        return (ClientTooltipComponent)(switch (visualTooltipComponent) {
            case BundleTooltip bundletooltip -> new ClientBundleTooltip(bundletooltip.contents());
            case ClientActivePlayersTooltip.ActivePlayersTooltip clientactiveplayerstooltip$activeplayerstooltip -> new ClientActivePlayersTooltip(
                clientactiveplayerstooltip$activeplayerstooltip
            );
            default -> {
                ClientTooltipComponent result = net.neoforged.neoforge.client.gui.ClientTooltipComponentManager.createClientTooltipComponent(visualTooltipComponent);
                if (result != null) yield result;
                throw new IllegalArgumentException("Unknown TooltipComponent");
            }
        });
    }

    int getHeight(Font font);

    int getWidth(Font font);

    default boolean showTooltipWithItemInHand() {
        return false;
    }

    default void renderText(GuiGraphics guiGraphics, Font font, int x, int y) {
    }

    default void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
    }
}
