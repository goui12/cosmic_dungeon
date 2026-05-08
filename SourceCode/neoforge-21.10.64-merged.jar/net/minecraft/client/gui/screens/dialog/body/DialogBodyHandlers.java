package net.minecraft.client.gui.screens.dialog.body;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DialogBodyHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MapCodec<? extends DialogBody>, DialogBodyHandler<?>> HANDLERS = new HashMap<>();

    private static <B extends DialogBody> void register(MapCodec<B> codec, DialogBodyHandler<? super B> handler) {
        HANDLERS.put(codec, handler);
    }

    @Nullable
    private static <B extends DialogBody> DialogBodyHandler<B> getHandler(B body) {
        return (DialogBodyHandler<B>)HANDLERS.get(body.mapCodec());
    }

    @Nullable
    public static <B extends DialogBody> LayoutElement createBodyElement(DialogScreen<?> screen, B body) {
        DialogBodyHandler<B> dialogbodyhandler = getHandler(body);
        if (dialogbodyhandler == null) {
            LOGGER.warn("Unrecognized dialog body {}", body);
            return null;
        } else {
            return dialogbodyhandler.createControls(screen, body);
        }
    }

    public static void bootstrap() {
        register(PlainMessage.MAP_CODEC, new DialogBodyHandlers.PlainMessageHandler());
        register(ItemBody.MAP_CODEC, new DialogBodyHandlers.ItemHandler());
    }

    static void runActionOnParent(DialogScreen<?> screen, @Nullable Style style) {
        if (style != null) {
            ClickEvent clickevent = style.getClickEvent();
            if (clickevent != null) {
                screen.runAction(Optional.of(clickevent));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ItemHandler implements DialogBodyHandler<ItemBody> {
        public LayoutElement createControls(DialogScreen<?> p_428557_, ItemBody p_426083_) {
            if (p_426083_.description().isPresent()) {
                PlainMessage plainmessage = p_426083_.description().get();
                LinearLayout linearlayout = LinearLayout.horizontal().spacing(2);
                linearlayout.defaultCellSetting().alignVerticallyMiddle();
                ItemDisplayWidget itemdisplaywidget = new ItemDisplayWidget(
                    Minecraft.getInstance(),
                    0,
                    0,
                    p_426083_.width(),
                    p_426083_.height(),
                    CommonComponents.EMPTY,
                    p_426083_.item(),
                    p_426083_.showDecorations(),
                    p_426083_.showTooltip()
                );
                linearlayout.addChild(itemdisplaywidget);
                linearlayout.addChild(
                    new FocusableTextWidget(
                            plainmessage.width(), plainmessage.contents(), p_428557_.getFont(), false, FocusableTextWidget.BackgroundFill.NEVER, 4
                        )
                        .configureStyleHandling(true, p_428473_ -> DialogBodyHandlers.runActionOnParent(p_428557_, p_428473_))
                );
                return linearlayout;
            } else {
                return new ItemDisplayWidget(
                    Minecraft.getInstance(),
                    0,
                    0,
                    p_426083_.width(),
                    p_426083_.height(),
                    p_426083_.item().getHoverName(),
                    p_426083_.item(),
                    p_426083_.showDecorations(),
                    p_426083_.showTooltip()
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class PlainMessageHandler implements DialogBodyHandler<PlainMessage> {
        public LayoutElement createControls(DialogScreen<?> p_428310_, PlainMessage p_428489_) {
            return new FocusableTextWidget(p_428489_.width(), p_428489_.contents(), p_428310_.getFont(), false, FocusableTextWidget.BackgroundFill.NEVER, 4)
                .configureStyleHandling(true, p_428435_ -> DialogBodyHandlers.runActionOnParent(p_428310_, p_428435_))
                .setCentered(true);
        }
    }
}
