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
    public static boolean save(ServerPlayer player){
        try{
            var expected=snapshot(player);var server=player.level().getServer();
            boolean owner=server.isSingleplayerOwner(player.nameAndId());
            if(owner)server.saveEverything(true,true,true);else server.getPlayerList().getPlayerIo().save(player);
            var path=server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID()+".dat");
            if(!matches(expected,NbtIo.readCompressed(path,NbtAccounter.create(64L*1024*1024))))return false;
            if(owner){
                var level=NbtIo.readCompressed(server.getWorldPath(LevelResource.LEVEL_DATA_FILE),NbtAccounter.create(64L*1024*1024));
                return matches(expected,level.getCompoundOrEmpty("Data").getCompoundOrEmpty("Player"));
            }
            return true;
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("Player inventory/escrow save requires reconciliation: {}",player.getUUID(),error);return false;}
    }
}
