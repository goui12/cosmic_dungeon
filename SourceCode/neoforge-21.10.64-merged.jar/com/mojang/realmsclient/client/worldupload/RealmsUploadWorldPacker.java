package com.mojang.realmsclient.client.worldupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadWorldPacker {
    private static final long SIZE_LIMIT = 5368709120L;
    private static final String WORLD_FOLDER_NAME = "world";
    private final BooleanSupplier isCanceled;
    private final Path directoryToPack;

    public static File pack(Path directoryToPack, BooleanSupplier isCanceled) throws IOException {
        return new RealmsUploadWorldPacker(directoryToPack, isCanceled).tarGzipArchive();
    }

    private RealmsUploadWorldPacker(Path directoryToPack, BooleanSupplier isCanceled) {
        this.isCanceled = isCanceled;
        this.directoryToPack = directoryToPack;
    }

    private File tarGzipArchive() throws IOException {
        TarArchiveOutputStream tararchiveoutputstream = null;

        File file2;
        try {
            File file1 = File.createTempFile("realms-upload-file", ".tar.gz");
            tararchiveoutputstream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file1)));
            tararchiveoutputstream.setLongFileMode(3);
            this.addFileToTarGz(tararchiveoutputstream, this.directoryToPack, "world", true);
            if (this.isCanceled.getAsBoolean()) {
                throw new RealmsUploadCanceledException();
            }

            tararchiveoutputstream.finish();
            this.verifyBelowSizeLimit(file1.length());
            file2 = file1;
        } finally {
            if (tararchiveoutputstream != null) {
                tararchiveoutputstream.close();
            }
        }

        return file2;
    }

    private void addFileToTarGz(TarArchiveOutputStream stream, Path directory, String prefix, boolean isRootDirectory) throws IOException {
        if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
        } else {
            this.verifyBelowSizeLimit(stream.getBytesWritten());
            File file1 = directory.toFile();
            String s = isRootDirectory ? prefix : prefix + file1.getName();
            TarArchiveEntry tararchiveentry = new TarArchiveEntry(file1, s);
            stream.putArchiveEntry(tararchiveentry);
            if (file1.isFile()) {
                try (InputStream inputstream = new FileInputStream(file1)) {
                    inputstream.transferTo(stream);
                }

                stream.closeArchiveEntry();
            } else {
                stream.closeArchiveEntry();
                File[] afile = file1.listFiles();
                if (afile != null) {
                    for (File file2 : afile) {
                        this.addFileToTarGz(stream, file2.toPath(), s + "/", false);
                    }
                }
            }
        }
    }

    private void verifyBelowSizeLimit(long size) {
        if (size > 5368709120L) {
            throw new RealmsUploadTooLargeException(5368709120L);
        }
    }
}
