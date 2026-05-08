package com.mojang.blaze3d.platform;

import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import net.minecraft.util.StringDecomposer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class ClipboardManager {
    public static final int FORMAT_UNAVAILABLE = 65545;
    private final ByteBuffer clipboardScratchBuffer = BufferUtils.createByteBuffer(8192);

    public String getClipboard(Window window, GLFWErrorCallbackI errorCallback) {
        GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback(errorCallback);
        String s = GLFW.glfwGetClipboardString(window.handle());
        s = s != null ? StringDecomposer.filterBrokenSurrogates(s) : "";
        GLFWErrorCallback glfwerrorcallback1 = GLFW.glfwSetErrorCallback(glfwerrorcallback);
        if (glfwerrorcallback1 != null) {
            glfwerrorcallback1.free();
        }

        return s;
    }

    private static void pushClipboard(Window window, ByteBuffer buffer, byte[] bytes) {
        buffer.clear();
        buffer.put(bytes);
        buffer.put((byte)0);
        buffer.flip();
        GLFW.glfwSetClipboardString(window.handle(), buffer);
    }

    public void setClipboard(Window window, String clipboard) {
        byte[] abyte = clipboard.getBytes(Charsets.UTF_8);
        int i = abyte.length + 1;
        if (i < this.clipboardScratchBuffer.capacity()) {
            pushClipboard(window, this.clipboardScratchBuffer, abyte);
        } else {
            ByteBuffer bytebuffer = MemoryUtil.memAlloc(i);

            try {
                pushClipboard(window, bytebuffer, abyte);
            } finally {
                MemoryUtil.memFree(bytebuffer);
            }
        }
    }
}
