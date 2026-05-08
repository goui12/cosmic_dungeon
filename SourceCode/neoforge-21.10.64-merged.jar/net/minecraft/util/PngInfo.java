package net.minecraft.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

public record PngInfo(int width, int height) {
    private static final HexFormat FORMAT = HexFormat.of().withUpperCase().withPrefix("0x");
    private static final long PNG_HEADER = -8552249625308161526L;
    private static final int IHDR_TYPE = 1229472850;
    private static final int IHDR_SIZE = 13;

    public static PngInfo fromStream(InputStream stream) throws IOException {
        DataInputStream datainputstream = new DataInputStream(stream);
        long i = datainputstream.readLong();
        if (i != -8552249625308161526L) {
            throw new IOException("Bad PNG Signature: " + FORMAT.toHexDigits(i));
        } else {
            int j = datainputstream.readInt();
            if (j != 13) {
                throw new IOException("Bad length for IHDR chunk: " + j);
            } else {
                int k = datainputstream.readInt();
                if (k != 1229472850) {
                    throw new IOException("Bad type for IHDR chunk: " + FORMAT.toHexDigits(k));
                } else {
                    int l = datainputstream.readInt();
                    int i1 = datainputstream.readInt();
                    return new PngInfo(l, i1);
                }
            }
        }
    }

    public static PngInfo fromBytes(byte[] bytes) throws IOException {
        return fromStream(new ByteArrayInputStream(bytes));
    }

    public static void validateHeader(ByteBuffer buffer) throws IOException {
        ByteOrder byteorder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);
        if (buffer.getLong(0) != -8552249625308161526L) {
            throw new IOException("Bad PNG Signature");
        } else if (buffer.getInt(8) != 13) {
            throw new IOException("Bad length for IHDR chunk!");
        } else if (buffer.getInt(12) != 1229472850) {
            throw new IOException("Bad type for IHDR chunk!");
        } else {
            buffer.order(byteorder);
        }
    }
}
