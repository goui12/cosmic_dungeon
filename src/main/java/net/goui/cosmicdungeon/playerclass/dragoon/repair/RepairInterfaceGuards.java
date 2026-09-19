package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;

/** Repair 2.0: ordinary interfaces cannot restore durability or consume repair-only components. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class RepairInterfaceGuards {
    private RepairInterfaceGuards() {}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void anvil(AnvilUpdateEvent event) {
        var left=event.getLeft();var right=event.getRight();var output=event.getOutput();
        if(RepairComponents.marked(left)||RepairComponents.marked(right)
                || !output.isEmpty() && left.isDamageableItem() && output.is(left.getItem())
                && output.getDamageValue()<left.getDamageValue()) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void grindstone(GrindstoneEvent.OnPlaceItem event) {
        var a=event.getTopItem();var b=event.getBottomItem();
        if(RepairComponents.marked(a)||RepairComponents.marked(b)
                || !a.isEmpty()&&!b.isEmpty()&&a.isDamageableItem()&&b.is(a.getItem())) event.setCanceled(true);
    }
}
