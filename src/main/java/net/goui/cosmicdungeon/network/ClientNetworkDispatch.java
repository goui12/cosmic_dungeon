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
 * - Dedicated server jar never tries to load client classes
 * - Fail closed + log once if something is missing/miswired
 */
public final class ClientNetworkDispatch {
    private static final Logger LOGGER = LoggerFactory.getLogger("CosmicDungeon|NetDispatch");

    // Must match your actual client class
    private static final String CLIENT_IMPL = "net.goui.cosmicdungeon.client.network.ModNetworkClient";

    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> LOG_ONCE = new ConcurrentHashMap<>();

    private ClientNetworkDispatch() {}

    public static void dispatch(String methodName, Object payload) {
        if (!isClient()) return;
        if (methodName == null || methodName.isBlank()) return;

        try {
            Method m = METHOD_CACHE.computeIfAbsent("dispatch:" + methodName, k -> findDispatchMethod(methodName));
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
            // NeoForge: FMLEnvironment exposes getDist(), not a public dist field.
            return FMLEnvironment.getDist() == Dist.CLIENT;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findDispatchMethod(String methodName) {
        try {
            Class<?> cls = Class.forName(CLIENT_IMPL, false, ClientNetworkDispatch.class.getClassLoader());

            // Expect: public static void <methodName>(<SomePayloadType>)
            for (Method m : cls.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (!Modifier.isStatic(m.getModifiers())) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1) continue;

                m.setAccessible(true);
                return m;
            }

            logOnce("missingMethod:" + methodName,
                    new NoSuchMethodException(CLIENT_IMPL + "#" + methodName + "(payload)"));
            return null;

        } catch (Throwable t) {
            logOnce("loadClientImplFor:" + methodName, t);
            return null;
        }
    }

    private static Method findSendToServerMethod(String ignoredKey) {
        try {
            Class<?> cls = Class.forName(CLIENT_IMPL, false, ClientNetworkDispatch.class.getClassLoader());
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
