package net.minecraft.client.quickplay;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class QuickPlay {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component ERROR_TITLE = Component.translatable("quickplay.error.title");
    private static final Component INVALID_IDENTIFIER = Component.translatable("quickplay.error.invalid_identifier");
    private static final Component REALM_CONNECT = Component.translatable("quickplay.error.realm_connect");
    private static final Component REALM_PERMISSION = Component.translatable("quickplay.error.realm_permission");
    private static final Component TO_TITLE = Component.translatable("gui.toTitle");
    private static final Component TO_WORLD_LIST = Component.translatable("gui.toWorld");
    private static final Component TO_REALMS_LIST = Component.translatable("gui.toRealms");

    public static void connect(Minecraft minecraft, GameConfig.QuickPlayVariant variant, RealmsClient client) {
        if (!variant.isEnabled()) {
            LOGGER.error("Quick play disabled");
            minecraft.setScreen(new TitleScreen());
        } else {
            switch (variant) {
                case GameConfig.QuickPlayMultiplayerData gameconfig$quickplaymultiplayerdata:
                    joinMultiplayerWorld(minecraft, gameconfig$quickplaymultiplayerdata.serverAddress());
                    break;
                case GameConfig.QuickPlayRealmsData gameconfig$quickplayrealmsdata:
                    joinRealmsWorld(minecraft, client, gameconfig$quickplayrealmsdata.realmId());
                    break;
                case GameConfig.QuickPlaySinglePlayerData gameconfig$quickplaysingleplayerdata:
                    String s = gameconfig$quickplaysingleplayerdata.worldId();
                    if (StringUtil.isBlank(s)) {
                        s = getLatestSingleplayerWorld(minecraft.getLevelSource());
                    }

                    joinSingleplayerWorld(minecraft, s);
                    break;
                case GameConfig.QuickPlayDisabled gameconfig$quickplaydisabled:
                    LOGGER.error("Quick play disabled");
                    minecraft.setScreen(new TitleScreen());
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }
    }

    @Nullable
    private static String getLatestSingleplayerWorld(LevelStorageSource levelSource) {
        try {
            List<LevelSummary> list = levelSource.loadLevelSummaries(levelSource.findLevelCandidates()).get();
            if (list.isEmpty()) {
                LOGGER.warn("no latest singleplayer world found");
                return null;
            } else {
                return list.getFirst().getLevelId();
            }
        } catch (ExecutionException | InterruptedException interruptedexception) {
            LOGGER.error("failed to load singleplayer world summaries", (Throwable)interruptedexception);
            return null;
        }
    }

    private static void joinSingleplayerWorld(Minecraft minecraft, @Nullable String levelName) {
        if (!StringUtil.isBlank(levelName) && minecraft.getLevelSource().levelExists(levelName)) {
            minecraft.createWorldOpenFlows().openWorld(levelName, () -> minecraft.setScreen(new TitleScreen()));
        } else {
            Screen screen = new SelectWorldScreen(new TitleScreen());
            minecraft.setScreen(new DisconnectedScreen(screen, ERROR_TITLE, INVALID_IDENTIFIER, TO_WORLD_LIST));
        }
    }

    private static void joinMultiplayerWorld(Minecraft minecraft, String ip) {
        ServerList serverlist = new ServerList(minecraft);
        serverlist.load();
        ServerData serverdata = serverlist.get(ip);
        if (serverdata == null) {
            serverdata = new ServerData(I18n.get("selectServer.defaultName"), ip, ServerData.Type.OTHER);
            serverlist.add(serverdata, true);
            serverlist.save();
        }

        ServerAddress serveraddress = ServerAddress.parseString(ip);
        ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), minecraft, serveraddress, serverdata, true, null);
    }

    private static void joinRealmsWorld(Minecraft minecraft, RealmsClient realmsClient, String serverId) {
        long i;
        RealmsServerList realmsserverlist;
        try {
            i = Long.parseLong(serverId);
            realmsserverlist = realmsClient.listRealms();
        } catch (NumberFormatException numberformatexception) {
            Screen screen1 = new RealmsMainScreen(new TitleScreen());
            minecraft.setScreen(new DisconnectedScreen(screen1, ERROR_TITLE, INVALID_IDENTIFIER, TO_REALMS_LIST));
            return;
        } catch (RealmsServiceException realmsserviceexception) {
            Screen screen = new TitleScreen();
            minecraft.setScreen(new DisconnectedScreen(screen, ERROR_TITLE, REALM_CONNECT, TO_TITLE));
            return;
        }

        RealmsServer realmsserver = realmsserverlist.servers.stream().filter(p_279424_ -> p_279424_.id == i).findFirst().orElse(null);
        if (realmsserver == null) {
            Screen screen2 = new RealmsMainScreen(new TitleScreen());
            minecraft.setScreen(new DisconnectedScreen(screen2, ERROR_TITLE, REALM_PERMISSION, TO_REALMS_LIST));
        } else {
            TitleScreen titlescreen = new TitleScreen();
            minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(titlescreen, new GetServerDetailsTask(titlescreen, realmsserver)));
        }
    }
}
