package com.mojang.blaze3d.platform.cursor;

import com.mojang.blaze3d.platform.Window;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class CursorType {
    public static final CursorType DEFAULT = new CursorType("default", 0L);
    private final String name;
    private final long handle;

    private CursorType(String name, long handle) {
        this.name = name;
        this.handle = handle;
    }

    public void select(Window window) {
        GLFW.glfwSetCursor(window.handle(), this.handle);
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static CursorType createStandardCursor(int id, String name, CursorType fallback) {
        long i = GLFW.glfwCreateStandardCursor(id);
        return i == 0L ? fallback : new CursorType(name, i);
    }
}
