package net.goui.cosmicdungeon.loading;

import net.neoforged.fml.earlydisplay.DisplayWindow;

/** Retains NeoForge's window, progress, credits and Minecraft handoff. */
public final class CosmicLoadingWindow extends DisplayWindow {
    private final CosmicLoadingScreenAccess access = new CosmicLoadingScreenAccess();

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
