package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.nbt.*;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

/** Append-only review evidence; no save mutation proceeds unless the original image is readable. */
final class CompanionReviewFiles {
    private CompanionReviewFiles(){}
    static Path write(Path folder,UUID review,boolean applied,CompoundTag evidence)throws IOException{
        Files.createDirectories(folder);
        Path path=folder.resolve(review+(applied?".applied.nbt":".prepared.nbt"));
        try(var output=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
            NbtIo.writeCompressed(evidence,output);
        }
        if(!NbtIo.readCompressed(path,NbtAccounter.create(64L*1024*1024)).equals(evidence))
            throw new IOException("Companion review evidence did not verify");
        return path;
    }
}
