package net.goui.cosmicdungeon.mercenary;

import java.util.UUID;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.playerclass.api.ClassItemEquipmentGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Group members may contribute armor; the summon keeps its existing hirer and mercenary bond. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryWolfArmor {
    private MercenaryWolfArmor() {}
    static boolean allowed(DungeonRunRegistryData.RunRecord run, MercenaryWolves.Bond bond,
            UUID owner, UUID actor, String dimension) {
        return MercenaryWolves.admitted(run,bond,owner,dimension)
                &&run.containsPlayer(actor)&&!run.isCompletionExited(actor);
    }
    static boolean canEquip(Wolf wolf, ItemStack stack) {
        return wolf.isAlive()&&wolf.isTame()&&!wolf.isBaby()&&!wolf.isWearingBodyArmor()
                &&stack.is(Items.WOLF_ARMOR)&&wolf.isEquippableInSlot(stack,EquipmentSlot.BODY);
    }
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)||!(event.getTarget() instanceof Wolf wolf)
                ||!MercenaryWolves.managed(wolf)||!event.getItemStack().is(Items.WOLF_ARMOR)) return;
        // A rejected request cannot fall through into vanilla owner interactions.
        event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);
        if(wolf.level()!=player.level()||!ClassItemEquipmentGuard.canUse(player,event.getItemStack()))return;
        var run=D1Members.run(player.level()).orElse(null);
        var bond=MercenaryWolves.bond(wolf);
        var owner=wolf.getOwnerReference();
        if(!allowed(run,bond,owner==null?null:owner.getUUID(),player.getUUID(),
                player.level().dimension().location().toString())||!D1Members.inside(player,run)
                ||D1RunData.get(player.level().getServer()).count(bond.run(),MercenaryWolves.dismissed(bond.mercenary()))!=0
                ||!canEquip(wolf,event.getItemStack()))return;
        wolf.setBodyArmorItem(event.getItemStack().copyWithCount(1));
        event.getItemStack().consume(1,player);
        wolf.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_INTERACT,player);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
