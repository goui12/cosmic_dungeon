package net.goui.cosmicdungeon.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

/**
 * Native rocket flight/visuals with intrinsic D1 identity and configurable server explosion damage.
 * Cinderbite internal 1vyElnGqiq5ZumdIu1cF51hA6HZn_eSB4z-l61dsZEss (March26 13:25):
 * four small red bursts, no trail/flicker. Cindermaul internal
 * 1A-TIYSaInjFJOm80KrgxSFErXow4JSHz2it-cvkY6xU (March26 13:27): five large orange/red
 * bursts, trail/flicker. The later 14:08 overview sets 12/15 HP in CosmicDungeon.config.
 * One gunpowder is the default within the documented 1-3 range; stack components can author
 * flight/visuals. No existing stack or chest is converted or rewritten.
 */
public final class D1FireworkItem extends FireworkRocketItem {
    public D1FireworkItem(Properties properties, boolean cindermaul) {
        super(properties.component(DataComponents.FIREWORKS, D1RocketPayload.defaults(cindermaul)));
    }

    public static boolean isCustomRocket(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof D1FireworkItem;
    }

    public static boolean containsCustomRocket(ChargedProjectiles projectiles) {
        // Native charged stacks are copied lazily; no world scan or per-tick work.
        return projectiles.getItems().stream().anyMatch(D1FireworkItem::isCustomRocket);
    }
}
