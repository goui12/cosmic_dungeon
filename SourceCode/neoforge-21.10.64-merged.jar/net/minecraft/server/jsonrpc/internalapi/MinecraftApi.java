package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.notifications.NotificationManager;

public class MinecraftApi {
    private final NotificationManager notificationManager;
    private final MinecraftAllowListService allowListService;
    private final MinecraftBanListService banListService;
    private final MinecraftPlayerListService minecraftPlayerListService;
    private final MinecraftGameRuleService gameRuleService;
    private final MinecraftOperatorListService minecraftOperatorListService;
    private final MinecraftServerSettingsService minecraftServerSettingsService;
    private final MinecraftServerStateService minecraftServerStateService;
    private final MinecraftExecutorService executorService;

    public MinecraftApi(
        NotificationManager notificationManager,
        MinecraftAllowListService allowListService,
        MinecraftBanListService banListService,
        MinecraftPlayerListService playerListService,
        MinecraftGameRuleService gameRuleService,
        MinecraftOperatorListService operatorListService,
        MinecraftServerSettingsService serverSettingService,
        MinecraftServerStateService serverStateService,
        MinecraftExecutorService executorService
    ) {
        this.notificationManager = notificationManager;
        this.allowListService = allowListService;
        this.banListService = banListService;
        this.minecraftPlayerListService = playerListService;
        this.gameRuleService = gameRuleService;
        this.minecraftOperatorListService = operatorListService;
        this.minecraftServerSettingsService = serverSettingService;
        this.minecraftServerStateService = serverStateService;
        this.executorService = executorService;
    }

    public <V> CompletableFuture<V> submit(Supplier<V> task) {
        return this.executorService.submit(task);
    }

    public CompletableFuture<Void> submit(Runnable task) {
        return this.executorService.submit(task);
    }

    public MinecraftAllowListService allowListService() {
        return this.allowListService;
    }

    public MinecraftBanListService banListService() {
        return this.banListService;
    }

    public MinecraftPlayerListService playerListService() {
        return this.minecraftPlayerListService;
    }

    public MinecraftGameRuleService gameRuleService() {
        return this.gameRuleService;
    }

    public MinecraftOperatorListService operatorListService() {
        return this.minecraftOperatorListService;
    }

    public MinecraftServerSettingsService serverSettingsService() {
        return this.minecraftServerSettingsService;
    }

    public MinecraftServerStateService serverStateService() {
        return this.minecraftServerStateService;
    }

    public NotificationManager notificationManager() {
        return this.notificationManager;
    }

    public static MinecraftApi of(DedicatedServer server) {
        JsonRpcLogger jsonrpclogger = new JsonRpcLogger();
        MinecraftAllowListServiceImpl minecraftallowlistserviceimpl = new MinecraftAllowListServiceImpl(server, jsonrpclogger);
        MinecraftBanListServiceImpl minecraftbanlistserviceimpl = new MinecraftBanListServiceImpl(server, jsonrpclogger);
        MinecraftPlayerListServiceImpl minecraftplayerlistserviceimpl = new MinecraftPlayerListServiceImpl(server, jsonrpclogger);
        MinecraftGameRuleServiceImpl minecraftgameruleserviceimpl = new MinecraftGameRuleServiceImpl(server, jsonrpclogger);
        MinecraftOperatorListServiceImpl minecraftoperatorlistserviceimpl = new MinecraftOperatorListServiceImpl(server, jsonrpclogger);
        MinecraftServerSettingsServiceImpl minecraftserversettingsserviceimpl = new MinecraftServerSettingsServiceImpl(server, jsonrpclogger);
        MinecraftServerStateServiceImpl minecraftserverstateserviceimpl = new MinecraftServerStateServiceImpl(server, jsonrpclogger);
        MinecraftExecutorService minecraftexecutorservice = new MinecraftExecutorServiceImpl(server);
        return new MinecraftApi(
            server.notificationManager(),
            minecraftallowlistserviceimpl,
            minecraftbanlistserviceimpl,
            minecraftplayerlistserviceimpl,
            minecraftgameruleserviceimpl,
            minecraftoperatorlistserviceimpl,
            minecraftserversettingsserviceimpl,
            minecraftserverstateserviceimpl,
            minecraftexecutorservice
        );
    }
}
