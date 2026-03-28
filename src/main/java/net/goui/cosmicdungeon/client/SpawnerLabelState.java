// file: src/main/java/net/goui/cosmicdungeon/client/SpawnerLabelState.java
package net.goui.cosmicdungeon.client;

/**
 * Common-safe state holder for the spawner label HUD toggle.
 *
 * IMPORTANT:
 * - This class is in the client package, but it has NO client-only imports.
 * - That means common code (network handlers) can safely reference it without
 *   dragging in Minecraft client classes.
 */
public final class SpawnerLabelState {
    private SpawnerLabelState() {}

    /**
     * Default: HIDE.
     * Volatile because it can be set by network thread -> read by render thread.
     */
    private static volatile boolean ENABLED = false;

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }
}