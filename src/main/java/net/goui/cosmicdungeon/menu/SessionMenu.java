package net.goui.cosmicdungeon.menu;

import java.util.UUID;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Transient opening identity; never saved as an account or world identifier. */
public interface SessionMenu {
    UUID sessionId();

    static boolean matches(int expectedContainer, UUID expectedSession, int container, UUID session) {
        return expectedSession != null && expectedContainer == container && expectedSession.equals(session);
    }

    static boolean matches(AbstractContainerMenu menu, int container, UUID session) {
        return menu instanceof SessionMenu scoped && matches(menu.containerId, scoped.sessionId(), container, session);
    }
}
