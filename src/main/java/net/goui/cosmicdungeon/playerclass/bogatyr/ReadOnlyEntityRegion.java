package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Read-only verification of one saved entity chunk; never opens a live .mca for writing. */
public final class ReadOnlyEntityRegion {
    private ReadOnlyEntityRegion(){}
    public static CompoundTag read(Path folder,int chunkX,int chunkZ)throws IOException{
        Path path=folder.resolve("r."+(chunkX>>5)+"."+(chunkZ>>5)+".mca");
        if(!Files.isRegularFile(path))return null;
        try(var file=new RandomAccessFile(path.toFile(),"r")){
            if(file.length()<8192)throw new IOException("Truncated entity region header");
            file.seek(4L*((chunkX&31)+32*(chunkZ&31)));
            int location=file.readInt();if(location==0)return null;
            int sector=location>>>8,sectors=location&255;
            long offset=4096L*sector;
            if(sector<2||sectors==0||offset+5>file.length())throw new IOException("Invalid entity sector");
            file.seek(offset);int length=file.readInt(),compression=file.readUnsignedByte();
            if(length<1||length>4096L*sectors-4||offset+4L+length>file.length())
                throw new IOException("Invalid entity chunk length");
            var version=RegionFileVersion.fromId(compression&127);
            if(version==null||(compression&127)==127)throw new IOException("Unsupported entity compression");
            if((compression&128)!=0){
                if(length!=1)throw new IOException("Invalid external entity chunk");
                Path external=folder.resolve("c."+chunkX+"."+chunkZ+".mcc");
                try(var raw=Files.newInputStream(external);var input=new DataInputStream(version.wrap(raw))){
                    return checked(NbtIo.read(input,NbtAccounter.create(64L*1024*1024)),chunkX,chunkZ);
                }
            }
            byte[] bytes=new byte[length-1];file.readFully(bytes);
            try(var input=new DataInputStream(version.wrap(new ByteArrayInputStream(bytes)))){
                return checked(NbtIo.read(input,NbtAccounter.create(64L*1024*1024)),chunkX,chunkZ);
            }
        }
    }
    private static CompoundTag checked(CompoundTag tag,int chunkX,int chunkZ)throws IOException{
        var position=tag.getIntArray("Position").orElse(new int[0]);
        if(position.length!=2||position[0]!=chunkX||position[1]!=chunkZ)
            throw new IOException("Entity chunk position mismatch");
        return tag;
    }
    public static Optional<CompoundTag> find(CompoundTag chunk,UUID wolf){
        if(chunk==null)return Optional.empty();
        CompoundTag match=null;
        for(var tag:chunk.getListOrEmpty("Entities")){
            if(tag instanceof CompoundTag entity&&entity.read("UUID",UUIDUtil.CODEC).filter(wolf::equals).isPresent()){
                if(match!=null)throw new IllegalStateException("Duplicate saved wolf UUID");
                match=entity;
            }
        }
        return Optional.ofNullable(match).map(CompoundTag::copy);
    }
}
