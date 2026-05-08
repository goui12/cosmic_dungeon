package net.minecraft.server.notifications;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameRules;

public class NotificationManager implements NotificationService {
    private final List<NotificationService> notificationServices = Lists.newArrayList();

    public void registerService(NotificationService service) {
        this.notificationServices.add(service);
    }

    @Override
    public void playerJoined(ServerPlayer player) {
        this.notificationServices.forEach(p_443382_ -> p_443382_.playerJoined(player));
    }

    @Override
    public void playerLeft(ServerPlayer player) {
        this.notificationServices.forEach(p_443011_ -> p_443011_.playerLeft(player));
    }

    @Override
    public void serverStarted() {
        this.notificationServices.forEach(NotificationService::serverStarted);
    }

    @Override
    public void serverShuttingDown() {
        this.notificationServices.forEach(NotificationService::serverShuttingDown);
    }

    @Override
    public void serverSaveStarted() {
        this.notificationServices.forEach(NotificationService::serverSaveStarted);
    }

    @Override
    public void serverSaveCompleted() {
        this.notificationServices.forEach(NotificationService::serverSaveCompleted);
    }

    @Override
    public void playerOped(ServerOpListEntry entry) {
        this.notificationServices.forEach(p_443522_ -> p_443522_.playerOped(entry));
    }

    @Override
    public void playerDeoped(ServerOpListEntry entry) {
        this.notificationServices.forEach(p_442984_ -> p_442984_.playerDeoped(entry));
    }

    @Override
    public void playerAddedToAllowlist(NameAndId player) {
        this.notificationServices.forEach(p_442575_ -> p_442575_.playerAddedToAllowlist(player));
    }

    @Override
    public void playerRemovedFromAllowlist(NameAndId player) {
        this.notificationServices.forEach(p_443062_ -> p_443062_.playerRemovedFromAllowlist(player));
    }

    @Override
    public void ipBanned(IpBanListEntry entry) {
        this.notificationServices.forEach(p_443574_ -> p_443574_.ipBanned(entry));
    }

    @Override
    public void ipUnbanned(String ip) {
        this.notificationServices.forEach(p_443156_ -> p_443156_.ipUnbanned(ip));
    }

    @Override
    public void playerBanned(UserBanListEntry entry) {
        this.notificationServices.forEach(p_442822_ -> p_442822_.playerBanned(entry));
    }

    @Override
    public void playerUnbanned(NameAndId player) {
        this.notificationServices.forEach(p_442676_ -> p_442676_.playerUnbanned(player));
    }

    @Override
    public void onGameRuleChanged(String gamerule, GameRules.Value<?> value) {
        this.notificationServices.forEach(p_442488_ -> p_442488_.onGameRuleChanged(gamerule, value));
    }

    @Override
    public void statusHeartbeat() {
        this.notificationServices.forEach(NotificationService::statusHeartbeat);
    }
}
