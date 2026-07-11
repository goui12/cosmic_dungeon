package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.resources.ResourceLocation;

final class HelpMenuAssets {
    static final ResourceLocation BACKGROUND_LARGE = texture("background_large");
    static final ResourceLocation TITLE = texture("title");
    static final ResourceLocation TEXT_BUTTON = texture("text_button");
    static final ResourceLocation TEXT_BUTTON_HOVER = texture("text_button_hover");
    static final ResourceLocation TEXT_BUTTON_SELECTED = texture("text_button_selected");
    static final ResourceLocation TEXT_BUTTON_DISABLED = texture("text_button_disabled");
    static final ResourceLocation UP_BUTTON = texture("up_button");
    static final ResourceLocation UP_BUTTON_HOVER = texture("up_button_hover");
    static final ResourceLocation DOWN_BUTTON = texture("down_button");
    static final ResourceLocation DOWN_BUTTON_HOVER = texture("down_button_hover");

    static final int MENU_W = 384;
    static final int MENU_H = 240;
    static final int TITLE_W = 128;
    static final int TITLE_H = 32;
    static final int TEXT_BUTTON_W = 128;
    static final int TEXT_BUTTON_H = 24;
    static final int ARROW_W = 28;
    static final int ARROW_H = 28;

    private HelpMenuAssets() {}

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/menu/" + name + ".png");
    }
}
