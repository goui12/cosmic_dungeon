package net.goui.cosmicdungeon.transaction;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import java.util.Objects;
/** Shared native readback for owner-local inventory/escrow snapshots, including integrated owner. */
public final class PlayerSaveProof {
    private PlayerSaveProof(){}
    public static CompoundTag snapshot(ServerPlayer player){
        var errors=new ProblemReporter.Collector();var output=TagValueOutput.createWithContext(errors,player.registryAccess());
        player.saveWithoutId(output);
        if(!errors.isEmpty())throw new IllegalStateException(errors.getReport());return output.buildResult();
    }
    public static boolean matches(CompoundTag expected,CompoundTag actual){
        return Objects.equals(expected.get("Inventory"),actual.get("Inventory"))
                &&Objects.equals(expected.get("equipment"),actual.get("equipment"))
                &&expected.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG)
                .equals(actual.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG));
    }
    public static boolean matchesLocation(CompoundTag expected,CompoundTag actual){
        return matches(expected,actual)&&Objects.equals(expected.get("Dimension"),actual.get("Dimension"))
                &&Objects.equals(expected.get("Pos"),actual.get("Pos"))&&Objects.equals(expected.get("Rotation"),actual.get("Rotation"));
    }
    public static boolean save(ServerPlayer player){return save(player,false);}
    public static boolean saveWithLocation(ServerPlayer player){return save(player,true);}
    private static boolean save(ServerPlayer player,boolean location){
        try{
            var expected=snapshot(player);var server=player.level().getServer();
            boolean owner=server.isSingleplayerOwner(player.nameAndId());
            if(owner)server.saveEverything(true,true,true);else server.getPlayerList().getPlayerIo().save(player);
            var path=server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID()+".dat");
            var actual=NbtIo.readCompressed(path,NbtAccounter.create(64L*1024*1024));
            if(!(location?matchesLocation(expected,actual):matches(expected,actual)))return false;
            if(owner){
                var level=NbtIo.readCompressed(server.getWorldPath(LevelResource.LEVEL_DATA_FILE),NbtAccounter.create(64L*1024*1024));
                var integrated=level.getCompoundOrEmpty("Data").getCompoundOrEmpty("Player");
                return location?matchesLocation(expected,integrated):matches(expected,integrated);
            }
            return true;
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("Player inventory/escrow save requires reconciliation: {}",player.getUUID(),error);return false;}
    }
}
