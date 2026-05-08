package net.minecraft.server.notifications;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameRules;

public class EmptyNotificationService implements NotificationService {
    @Override
    public void playerJoined(ServerPlayer p_442504_) {
    }

    @Override
    public void playerLeft(ServerPlayer p_443141_) {
    }

    @Override
    public void serverStarted() {
    }

    @Override
    public void serverShuttingDown() {
    }

    @Override
    public void serverSaveStarted() {
    }

    @Override
    public void serverSaveCompleted() {
    }

    @Override
    public void playerOped(ServerOpListEntry p_442919_) {
    }

    @Override
    public void playerDeoped(ServerOpListEntry p_443063_) {
    }

    @Override
    public void playerAddedToAllowlist(NameAndId p_443022_) {
    }

    @Override
    public void playerRemovedFromAllowlist(NameAndId p_443591_) {
    }

    @Override
    public void ipBanned(IpBanListEntry p_443268_) {
    }

    @Override
    public void ipUnbanned(String p_442960_) {
    }

    @Override
    public void playerBanned(UserBanListEntry p_443444_) {
    }

    @Override
    public void playerUnbanned(NameAndId p_443003_) {
    }

    @Override
    public void onGameRuleChanged(String p_443089_, GameRules.Value<?> p_443446_) {
    }

    @Override
    public void statusHeartbeat() {
    }
}
