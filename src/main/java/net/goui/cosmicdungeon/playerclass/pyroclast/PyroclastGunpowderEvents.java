package net.goui.cosmicdungeon.playerclass.pyroclast;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.List;

/** Debloated Classes!S6: right-click Flint with Gravel in inventory; all quantities are server config. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class PyroclastGunpowderEvents {
    private PyroclastGunpowderEvents() {}
    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItemStack().is(Items.FLINT)
                || !ClassData.getClassId(player).equals("pyroclast")) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.getCooldowns().isOnCooldown(event.getItemStack())) return;
        var inv=player.getInventory();
        if (count(player,Items.FLINT)<Config.PYRO_FLINT_COST.get() || count(player,Items.GRAVEL)<Config.PYRO_GRAVEL_COST.get()) {
            player.displayClientMessage(Component.literal("Flint Alchemy needs flint and gravel."),true); return;
        }
        List<ItemStack> before=new ArrayList<>();
        for(int i=0;i<inv.getContainerSize();i++) before.add(inv.getItem(i).copy());
        consume(player,Items.FLINT,Config.PYRO_FLINT_COST.get());
        consume(player,Items.GRAVEL,Config.PYRO_GRAVEL_COST.get());
        var output=new ItemStack(Items.GUNPOWDER,Config.PYRO_GUNPOWDER_YIELD.get());
        // Pricing Master conversion rule: unpriced alchemy inputs must not mint sellable output.
        var provenance=new net.minecraft.nbt.CompoundTag();
        output.set(net.goui.cosmicdungeon.component.ModDataComponents.VENDOR_PURCHASE_CAP.get(),0L);
        provenance.putString("cosmicdungeon_origin","flint_alchemy");
        output.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(provenance));
        if(!inv.add(output) || !output.isEmpty()) {
            for(int i=0;i<before.size();i++) inv.setItem(i,before.get(i));
            player.displayClientMessage(Component.literal("Make room for the gunpowder."),true);
        } else {
            player.getCooldowns().addCooldown(new ItemStack(Items.FLINT),5);
        }
        inv.setChanged(); player.inventoryMenu.broadcastChanges();
    }
    private static int count(ServerPlayer player,Item item) {
        int count=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var s=player.getInventory().getItem(i);
            if(s.is(item)&&!RepairComponents.marked(s)) count+=s.getCount();
        }
        return count;
    }
    private static void consume(ServerPlayer player,Item item,int count) {
        for(int i=0;i<player.getInventory().getContainerSize()&&count>0;i++) {
            var s=player.getInventory().getItem(i);
            if(!s.is(item)||RepairComponents.marked(s)) continue;
            int take=Math.min(count,s.getCount());s.shrink(take);count-=take;
        }
    }
}
