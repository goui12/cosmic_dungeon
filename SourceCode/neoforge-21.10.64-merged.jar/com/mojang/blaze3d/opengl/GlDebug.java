package com.mojang.blaze3d.opengl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlDebug {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CIRCULAR_LOG_SIZE = 10;
    private final Queue<GlDebug.LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
    @Nullable
    private volatile GlDebug.LogEntry lastEntry;
    private static final List<Integer> DEBUG_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
    private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of(37190, 37191, 37192);

    private static String printUnknownToken(int token) {
        return "Unknown (0x" + Integer.toHexString(token).toUpperCase() + ")";
    }

    public static String sourceToString(int source) {
        switch (source) {
            case 33350:
                return "API";
            case 33351:
                return "WINDOW SYSTEM";
            case 33352:
                return "SHADER COMPILER";
            case 33353:
                return "THIRD PARTY";
            case 33354:
                return "APPLICATION";
            case 33355:
                return "OTHER";
            default:
                return printUnknownToken(source);
        }
    }

    public static String typeToString(int type) {
        switch (type) {
            case 33356:
                return "ERROR";
            case 33357:
                return "DEPRECATED BEHAVIOR";
            case 33358:
                return "UNDEFINED BEHAVIOR";
            case 33359:
                return "PORTABILITY";
            case 33360:
                return "PERFORMANCE";
            case 33361:
                return "OTHER";
            case 33384:
                return "MARKER";
            default:
                return printUnknownToken(type);
        }
    }

    public static String severityToString(int type) {
        switch (type) {
            case 33387:
                return "NOTIFICATION";
            case 37190:
                return "HIGH";
            case 37191:
                return "MEDIUM";
            case 37192:
                return "LOW";
            default:
                return printUnknownToken(type);
        }
    }

    private void printDebugLog(int source, int type, int id, int severity, int length, long message, long userProgram) {
        String s = GLDebugMessageCallback.getMessage(length, message);
        GlDebug.LogEntry gldebug$logentry;
        synchronized (this.MESSAGE_BUFFER) {
            gldebug$logentry = this.lastEntry;
            if (gldebug$logentry != null && gldebug$logentry.isSame(source, type, id, severity, s)) {
                gldebug$logentry.count++;
            } else {
                gldebug$logentry = new GlDebug.LogEntry(source, type, id, severity, s);
                this.MESSAGE_BUFFER.add(gldebug$logentry);
                this.lastEntry = gldebug$logentry;
            }
        }

        LOGGER.info("OpenGL debug message: {}", gldebug$logentry);
    }

    public List<String> getLastOpenGlDebugMessages() {
        synchronized (this.MESSAGE_BUFFER) {
            List<String> list = Lists.newArrayListWithCapacity(this.MESSAGE_BUFFER.size());

            for (GlDebug.LogEntry gldebug$logentry : this.MESSAGE_BUFFER) {
                list.add(gldebug$logentry + " x " + gldebug$logentry.count);
            }

            return list;
        }
    }

    @Nullable
    public static GlDebug enableDebugCallback(int verbosity, boolean synchronous, Set<String> enabledExtensions) {
        if (verbosity <= 0) {
            return null;
        } else {
            GLCapabilities glcapabilities = GL.getCapabilities();
            if (glcapabilities.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
                GlDebug gldebug1 = new GlDebug();
                enabledExtensions.add("GL_KHR_debug");
                GL11.glEnable(37600);
                if (synchronous) {
                    GL11.glEnable(33346);
                }

                for (int j = 0; j < DEBUG_LEVELS.size(); j++) {
                    boolean flag1 = j < verbosity;
                    KHRDebug.glDebugMessageControl(4352, 4352, DEBUG_LEVELS.get(j), (int[])null, flag1);
                }

                KHRDebug.glDebugMessageCallback(GLX.make(GLDebugMessageCallback.create(gldebug1::printDebugLog), DebugMemoryUntracker::untrack), 0L);
                return gldebug1;
            } else if (glcapabilities.GL_ARB_debug_output && GlDevice.USE_GL_ARB_debug_output) {
                GlDebug gldebug = new GlDebug();
                enabledExtensions.add("GL_ARB_debug_output");
                if (synchronous) {
                    GL11.glEnable(33346);
                }

                for (int i = 0; i < DEBUG_LEVELS_ARB.size(); i++) {
                    boolean flag = i < verbosity;
                    ARBDebugOutput.glDebugMessageControlARB(4352, 4352, DEBUG_LEVELS_ARB.get(i), (int[])null, flag);
                }

                ARBDebugOutput.glDebugMessageCallbackARB(GLX.make(GLDebugMessageARBCallback.create(gldebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
                return gldebug;
            } else {
                return null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class LogEntry {
        private final int id;
        private final int source;
        private final int type;
        private final int severity;
        private final String message;
        int count = 1;

        LogEntry(int source, int type, int id, int severity, String message) {
            this.id = id;
            this.source = source;
            this.type = type;
            this.severity = severity;
            this.message = message;
        }

        boolean isSame(int source, int type, int id, int severity, String message) {
            return type == this.type && source == this.source && id == this.id && severity == this.severity && message.equals(this.message);
        }

        @Override
        public String toString() {
            return "id="
                + this.id
                + ", source="
                + GlDebug.sourceToString(this.source)
                + ", type="
                + GlDebug.typeToString(this.type)
                + ", severity="
                + GlDebug.severityToString(this.severity)
                + ", message='"
                + this.message
                + "'";
        }
    }
}
