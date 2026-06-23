package net.goui.cosmicdungeon.playerclass.metalmancer.item;

import net.goui.cosmicdungeon.playerclass.api.ClassBoundItem;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Base class for items that only work when the player is a Metalmancer.
 *
 * Subclasses can call allowed(player) before doing anything fancy.
 */
public class MetalmancerOnlyItem extends Item implements ClassBoundItem {
    public MetalmancerOnlyItem(Properties properties) {
        super(properties);
    }

    @Override
    public String requiredClassId() {
        return ClassKeys.CLASS_ID_METALMANCER;
    }

    /** Returns true if the player currently has the Metalmancer class. */
    protected boolean allowed(Player player) {
        return ClassNbtUtil.isMetalmancer(player);
    }

    /** Static helper for any other class that wants to check Metalmancer status. */
    public static boolean isMetalmancer(Player player) {
        return ClassNbtUtil.isMetalmancer(player);
    }
}
