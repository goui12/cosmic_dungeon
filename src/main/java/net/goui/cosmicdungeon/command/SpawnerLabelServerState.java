package net.goui.cosmicdungeon.command;
import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.payload.SpawnerLabelPayload;
import net.minecraft.server.level.ServerPlayer;
/** Detailed hover is intrinsic to the current Developer rank; no per-player toggle or saved preference. */
public final class SpawnerLabelServerState {
    private SpawnerLabelServerState(){}
    public static boolean isEffective(ServerPlayer p){return p!=null&&Authority.isDeveloper(p);}
    public static void sync(ServerPlayer p){if(p!=null)ModNetwork.sendTo(p,new SpawnerLabelPayload(isEffective(p)));}
    public static void revoke(ServerPlayer p){if(p!=null)ModNetwork.sendTo(p,new SpawnerLabelPayload(false));}
}
