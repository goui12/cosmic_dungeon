package com.mojang.realmsclient.util;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
    private static final int MINUTES = 60;
    private static final int HOURS = 3600;
    private static final int DAYS = 86400;

    public static Component convertToAgePresentation(long millis) {
        if (millis < 0L) {
            return RIGHT_NOW;
        } else {
            long i = millis / 1000L;
            if (i < 60L) {
                return Component.translatable("mco.time.secondsAgo", i);
            } else if (i < 3600L) {
                long l = i / 60L;
                return Component.translatable("mco.time.minutesAgo", l);
            } else if (i < 86400L) {
                long k = i / 3600L;
                return Component.translatable("mco.time.hoursAgo", k);
            } else {
                long j = i / 86400L;
                return Component.translatable("mco.time.daysAgo", j);
            }
        }
    }

    public static Component convertToAgePresentationFromInstant(Date date) {
        return convertToAgePresentation(System.currentTimeMillis() - date.getTime());
    }

    public static void renderPlayerFace(GuiGraphics guiGraphics, int x, int y, int size, UUID playerUuid) {
        PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = Minecraft.getInstance()
            .playerSkinRenderCache()
            .getOrDefault(ResolvableProfile.createUnresolved(playerUuid));
        PlayerFaceRenderer.draw(guiGraphics, playerskinrendercache$renderinfo.playerSkin(), x, y, size);
    }

    public static <T> CompletableFuture<T> supplyAsync(RealmsUtil.RealmsIoFunction<T> action, @Nullable Consumer<RealmsServiceException> onError) {
        return CompletableFuture.supplyAsync(() -> {
            RealmsClient realmsclient = RealmsClient.getOrCreate();

            try {
                return action.apply(realmsclient);
            } catch (Throwable throwable) {
                if (throwable instanceof RealmsServiceException realmsserviceexception) {
                    if (onError != null) {
                        onError.accept(realmsserviceexception);
                    }
                } else {
                    LOGGER.error("Unhandled exception", throwable);
                }

                throw new RuntimeException(throwable);
            }
        }, Util.nonCriticalIoPool());
    }

    public static CompletableFuture<Void> runAsync(RealmsUtil.RealmsIoConsumer action, @Nullable Consumer<RealmsServiceException> onError) {
        return supplyAsync(action, onError);
    }

    public static Consumer<RealmsServiceException> openScreenOnFailure(Function<RealmsServiceException, Screen> screenSupplier) {
        Minecraft minecraft = Minecraft.getInstance();
        return p_428731_ -> minecraft.execute(() -> minecraft.setScreen(screenSupplier.apply(p_428731_)));
    }

    public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(Function<RealmsServiceException, Screen> screenSupplier, String errorMessage) {
        return openScreenOnFailure(screenSupplier).andThen(p_428690_ -> LOGGER.error(errorMessage, (Throwable)p_428690_));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface RealmsIoConsumer extends RealmsUtil.RealmsIoFunction<Void> {
        void accept(RealmsClient client) throws RealmsServiceException;

        default Void apply(RealmsClient client) throws RealmsServiceException {
            this.accept(client);
            return null;
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface RealmsIoFunction<T> {
        T apply(RealmsClient client) throws RealmsServiceException;
    }
}
