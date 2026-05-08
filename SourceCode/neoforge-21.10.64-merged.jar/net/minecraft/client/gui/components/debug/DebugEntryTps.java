package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.Connection;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryTps implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_433345_, @Nullable Level p_434611_, @Nullable LevelChunk p_433576_, @Nullable LevelChunk p_434015_) {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer integratedserver = minecraft.getSingleplayerServer();
        ClientPacketListener clientpacketlistener = minecraft.getConnection();
        if (clientpacketlistener != null && p_434611_ != null) {
            Connection connection = clientpacketlistener.getConnection();
            float f = connection.getAverageSentPackets();
            float f1 = connection.getAverageReceivedPackets();
            TickRateManager tickratemanager = p_434611_.tickRateManager();
            String s;
            if (tickratemanager.isSteppingForward()) {
                s = " (frozen - stepping)";
            } else if (tickratemanager.isFrozen()) {
                s = " (frozen)";
            } else {
                s = "";
            }

            String s1;
            if (integratedserver != null) {
                ServerTickRateManager servertickratemanager = integratedserver.tickRateManager();
                boolean flag = servertickratemanager.isSprinting();
                if (flag) {
                    s = " (sprinting)";
                }

                String s2 = flag ? "-" : String.format(Locale.ROOT, "%.1f", tickratemanager.millisecondsPerTick());
                s1 = String.format(
                    Locale.ROOT, "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx", integratedserver.getCurrentSmoothedTickTime(), s2, s, f, f1
                );
            } else {
                s1 = String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", clientpacketlistener.serverBrand(), s, f, f1);
            }

            p_433345_.addLine(s1);
        }
    }

    @Override
    public boolean isAllowed(boolean p_435085_) {
        return true;
    }
}
