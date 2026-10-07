package net.goui.cosmicdungeon.client.screen.settings;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

public final class CosmicDungeonOptionsIntegration {
    private CosmicDungeonOptionsIntegration() {}

    public static void registerConfigScreen(ModContainer container) {
        net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient.classHelp((parent, classId) ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        net.goui.cosmicdungeon.client.screen.HelpMenuScreen.forClass(parent, classId)));
        Supplier<IConfigScreenFactory> factory = () -> (modContainer, parent) -> new CosmicDungeonOptionsScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
