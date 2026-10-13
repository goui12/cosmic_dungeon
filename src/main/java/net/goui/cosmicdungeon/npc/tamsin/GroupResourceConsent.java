package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Run-frozen explicit consent; every inventory debit still revalidates live donor eligibility. */
public final class GroupResourceConsent {
    private static final String KEY="resource_sharing_v1:";
    private GroupResourceConsent() {}
    public static boolean inventoryContext(ServerPlayer player) {
        return player.containerMenu == player.inventoryMenu
                || player.containerMenu instanceof net.goui.cosmicdungeon.playerclass.api.ExtraInventoryMenu
                && player.containerMenu.stillValid(player);
    }
    public static String choice(D1RunData data,long run,UUID member) {
        var values=data.values(run,KEY+member);
        return values.size()==1 && Set.of("YES","NO").contains(values.getFirst()) ? values.getFirst() : "UNKNOWN";
    }
    public static void carryIntoRun(MinecraftServer server,D1PartyLobby.Party party) {
        var run=DungeonRunRegistryData.get(server).findRunForPlayer(party.leader()).orElse(null);
        if(party.phase()!=D1PartyLobby.Phase.PREPARING||run==null||run.stateEnum()!=DungeonRunState.ACTIVE
                ||!new HashSet<>(run.orderedPlayers()).equals(new HashSet<>(party.members())))return;
        var data=D1RunData.get(server);
        if(data.sealed(run.runId()))return;
        net.goui.cosmicdungeon.dungeon.metrics.DungeonMetricsService.party(server,run,party.id());
        for(UUID id:party.members()) {
            String choice=party.resourceSharing(id);
            // Existing/unknown saved values belong to this run; replay must never replace them.
            if(!choice.equals("UNKNOWN")&&data.values(run.runId(),KEY+id).isEmpty())
                data.setValue(run.runId(),KEY+id,choice);
        }
    }
    private static boolean online(ServerPlayer player) {
        return player!=null&&player.connection!=null&&!player.hasDisconnected()
                &&player.connection.isAcceptingMessages()
                &&player.level().getServer().getPlayerList().getPlayer(player.getUUID())==player;
    }
    private static DungeonRunRegistryData.RunRecord active(ServerPlayer player) {
        if(!online(player))return null;
        var server=player.level().getServer();
        var run=DungeonRunRegistryData.get(server).findRunForPlayer(player.getUUID()).orElse(null);
        return run!=null&&run.stateEnum()==DungeonRunState.ACTIVE&&!run.isCompletionExited(player.getUUID())
                &&run.containsDimension(player.level().dimension())&&!D1RunData.get(server).sealed(run.runId()) ? run : null;
    }
    public static PartyPayloads.Sharing view(ServerPlayer player,D1PartyLobby.Party party) {
        if(party!=null&&party.members().contains(player.getUUID()))
            return new PartyPayloads.Sharing(party.id().toString(),party.resourceSharing(player.getUUID()));
        var run=active(player);
        return run==null?PartyPayloads.Sharing.NONE:new PartyPayloads.Sharing(Long.toString(run.runId()),
                choice(D1RunData.get(player.level().getServer()),run.runId(),player.getUUID()));
    }
    public static String choose(ServerPlayer player,long revision,String context,boolean enabled) {
        var run=active(player);
        if(run==null||run.runId()!=revision||!Long.toString(run.runId()).equals(context))
            return "Dungeon group changed; review your resource choice.";
        return "Resource choices are locked for this adventure. Change them after returning to pre-game.";
    }
    /** Live execution guard, never a cached authorization; distance within the run is irrelevant. */
    public static boolean canContribute(ServerPlayer requester,ServerPlayer donor,long expectedRun) {
        var run=active(requester);
        var donorRun=active(donor);
        return run!=null&&donorRun!=null&&run.runId()==expectedRun&&donorRun.runId()==expectedRun
                &&donorRun.stateEnum()==DungeonRunState.ACTIVE&&!donorRun.isCompletionExited(donor.getUUID())
                &&!D1RunData.get(donor.level().getServer()).sealed(expectedRun)
                &&!net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(requester)
                &&!net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(donor)
                &&requester.level().getServer()==donor.level().getServer()
                &&requester.isAlive()&&!requester.isSpectator()&&donor.isAlive()&&!donor.isSpectator()
                &&!requester.getUUID().equals(donor.getUUID())
                &&choice(D1RunData.get(donor.level().getServer()),expectedRun,donor.getUUID()).equals("YES");
    }
}
