package net.goui.cosmicdungeon.loading;

import net.neoforged.fml.earlydisplay.DisplayWindow;

/** Retains NeoForge's window, progress, credits and Minecraft handoff. */
public final class CosmicLoadingWindow extends DisplayWindow {
    private final CosmicLoadingScreenAccess access = new CosmicLoadingScreenAccess();

    @Override
    public void initialize(net.neoforged.fml.loading.ProgramArgs arguments) {
        try {
            CosmicLoadingAssets.prepare(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        } catch (java.io.IOException failure) {
            System.getLogger(CosmicLoadingWindow.class.getName()).log(System.Logger.Level.WARNING,
                    "Could not prepare Cosmic Dungeon loading assets; NeoForge retains its fallback theme.", failure);
        }
        super.initialize(arguments);
    }

    @Override
    public String name() {
        return "cosmicdungeon";
    }

    @Override
    public void initWindow() {
        super.initWindow();
        // Native initialize constructs its renderer after this method returns.
        access.wrapRendererConstruction(this);
    }
}
