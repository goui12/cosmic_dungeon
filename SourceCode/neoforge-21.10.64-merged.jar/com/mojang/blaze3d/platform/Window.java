package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class Window implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int BASE_WIDTH = 320;
    public static final int BASE_HEIGHT = 240;
    private final GLFWErrorCallback defaultErrorCallback = GLFWErrorCallback.create(this::defaultErrorCallback);
    private final WindowEventHandler eventHandler;
    private final ScreenManager screenManager;
    private final long handle;
    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;
    private Optional<VideoMode> preferredFullscreenVideoMode;
    private boolean fullscreen;
    private boolean actuallyFullscreen;
    private int x;
    private int y;
    private int width;
    private int height;
    private int framebufferWidth;
    private int framebufferHeight;
    private int guiScaledWidth;
    private int guiScaledHeight;
    private int guiScale;
    private String errorSection = "";
    private boolean dirty;
    private boolean vsync;
    private boolean iconified;
    private boolean minimized;
    private boolean allowCursorChanges;
    private CursorType currentCursor = CursorType.DEFAULT;

    public Window(WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, @Nullable String preferredFullscreenVideoMode, String title) {
        this.screenManager = screenManager;
        this.setBootErrorCallback();
        this.setErrorSection("Pre startup");
        this.eventHandler = eventHandler;
        Optional<VideoMode> optional = VideoMode.read(preferredFullscreenVideoMode);
        if (optional.isPresent()) {
            this.preferredFullscreenVideoMode = optional;
        } else if (displayData.fullscreenWidth().isPresent() && displayData.fullscreenHeight().isPresent()) {
            this.preferredFullscreenVideoMode = Optional.of(
                new VideoMode(displayData.fullscreenWidth().getAsInt(), displayData.fullscreenHeight().getAsInt(), 8, 8, 8, 60)
            );
        } else {
            this.preferredFullscreenVideoMode = Optional.empty();
        }

        this.actuallyFullscreen = this.fullscreen = displayData.isFullscreen();
        Monitor monitor = screenManager.getMonitor(GLFW.glfwGetPrimaryMonitor());
        this.windowedWidth = this.width = Math.max(displayData.width(), 1);
        this.windowedHeight = this.height = Math.max(displayData.height(), 1);
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(139265, 196609);
        GLFW.glfwWindowHint(139275, 221185);
        GLFW.glfwWindowHint(139266, 3);
        GLFW.glfwWindowHint(139267, 3);
        GLFW.glfwWindowHint(139272, 204801);
        GLFW.glfwWindowHint(139270, 1);
        var earlyLoadingScreen = net.neoforged.fml.loading.EarlyLoadingScreenController.current();
        if (earlyLoadingScreen != null) {
            this.handle = takeOverWindow(earlyLoadingScreen, title);
        } else {
        this.handle = GLFW.glfwCreateWindow(this.width, this.height, title, this.fullscreen && monitor != null ? monitor.getMonitor() : 0L, 0L);
        if (monitor != null) {
            VideoMode videomode = monitor.getPreferredVidMode(this.fullscreen ? this.preferredFullscreenVideoMode : Optional.empty());
            this.windowedX = this.x = monitor.getX() + videomode.getWidth() / 2 - this.width / 2;
            this.windowedY = this.y = monitor.getY() + videomode.getHeight() / 2 - this.height / 2;
        } else {
            int[] aint1 = new int[1];
            int[] aint = new int[1];
            GLFW.glfwGetWindowPos(this.handle, aint1, aint);
            this.windowedX = this.x = aint1[0];
            this.windowedY = this.y = aint[0];
        }
        }

        this.setMode();
        this.refreshFramebufferSize();
        GLFW.glfwSetFramebufferSizeCallback(this.handle, this::onFramebufferResize);
        GLFW.glfwSetWindowPosCallback(this.handle, this::onMove);
        GLFW.glfwSetWindowSizeCallback(this.handle, this::onResize);
        GLFW.glfwSetWindowFocusCallback(this.handle, this::onFocus);
        GLFW.glfwSetCursorEnterCallback(this.handle, this::onEnter);
        GLFW.glfwSetWindowIconifyCallback(this.handle, this::onIconify);
    }

    public static String getPlatform() {
        int i = GLFW.glfwGetPlatform();

        return switch (i) {
            case 0 -> "<error>";
            case 393217 -> "win32";
            case 393218 -> "cocoa";
            case 393219 -> "wayland";
            case 393220 -> "x11";
            case 393221 -> "null";
            default -> String.format(Locale.ROOT, "unknown (%08X)", i);
        };
    }

    public int getRefreshRate() {
        RenderSystem.assertOnRenderThread();
        return GLX._getRefreshRate(this);
    }

    public boolean shouldClose() {
        return GLX._shouldClose(this);
    }

    public static void checkGlfwError(BiConsumer<Integer, String> errorConsumer) {
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            int i = GLFW.glfwGetError(pointerbuffer);
            if (i != 0) {
                long j = pointerbuffer.get();
                String s = j == 0L ? "" : MemoryUtil.memUTF8(j);
                errorConsumer.accept(i, s);
            }
        }
    }

    public void setIcon(PackResources packResources, IconSet iconSet) throws IOException {
        int i = GLFW.glfwGetPlatform();
        switch (i) {
            case 393217:
            case 393220:
                List<IoSupplier<InputStream>> list = iconSet.getStandardIcons(packResources);
                List<ByteBuffer> list1 = new ArrayList<>(list.size());

                try (MemoryStack memorystack = MemoryStack.stackPush()) {
                    Buffer buffer = GLFWImage.malloc(list.size(), memorystack);

                    for (int j = 0; j < list.size(); j++) {
                        try (NativeImage nativeimage = NativeImage.read(list.get(j).get())) {
                            ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeimage.getWidth() * nativeimage.getHeight() * 4);
                            list1.add(bytebuffer);
                            bytebuffer.asIntBuffer().put(nativeimage.getPixelsABGR());
                            buffer.position(j);
                            buffer.width(nativeimage.getWidth());
                            buffer.height(nativeimage.getHeight());
                            buffer.pixels(bytebuffer);
                        }
                    }

                    GLFW.glfwSetWindowIcon(this.handle, buffer.position(0));
                    break;
                } finally {
                    list1.forEach(MemoryUtil::memFree);
                }
            case 393218:
                MacosUtil.loadIcon(iconSet.getMacIcon(packResources));
            case 393219:
            case 393221:
                break;
            default:
                LOGGER.warn("Not setting icon for unrecognized platform: {}", i);
        }
    }

    public void setErrorSection(String errorSection) {
        this.errorSection = errorSection;
    }

    private void setBootErrorCallback() {
        GLFW.glfwSetErrorCallback(Window::bootCrash);
    }

    private static void bootCrash(int error, long description) {
        String s = "GLFW error " + error + ": " + MemoryUtil.memUTF8(description);
        TinyFileDialogs.tinyfd_messageBox(
            "Minecraft", s + ".\n\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).", "ok", "error", false
        );
        throw new Window.WindowInitFailed(s);
    }

    public void defaultErrorCallback(int error, long description) {
        String s = MemoryUtil.memUTF8(description);
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Encountered GL error off-thread @ " + errorSection + ": " + error + ": " + s);
        }
        LOGGER.error("########## GL ERROR ##########");
        LOGGER.error("@ {}", this.errorSection);
        LOGGER.error("{}: {}", error, s);
    }

    public void setDefaultErrorCallback() {
        GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback(this.defaultErrorCallback);
        if (glfwerrorcallback != null) {
            glfwerrorcallback.free();
        }
    }

    public void updateVsync(boolean vsync) {
        RenderSystem.assertOnRenderThread();
        this.vsync = vsync;
        GLFW.glfwSwapInterval(vsync ? 1 : 0);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        Callbacks.glfwFreeCallbacks(this.handle);
        this.defaultErrorCallback.close();
        GLFW.glfwDestroyWindow(this.handle);
        GLFW.glfwTerminate();
    }

    private void onMove(long window, int x, int y) {
        this.x = x;
        this.y = y;
    }

    private void onFramebufferResize(long window, int framebufferWidth, int framebufferHeight) {
        if (window == this.handle) {
            int i = this.getWidth();
            int j = this.getHeight();
            if (framebufferWidth != 0 && framebufferHeight != 0) {
                this.minimized = false;
                this.framebufferWidth = framebufferWidth;
                this.framebufferHeight = framebufferHeight;
                if (this.getWidth() != i || this.getHeight() != j) {
                    try {
                        this.eventHandler.resizeDisplay();
                    } catch (Exception exception) {
                        CrashReport crashreport = CrashReport.forThrowable(exception, "Window resize");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Window Dimensions");
                        crashreportcategory.setDetail("Old", i + "x" + j);
                        crashreportcategory.setDetail("New", framebufferWidth + "x" + framebufferHeight);
                        throw new ReportedException(crashreport);
                    }
                }
            } else {
                this.minimized = true;
            }
        }
    }

    private void refreshFramebufferSize() {
        int[] aint = new int[1];
        int[] aint1 = new int[1];
        GLFW.glfwGetFramebufferSize(this.handle, aint, aint1);
        this.framebufferWidth = aint[0] > 0 ? aint[0] : 1;
        this.framebufferHeight = aint1[0] > 0 ? aint1[0] : 1;
    }

    private void onResize(long window, int width, int height) {
        this.width = width;
        this.height = height;
    }

    private void onFocus(long window, boolean hasFocus) {
        if (window == this.handle) {
            this.eventHandler.setWindowActive(hasFocus);
        }
    }

    /**
     * @param cursorEntered {@code true} if the cursor entered the window, {@code
     *                      false} if the cursor left
     */
    private void onEnter(long window, boolean cursorEntered) {
        if (cursorEntered) {
            this.eventHandler.cursorEntered();
        }
    }

    private void onIconify(long window, boolean iconified) {
        this.iconified = iconified;
    }

    public void updateDisplay(@Nullable TracyFrameCapture tracyFrameCapture) {
        RenderSystem.flipFrame(this, tracyFrameCapture);
        if (this.fullscreen != this.actuallyFullscreen) {
            this.actuallyFullscreen = this.fullscreen;
            this.updateFullscreen(this.vsync, tracyFrameCapture);
        }
    }

    public Optional<VideoMode> getPreferredFullscreenVideoMode() {
        return this.preferredFullscreenVideoMode;
    }

    public void setPreferredFullscreenVideoMode(Optional<VideoMode> preferredFullscreenVideoMode) {
        boolean flag = !preferredFullscreenVideoMode.equals(this.preferredFullscreenVideoMode);
        this.preferredFullscreenVideoMode = preferredFullscreenVideoMode;
        if (flag) {
            this.dirty = true;
        }
    }

    public void changeFullscreenVideoMode() {
        if (this.fullscreen && this.dirty) {
            this.dirty = false;
            this.setMode();
            this.eventHandler.resizeDisplay();
        }
    }

    private void setMode() {
        boolean flag = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
        if (this.fullscreen) {
            Monitor monitor = this.screenManager.findBestMonitor(this);
            if (monitor == null) {
                LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
                this.fullscreen = false;
            } else {
                if (MacosUtil.IS_MACOS) {
                    MacosUtil.exitNativeFullscreen(this);
                }

                VideoMode videomode = monitor.getPreferredVidMode(this.preferredFullscreenVideoMode);
                if (!flag) {
                    this.windowedX = this.x;
                    this.windowedY = this.y;
                    this.windowedWidth = this.width;
                    this.windowedHeight = this.height;
                }

                this.x = 0;
                this.y = 0;
                this.width = videomode.getWidth();
                this.height = videomode.getHeight();
                GLFW.glfwSetWindowMonitor(this.handle, monitor.getMonitor(), this.x, this.y, this.width, this.height, videomode.getRefreshRate());
                if (MacosUtil.IS_MACOS) {
                    MacosUtil.clearResizableBit(this);
                }
            }
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;
            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
        }
    }

    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
    }

    public void setWindowed(int windowedWidth, int windowedHeight) {
        this.windowedWidth = windowedWidth;
        this.windowedHeight = windowedHeight;
        this.fullscreen = false;
        this.setMode();
    }

    private void updateFullscreen(boolean vsyncEnabled, @Nullable TracyFrameCapture tracyFrameCapture) {
        RenderSystem.assertOnRenderThread();

        try {
            this.setMode();
            this.eventHandler.resizeDisplay();
            this.updateVsync(vsyncEnabled);
            this.updateDisplay(tracyFrameCapture);
        } catch (Exception exception) {
            LOGGER.error("Couldn't toggle fullscreen", (Throwable)exception);
        }
    }

    public int calculateScale(int guiScale, boolean forceUnicode) {
        int i = 1;

        while (
            i != guiScale
                && i < this.framebufferWidth
                && i < this.framebufferHeight
                && this.framebufferWidth / (i + 1) >= 320
                && this.framebufferHeight / (i + 1) >= 240
        ) {
            i++;
        }

        if (forceUnicode && i % 2 != 0) {
            i++;
        }

        return i;
    }

    public void setGuiScale(int guiScale) {
        this.guiScale = guiScale;
        double d0 = guiScale;
        int i = (int)(this.framebufferWidth / d0);
        this.guiScaledWidth = this.framebufferWidth / d0 > i ? i + 1 : i;
        int j = (int)(this.framebufferHeight / d0);
        this.guiScaledHeight = this.framebufferHeight / d0 > j ? j + 1 : j;
    }

    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(this.handle, title);
    }

    public long handle() {
        return this.handle;
    }

    public boolean isFullscreen() {
        return this.fullscreen;
    }

    public boolean isIconified() {
        return this.iconified;
    }

    public int getWidth() {
        return this.framebufferWidth;
    }

    public int getHeight() {
        return this.framebufferHeight;
    }

    public void setWidth(int framebufferWidth) {
        this.framebufferWidth = framebufferWidth;
    }

    public void setHeight(int framebufferHeight) {
        this.framebufferHeight = framebufferHeight;
    }

    public int getScreenWidth() {
        return this.width;
    }

    public int getScreenHeight() {
        return this.height;
    }

    public int getGuiScaledWidth() {
        return this.guiScaledWidth;
    }

    public int getGuiScaledHeight() {
        return this.guiScaledHeight;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getGuiScale() {
        return this.guiScale;
    }

    @Nullable
    public Monitor findBestMonitor() {
        return this.screenManager.findBestMonitor(this);
    }

    public void updateRawMouseInput(boolean enableRawMouseMotion) {
        InputConstants.updateRawMouseInput(this, enableRawMouseMotion);
    }

    public void setWindowCloseCallback(Runnable windowCloseCallback) {
        GLFWWindowCloseCallback glfwwindowclosecallback = GLFW.glfwSetWindowCloseCallback(this.handle, p_365115_ -> windowCloseCallback.run());
        if (glfwwindowclosecallback != null) {
            glfwwindowclosecallback.free();
        }
    }

    public boolean isMinimized() {
        return this.minimized;
    }

    public void setAllowCursorChanges(boolean allowCursorChanges) {
        this.allowCursorChanges = allowCursorChanges;
    }

    public void selectCursor(CursorType cursor) {
        CursorType cursortype = this.allowCursorChanges ? cursor : CursorType.DEFAULT;
        if (this.currentCursor != cursortype) {
            this.currentCursor = cursortype;
            cursortype.select(this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WindowInitFailed extends SilentInitException {
        WindowInitFailed(String p_85455_) {
            super(p_85455_);
        }
    }

    // Neo take over window and its properties from early display
    private long takeOverWindow(net.neoforged.fml.loading.EarlyLoadingScreenController earlyLoadingScreen, String title) {
        long window = earlyLoadingScreen.takeOverGlfwWindow();

        GLFW.glfwSetWindowTitle(window, title);

        var x = new int[1];
        var y = new int[1];
        GLFW.glfwGetWindowPos(window, x, y);
        this.x = this.windowedX = x[0];
        this.y = this.windowedY = y[0];

        var width = new int[1];
        var height = new int[1];
        GLFW.glfwGetWindowSize(window, width, height);
        // The window height and width can be 0 if minimized
        this.width = this.windowedWidth = Math.max(width[0], 1);
        this.height = this.windowedHeight = Math.max(height[0], 1);

        return window;
    }
}
