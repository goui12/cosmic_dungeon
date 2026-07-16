package net.goui.cosmicdungeon.client.screen.settings;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

public final class CosmicDungeonOptionsIntegration {
    private CosmicDungeonOptionsIntegration() {}

    public static void registerConfigScreen(ModContainer container) {
        Supplier<IConfigScreenFactory> factory = () -> (modContainer, parent) -> new CosmicDungeonOptionsScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
