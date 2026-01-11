package net.goui.cosmicdungeon.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Common-safe bridge to client-only network code.
 *
 * Goals:
 * - Zero client-only imports in common sources
 * - Dedicated server never tries to load client classes
 * - Fail closed + log once if something is missing/miswired
 */
public final class ClientNetworkDispatch {
    private static final Logger LOGGER = LoggerFactory.getLogger("CosmicDungeon|NetDispatch");

    // MUST match the actual class path present in the client jar.
    private static final String CLIENT_IMPL = "net.goui.cosmicdungeon.client.ModNetworkClient";

    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> LOG_ONCE = new ConcurrentHashMap<>();

    private static volatile Class<?> CACHED_CLIENT_CLASS;

    private ClientNetworkDispatch() {}

    public static void dispatch(String methodName, Object payload) {
        if (!isClient()) return;
        if (methodName == null || methodName.isBlank()) return;
        if (payload == null) return;

        try {
            String cacheKey = "dispatch:" + methodName + ":" + payload.getClass().getName();
            Method m = METHOD_CACHE.computeIfAbsent(cacheKey, k -> findDispatchMethod(methodName, payload.getClass()));
            if (m == null) return;
            m.invoke(null, payload);
        } catch (Throwable t) {
            logOnce("dispatchInvoke:" + methodName, t);
        }
    }

    public static void sendToServer(CustomPacketPayload payload) {
        if (!isClient()) return;
        if (payload == null) return;

        try {
            Method m = METHOD_CACHE.computeIfAbsent("sendToServer", ClientNetworkDispatch::findSendToServerMethod);
            if (m == null) return;
            m.invoke(null, payload);
        } catch (Throwable t) {
            logOnce("sendToServerInvoke", t);
        }
    }

    private static boolean isClient() {
        try {
            return FMLEnvironment.getDist() == Dist.CLIENT;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> loadClientImplClass() throws ClassNotFoundException {
        Class<?> cached = CACHED_CLIENT_CLASS;
        if (cached != null) return cached;

        ClassNotFoundException last = null;

        // 1) Context class loader (often correct under module classloading)
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        if (ctx != null) {
            try {
                Class<?> c = Class.forName(CLIENT_IMPL, false, ctx);
                CACHED_CLIENT_CLASS = c;
                return c;
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }

        // 2) This class's loader
        ClassLoader own = ClientNetworkDispatch.class.getClassLoader();
        if (own != null) {
            try {
                Class<?> c = Class.forName(CLIENT_IMPL, false, own);
                CACHED_CLIENT_CLASS = c;
                return c;
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }

        // 3) Default loader
        try {
            Class<?> c = Class.forName(CLIENT_IMPL);
            CACHED_CLIENT_CLASS = c;
            return c;
        } catch (ClassNotFoundException e) {
            last = e;
        }

        throw last;
    }

    private static Method findDispatchMethod(String methodName, Class<?> payloadClass) {
        try {
            Class<?> cls = loadClientImplClass();

            Method best = null;

            for (Method m : cls.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (!Modifier.isStatic(m.getModifiers())) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1) continue;

                // Must be compatible with the actual payload type being passed.
                if (!p[0].isAssignableFrom(payloadClass)) continue;

                // Prefer the most specific parameter type (closest to payloadClass)
                if (best == null || best.getParameterTypes()[0].isAssignableFrom(p[0])) {
                    best = m;
                }
            }

            if (best == null) {
                logOnce("missingMethod:" + methodName + ":" + payloadClass.getName(),
                        new NoSuchMethodException(CLIENT_IMPL + "#" + methodName + "(" + payloadClass.getName() + ")"));
                return null;
            }

            best.setAccessible(true);
            return best;

        } catch (Throwable t) {
            logOnce("loadClientImplFor:" + methodName, t);
            return null;
        }
    }

    private static Method findSendToServerMethod(String ignoredKey) {
        try {
            Class<?> cls = loadClientImplClass();
            Method m = cls.getDeclaredMethod("sendToServer", CustomPacketPayload.class);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            logOnce("missingSendToServer", t);
            return null;
        }
    }

    private static void logOnce(String key, Throwable t) {
        AtomicBoolean flag = LOG_ONCE.computeIfAbsent(key, k -> new AtomicBoolean(false));
        if (flag.compareAndSet(false, true)) {
            LOGGER.error("[net] Client dispatch failure ({}). This is safe but indicates a wiring problem.", key, t);
        }
    }
}
