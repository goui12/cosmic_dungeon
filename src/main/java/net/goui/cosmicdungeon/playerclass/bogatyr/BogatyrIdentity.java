package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.wolf.Wolf;
import java.util.*;

/** Only actual source removal or an exact saved delivery can move a permanent directory identity. */
public final class BogatyrIdentity {
    private record Travel(UUID owner,UUID entity,String source){}
    private static final Map<UUID,Travel> TRAVEL=new HashMap<>();
    private BogatyrIdentity(){}
    public static UUID id(Wolf wolf){return WolfIdentity.id(wolf.getPersistentData(),wolf.getUUID());}
    public static void fresh(Wolf wolf){WolfIdentity.set(wolf.getPersistentData(),UUID.randomUUID());}
    public static void clear(){TRAVEL.clear();}
    public static void departing(Wolf wolf){
        if(wolf.level() instanceof ServerLevel level&&BogatyrCompanions.owner(wolf)!=null)
            TRAVEL.put(id(wolf),new Travel(BogatyrCompanions.owner(wolf),wolf.getUUID(),level.dimension().location().toString()));
    }
    public static boolean observe(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level))return false;
        var owner=BogatyrCompanions.owner(wolf);if(owner==null)return false;
        UUID id=id(wolf);var data=BogatyrCompanionData.get(level.getServer());
        var old=data.find(id).orElse(null);String dimension=level.dimension().location().toString();
        if(data.identityHeld(dimension,wolf.getUUID()))return false;
        if(wolf.getPersistentData().contains(WolfIdentity.KEY)&&WolfIdentity.explicit(wolf.getPersistentData()).isEmpty()){
            data.identityHold(dimension,wolf.getUUID());return false;
        }
        var travel=TRAVEL.get(id);
        boolean moved=old!=null&&travel!=null&&travel.owner().equals(owner)
                &&travel.entity().equals(wolf.getUUID())&&travel.source().equals(old.dimension());
        var archive=data.archive(id).orElse(null);
        boolean targetExists=archive!=null&&(archive.targetRun()==0
                ?level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                :net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(level.getServer())
                    .getRun(archive.targetRun()).filter(run->run.containsDimension(level.dimension())
                            &&run.dungeonId().equals("dungeon_1")).isPresent());
        boolean delivered=targetExists&&archive.phase().equals(WolfArchive.RELEASING)
                &&archive.targetDimension().equals(dimension)&&wolf.getUUID().equals(id)
                &&wolf.getPersistentData().getStringOr(WolfArchive.MARKER,"").equals(archive.transaction().toString());
        if(!WolfIdentity.matches(old,owner,wolf.getUUID(),dimension,moved,delivered)){
            data.identityHold(dimension,wolf.getUUID());
            com.mojang.logging.LogUtils.getLogger().error(
                    "Companion identity conflict held: bond={} entity={} dimension={}",id,wolf.getUUID(),dimension);
            return false;
        }
        // Legacy identity remains unchanged. New bonds receive fresh IDs before enrollment.
        WolfIdentity.set(wolf.getPersistentData(),id);
        return true;
    }
    public static void recorded(Wolf wolf){TRAVEL.remove(id(wolf));}
    public static boolean held(Wolf wolf){
        return wolf.level() instanceof ServerLevel level&&BogatyrCompanionData.get(level.getServer())
                .identityHeld(level.dimension().location().toString(),wolf.getUUID());
    }
}
// TODO(M44, identity review): persistently held cloned/stale identities require developer review.
// Inspect exact owner, native UUID, bond UUID and both source/destination saves before clearing.
// A copied tagged pet is never silently adopted or permitted to overwrite another owner's entry.
