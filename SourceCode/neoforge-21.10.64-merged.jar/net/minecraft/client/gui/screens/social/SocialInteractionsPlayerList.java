package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
    private final SocialInteractionsScreen socialInteractionsScreen;
    private final List<PlayerEntry> players = Lists.newArrayList();
    @Nullable
    private String filter;

    public SocialInteractionsPlayerList(SocialInteractionsScreen socialInteractionsScreen, Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.socialInteractionsScreen = socialInteractionsScreen;
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    @Override
    protected void enableScissor(GuiGraphics guiGraphics) {
        guiGraphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
    }

    public void updatePlayerList(Collection<UUID> ids, double scrollAmount, boolean addChatLogPlayers) {
        Map<UUID, PlayerEntry> map = new HashMap<>();
        this.addOnlinePlayers(ids, map);
        if (addChatLogPlayers) {
            this.addSeenPlayers(map);
        }

        this.updatePlayersFromChatLog(map, addChatLogPlayers);
        this.updateFiltersAndScroll(map.values(), scrollAmount);
    }

    private void addOnlinePlayers(Collection<UUID> ids, Map<UUID, PlayerEntry> playerMap) {
        ClientPacketListener clientpacketlistener = this.minecraft.player.connection;

        for (UUID uuid : ids) {
            PlayerInfo playerinfo = clientpacketlistener.getPlayerInfo(uuid);
            if (playerinfo != null) {
                PlayerEntry playerentry = this.makePlayerEntry(uuid, playerinfo);
                playerMap.put(uuid, playerentry);
            }
        }
    }

    private void addSeenPlayers(Map<UUID, PlayerEntry> players) {
        Map<UUID, PlayerInfo> map = this.minecraft.player.connection.getSeenPlayers();

        for (Map.Entry<UUID, PlayerInfo> entry : map.entrySet()) {
            players.computeIfAbsent(entry.getKey(), p_437162_ -> {
                PlayerEntry playerentry = this.makePlayerEntry(p_437162_, entry.getValue());
                playerentry.setRemoved(true);
                return playerentry;
            });
        }
    }

    private PlayerEntry makePlayerEntry(UUID uuid, PlayerInfo playerInfo) {
        return new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, uuid, playerInfo.getProfile().name(), playerInfo::getSkin, playerInfo.hasVerifiableChat()
        );
    }

    private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> playerMap, boolean addPlayers) {
        Map<UUID, GameProfile> map = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
        map.forEach(
            (p_442318_, p_442319_) -> {
                PlayerEntry playerentry;
                if (addPlayers) {
                    playerentry = playerMap.computeIfAbsent(
                        p_442318_,
                        p_442315_ -> {
                            PlayerEntry playerentry1 = new PlayerEntry(
                                this.minecraft,
                                this.socialInteractionsScreen,
                                p_442319_.id(),
                                p_442319_.name(),
                                this.minecraft.getSkinManager().createLookup(p_442319_, true),
                                true
                            );
                            playerentry1.setRemoved(true);
                            return playerentry1;
                        }
                    );
                } else {
                    playerentry = playerMap.get(p_442318_);
                    if (playerentry == null) {
                        return;
                    }
                }

                playerentry.setHasRecentMessages(true);
            }
        );
    }

    private static Map<UUID, GameProfile> collectProfilesFromChatLog(ChatLog chatLog) {
        Map<UUID, GameProfile> map = new Object2ObjectLinkedOpenHashMap<>();

        for (int i = chatLog.end(); i >= chatLog.start(); i--) {
            if (chatLog.lookup(i) instanceof LoggedChatMessage.Player loggedchatmessage$player && loggedchatmessage$player.message().hasSignature()) {
                map.put(loggedchatmessage$player.profileId(), loggedchatmessage$player.profile());
            }
        }

        return map;
    }

    private void sortPlayerEntries() {
        this.players.sort(Comparator.<PlayerEntry, Integer>comparing(p_240744_ -> {
            if (this.minecraft.isLocalPlayer(p_240744_.getPlayerId())) {
                return 0;
            } else if (this.minecraft.getReportingContext().hasDraftReportFor(p_240744_.getPlayerId())) {
                return 1;
            } else if (p_240744_.getPlayerId().version() == 2) {
                return 4;
            } else {
                return p_240744_.hasRecentMessages() ? 2 : 3;
            }
        }).thenComparing(p_240745_ -> {
            if (!p_240745_.getPlayerName().isBlank()) {
                int i = p_240745_.getPlayerName().codePointAt(0);
                if (i == 95 || i >= 97 && i <= 122 || i >= 65 && i <= 90 || i >= 48 && i <= 57) {
                    return 0;
                }
            }

            return 1;
        }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
    }

    private void updateFiltersAndScroll(Collection<PlayerEntry> players, double scrollAmount) {
        this.players.clear();
        this.players.addAll(players);
        this.sortPlayerEntries();
        this.updateFilteredPlayers();
        this.replaceEntries(this.players);
        this.setScrollAmount(scrollAmount);
    }

    private void updateFilteredPlayers() {
        if (this.filter != null) {
            this.players.removeIf(p_100710_ -> !p_100710_.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
            this.replaceEntries(this.players);
        }
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    public void addPlayer(PlayerInfo playerInfo, SocialInteractionsScreen.Page page) {
        UUID uuid = playerInfo.getProfile().id();

        for (PlayerEntry playerentry : this.players) {
            if (playerentry.getPlayerId().equals(uuid)) {
                playerentry.setRemoved(false);
                return;
            }
        }

        if ((page == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uuid))
            && (Strings.isNullOrEmpty(this.filter) || playerInfo.getProfile().name().toLowerCase(Locale.ROOT).contains(this.filter))) {
            boolean flag = playerInfo.hasVerifiableChat();
            PlayerEntry playerentry1 = new PlayerEntry(
                this.minecraft, this.socialInteractionsScreen, playerInfo.getProfile().id(), playerInfo.getProfile().name(), playerInfo::getSkin, flag
            );
            this.addEntry(playerentry1);
            this.players.add(playerentry1);
        }
    }

    public void removePlayer(UUID id) {
        for (PlayerEntry playerentry : this.players) {
            if (playerentry.getPlayerId().equals(id)) {
                playerentry.setRemoved(true);
                return;
            }
        }
    }

    public void refreshHasDraftReport() {
        this.players.forEach(p_417633_ -> p_417633_.refreshHasDraftReport(this.minecraft.getReportingContext()));
    }
}
