package net.goui.cosmicdungeon.transaction;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Native SavedData writes swallow I/O errors; critical handoffs require a codec readback. */
public final class SavedDataProof {
    private static final Map<MinecraftServer,Set<String>> VALIDATED=new WeakHashMap<>();
    private SavedDataProof(){}
    private static Path path(MinecraftServer server,String id){
        return server.getWorldPath(LevelResource.ROOT).resolve("data/"+id+".dat");
    }
    public static <T> T read(MinecraftServer server,String id,Codec<T> codec)throws java.io.IOException{
        var root=NbtIo.readCompressed(path(server,id),NbtAccounter.create(64L*1024*1024));
        return codec.parse(NbtOps.INSTANCE,root.getCompound("data").orElseThrow()).getOrThrow();
    }
    public static <T> void validate(MinecraftServer server,String id,Codec<T> codec){
        var ids=VALIDATED.computeIfAbsent(server,s->new HashSet<>());
        if(ids.contains(id))return;
        try{if(Files.exists(path(server,id)))read(server,id,codec);ids.add(id);}
        catch(Exception failure){throw new IllegalStateException(id+" needs review; original save preserved",failure);}
    }
    public static <T extends SavedData> boolean save(MinecraftServer server,String id,Codec<T> codec,T data){
        if(server==null)return false;
        try{
            var expected=codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow();
            data.setDirty();server.overworld().getDataStorage().saveAndJoin();
            if(expected.equals(codec.encodeStart(NbtOps.INSTANCE,read(server,id,codec)).getOrThrow()))return true;
        }catch(Exception failure){com.mojang.logging.LogUtils.getLogger().error("Save verification failed: {}",id,failure);}
        data.setDirty();return false;
    }
}
