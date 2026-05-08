package com.mojang.realmsclient.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.BackupList;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.dto.PendingInvitesList;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsConfigurationDto;
import com.mojang.realmsclient.dto.RealmsDescriptionDto;
import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlotUpdateDto;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.RealmsWorldResetDto;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServerActivityList;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsHttpException;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import com.mojang.util.UndashedUuid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LenientJsonParser;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsClient {
    public static final RealmsClient.Environment ENVIRONMENT = Optional.ofNullable(System.getenv("realms.environment"))
        .or(() -> Optional.ofNullable(System.getProperty("realms.environment")))
        .flatMap(RealmsClient.Environment::byName)
        .orElse(RealmsClient.Environment.PRODUCTION);
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static volatile RealmsClient realmsClientInstance = null;
    private final CompletableFuture<Set<String>> featureFlags;
    private final String sessionId;
    private final String username;
    private final Minecraft minecraft;
    private static final String WORLDS_RESOURCE_PATH = "worlds";
    private static final String INVITES_RESOURCE_PATH = "invites";
    private static final String MCO_RESOURCE_PATH = "mco";
    private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
    private static final String ACTIVITIES_RESOURCE = "activities";
    private static final String OPS_RESOURCE = "ops";
    private static final String REGIONS_RESOURCE = "regions/ping/stat";
    private static final String PREFERRED_REGION_RESOURCE = "regions/preferredRegions";
    private static final String TRIALS_RESOURCE = "trial";
    private static final String NOTIFICATIONS_RESOURCE = "notifications";
    private static final String FEATURE_FLAGS_RESOURCE = "feature/v1";
    private static final String PATH_LIST_ALL_REALMS = "/listUserWorldsOfType/any";
    private static final String PATH_CREATE_SNAPSHOT_REALM = "/$PARENT_WORLD_ID/createPrereleaseRealm";
    private static final String PATH_SNAPSHOT_ELIGIBLE_REALMS = "/listPrereleaseEligibleWorlds";
    private static final String PATH_INITIALIZE = "/$WORLD_ID/initialize";
    private static final String PATH_GET_ACTIVTIES = "/$WORLD_ID";
    private static final String PATH_GET_LIVESTATS = "/liveplayerlist";
    private static final String PATH_GET_SUBSCRIPTION = "/$WORLD_ID";
    private static final String PATH_OP = "/$WORLD_ID/$PROFILE_UUID";
    private static final String PATH_PUT_INTO_MINIGAMES_MODE = "/minigames/$MINIGAME_ID/$WORLD_ID";
    private static final String PATH_AVAILABLE = "/available";
    private static final String PATH_TEMPLATES = "/templates/$WORLD_TYPE";
    private static final String PATH_WORLD_JOIN = "/v1/$ID/join/pc";
    private static final String PATH_WORLD_GET = "/$ID";
    private static final String PATH_WORLD_INVITES = "/$WORLD_ID";
    private static final String PATH_WORLD_UNINVITE = "/$WORLD_ID/invite/$UUID";
    private static final String PATH_PENDING_INVITES_COUNT = "/count/pending";
    private static final String PATH_PENDING_INVITES = "/pending";
    private static final String PATH_ACCEPT_INVITE = "/accept/$INVITATION_ID";
    private static final String PATH_REJECT_INVITE = "/reject/$INVITATION_ID";
    private static final String PATH_UNINVITE_MYSELF = "/$WORLD_ID";
    private static final String PATH_WORLD_CONFIGURE = "/$WORLD_ID/configuration";
    private static final String PATH_SLOT = "/$WORLD_ID/slot/$SLOT_ID";
    private static final String PATH_WORLD_OPEN = "/$WORLD_ID/open";
    private static final String PATH_WORLD_CLOSE = "/$WORLD_ID/close";
    private static final String PATH_WORLD_RESET = "/$WORLD_ID/reset";
    private static final String PATH_DELETE_WORLD = "/$WORLD_ID";
    private static final String PATH_WORLD_BACKUPS = "/$WORLD_ID/backups";
    private static final String PATH_WORLD_DOWNLOAD = "/$WORLD_ID/slot/$SLOT_ID/download";
    private static final String PATH_WORLD_UPLOAD = "/$WORLD_ID/backups/upload";
    private static final String PATH_CLIENT_COMPATIBLE = "/client/compatible";
    private static final String PATH_TOS_AGREED = "/tos/agreed";
    private static final String PATH_NEWS = "/v1/news";
    private static final String PATH_MARK_NOTIFICATIONS_SEEN = "/seen";
    private static final String PATH_DISMISS_NOTIFICATIONS = "/dismiss";
    private static final GuardedSerializer GSON = new GuardedSerializer();

    public static RealmsClient getOrCreate() {
        Minecraft minecraft = Minecraft.getInstance();
        return getOrCreate(minecraft);
    }

    public static RealmsClient getOrCreate(Minecraft minecraft) {
        String s = minecraft.getUser().getName();
        String s1 = minecraft.getUser().getSessionId();
        RealmsClient realmsclient = realmsClientInstance;
        if (realmsclient != null) {
            return realmsclient;
        } else {
            synchronized (RealmsClient.class) {
                RealmsClient realmsclient1 = realmsClientInstance;
                if (realmsclient1 != null) {
                    return realmsclient1;
                } else {
                    realmsclient1 = new RealmsClient(s1, s, minecraft);
                    realmsClientInstance = realmsclient1;
                    return realmsclient1;
                }
            }
        }
    }

    private RealmsClient(String sessionId, String username, Minecraft minecraft) {
        this.sessionId = sessionId;
        this.username = username;
        this.minecraft = minecraft;
        RealmsClientConfig.setProxy(minecraft.getProxy());
        this.featureFlags = CompletableFuture.supplyAsync(this::fetchFeatureFlags, Util.nonCriticalIoPool());
    }

    public Set<String> getFeatureFlags() {
        return this.featureFlags.join();
    }

    private Set<String> fetchFeatureFlags() {
        if (Minecraft.getInstance().isOfflineDeveloperMode()) {
            return Set.of();
        } else {
            String s = url("feature/v1", null, false);

            try {
                String s1 = this.execute(Request.get(s, 5000, 10000));
                JsonArray jsonarray = LenientJsonParser.parse(s1).getAsJsonArray();
                Set<String> set = jsonarray.asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
                LOGGER.debug("Fetched Realms feature flags: {}", set);
                return set;
            } catch (RealmsServiceException realmsserviceexception) {
                LOGGER.error("Failed to fetch Realms feature flags", (Throwable)realmsserviceexception);
            } catch (Exception exception) {
                LOGGER.error("Could not parse Realms feature flags", (Throwable)exception);
            }

            return Set.of();
        }
    }

    public RealmsServerList listRealms() throws RealmsServiceException {
        String s = this.url("worlds");
        if (RealmsMainScreen.isSnapshot()) {
            s = s + "/listUserWorldsOfType/any";
        }

        String s1 = this.execute(Request.get(s));
        return RealmsServerList.parse(GSON, s1);
    }

    public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
        String s = this.url("worlds/listPrereleaseEligibleWorlds");
        String s1 = this.execute(Request.get(s));
        return RealmsServerList.parse(GSON, s1).servers;
    }

    public RealmsServer createSnapshotRealm(Long parentId) throws RealmsServiceException {
        String s = String.valueOf(parentId);
        String s1 = this.url("worlds" + "/$PARENT_WORLD_ID/createPrereleaseRealm".replace("$PARENT_WORLD_ID", s));
        return RealmsServer.parse(GSON, this.execute(Request.post(s1, s)));
    }

    public List<RealmsNotification> getNotifications() throws RealmsServiceException {
        String s = this.url("notifications");
        String s1 = this.execute(Request.get(s));
        return RealmsNotification.parseList(s1);
    }

    private static JsonArray uuidListToJsonArray(List<UUID> uuidList) {
        JsonArray jsonarray = new JsonArray();

        for (UUID uuid : uuidList) {
            if (uuid != null) {
                jsonarray.add(uuid.toString());
            }
        }

        return jsonarray;
    }

    public void notificationsSeen(List<UUID> uuidList) throws RealmsServiceException {
        String s = this.url("notifications/seen");
        this.execute(Request.post(s, GSON.toJson(uuidListToJsonArray(uuidList))));
    }

    public void notificationsDismiss(List<UUID> uuidList) throws RealmsServiceException {
        String s = this.url("notifications/dismiss");
        this.execute(Request.post(s, GSON.toJson(uuidListToJsonArray(uuidList))));
    }

    public RealmsServer getOwnRealm(long id) throws RealmsServiceException {
        String s = this.url("worlds" + "/$ID".replace("$ID", String.valueOf(id)));
        String s1 = this.execute(Request.get(s));
        return RealmsServer.parse(GSON, s1);
    }

    public PreferredRegionsDto getPreferredRegionSelections() throws RealmsServiceException {
        String s = this.url("regions/preferredRegions");
        String s1 = this.execute(Request.get(s));

        try {
            PreferredRegionsDto preferredregionsdto = GSON.fromJson(s1, PreferredRegionsDto.class);
            if (preferredregionsdto == null) {
                return PreferredRegionsDto.empty();
            } else {
                Set<RealmsRegion> set = preferredregionsdto.regionData().stream().map(RegionDataDto::region).collect(Collectors.toSet());

                for (RealmsRegion realmsregion : RealmsRegion.values()) {
                    if (realmsregion != RealmsRegion.INVALID_REGION && !set.contains(realmsregion)) {
                        LOGGER.debug("No realms region matching {} in server response", realmsregion);
                    }
                }

                return preferredregionsdto;
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse PreferredRegionSelections: {}", exception.getMessage());
            return PreferredRegionsDto.empty();
        }
    }

    public ServerActivityList getActivity(long worldId) throws RealmsServiceException {
        String s = this.url("activities" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.get(s));
        return ServerActivityList.parse(s1);
    }

    public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
        String s = this.url("activities/liveplayerlist");
        String s1 = this.execute(Request.get(s));
        return RealmsServerPlayerLists.parse(s1);
    }

    public RealmsJoinInformation join(long worldId) throws RealmsServiceException {
        String s = this.url("worlds" + "/v1/$ID/join/pc".replace("$ID", worldId + ""));
        String s1 = this.execute(Request.get(s, 5000, 30000));
        return RealmsJoinInformation.parse(GSON, s1);
    }

    public void initializeRealm(long worldId, String name, String description) throws RealmsServiceException {
        RealmsDescriptionDto realmsdescriptiondto = new RealmsDescriptionDto(name, description);
        String s = this.url("worlds" + "/$WORLD_ID/initialize".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = GSON.toJson(realmsdescriptiondto);
        this.execute(Request.post(s, s1, 5000, 10000));
    }

    public boolean hasParentalConsent() throws RealmsServiceException {
        String s = this.url("mco/available");
        String s1 = this.execute(Request.get(s));
        return Boolean.parseBoolean(s1);
    }

    public RealmsClient.CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
        String s = this.url("mco/client/compatible");
        String s1 = this.execute(Request.get(s));

        try {
            return RealmsClient.CompatibleVersionResponse.valueOf(s1);
        } catch (IllegalArgumentException illegalargumentexception) {
            throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse(s1));
        }
    }

    public void uninvite(long worldId, UUID playerUuid) throws RealmsServiceException {
        String s = this.url(
            "invites" + "/$WORLD_ID/invite/$UUID".replace("$WORLD_ID", String.valueOf(worldId)).replace("$UUID", UndashedUuid.toString(playerUuid))
        );
        this.execute(Request.delete(s));
    }

    public void uninviteMyselfFrom(long worldId) throws RealmsServiceException {
        String s = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(worldId)));
        this.execute(Request.delete(s));
    }

    public List<PlayerInfo> invite(long worldId, String playerName) throws RealmsServiceException {
        PlayerInfo playerinfo = new PlayerInfo();
        playerinfo.setName(playerName);
        String s = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.post(s, GSON.toJson(playerinfo)));
        return RealmsServer.parse(GSON, s1).players;
    }

    public BackupList backupsFor(long worldId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.get(s));
        return BackupList.parse(s1);
    }

    public void updateConfiguration(
        long worldId,
        String name,
        String description,
        @Nullable RegionSelectionPreferenceDto regionSettings,
        int slotId,
        RealmsWorldOptions options,
        List<RealmsSetting> settings
    ) throws RealmsServiceException {
        RegionSelectionPreferenceDto regionselectionpreferencedto = regionSettings != null
            ? regionSettings
            : new RegionSelectionPreferenceDto(RegionSelectionPreference.DEFAULT_SELECTION, null);
        RealmsDescriptionDto realmsdescriptiondto = new RealmsDescriptionDto(name, description);
        RealmsSlotUpdateDto realmsslotupdatedto = new RealmsSlotUpdateDto(slotId, options, RealmsSetting.isHardcore(settings));
        RealmsConfigurationDto realmsconfigurationdto = new RealmsConfigurationDto(
            realmsslotupdatedto, settings, regionselectionpreferencedto, realmsdescriptiondto
        );
        String s = this.url("worlds" + "/$WORLD_ID/configuration".replace("$WORLD_ID", String.valueOf(worldId)));
        this.execute(Request.post(s, GSON.toJson(realmsconfigurationdto)));
    }

    public void updateSlot(long worldId, int slotId, RealmsWorldOptions options, List<RealmsSetting> settings) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(worldId)).replace("$SLOT_ID", String.valueOf(slotId)));
        String s1 = GSON.toJson(new RealmsSlotUpdateDto(slotId, options, RealmsSetting.isHardcore(settings)));
        this.execute(Request.post(s, s1));
    }

    public boolean switchSlot(long worldId, int slotId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(worldId)).replace("$SLOT_ID", String.valueOf(slotId)));
        String s1 = this.execute(Request.put(s, ""));
        return Boolean.valueOf(s1);
    }

    public void restoreWorld(long worldId, String backupId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(worldId)), "backupId=" + backupId);
        this.execute(Request.put(s, "", 40000, 600000));
    }

    public WorldTemplatePaginatedList fetchWorldTemplates(int page, int pageSize, RealmsServer.WorldType worldType) throws RealmsServiceException {
        String s = this.url(
            "worlds" + "/templates/$WORLD_TYPE".replace("$WORLD_TYPE", worldType.toString()),
            String.format(Locale.ROOT, "page=%d&pageSize=%d", page, pageSize)
        );
        String s1 = this.execute(Request.get(s));
        return WorldTemplatePaginatedList.parse(s1);
    }

    public Boolean putIntoMinigameMode(long worldId, String minigameId) throws RealmsServiceException {
        String s = "/minigames/$MINIGAME_ID/$WORLD_ID".replace("$MINIGAME_ID", minigameId).replace("$WORLD_ID", String.valueOf(worldId));
        String s1 = this.url("worlds" + s);
        return Boolean.valueOf(this.execute(Request.put(s1, "")));
    }

    public Ops op(long worldId, UUID profileUuid) throws RealmsServiceException {
        String s = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(worldId)).replace("$PROFILE_UUID", UndashedUuid.toString(profileUuid));
        String s1 = this.url("ops" + s);
        return Ops.parse(this.execute(Request.post(s1, "")));
    }

    public Ops deop(long worldId, UUID profileUuid) throws RealmsServiceException {
        String s = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(worldId)).replace("$PROFILE_UUID", UndashedUuid.toString(profileUuid));
        String s1 = this.url("ops" + s);
        return Ops.parse(this.execute(Request.delete(s1)));
    }

    public Boolean open(long worldId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/open".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.put(s, ""));
        return Boolean.valueOf(s1);
    }

    public Boolean close(long worldId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/close".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.put(s, ""));
        return Boolean.valueOf(s1);
    }

    public Boolean resetWorldWithTemplate(long worldId, String worldTemplateId) throws RealmsServiceException {
        RealmsWorldResetDto realmsworldresetdto = new RealmsWorldResetDto(null, Long.valueOf(worldTemplateId), -1, false, Set.of());
        String s = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.post(s, GSON.toJson(realmsworldresetdto), 30000, 80000));
        return Boolean.valueOf(s1);
    }

    public Subscription subscriptionFor(long worldId) throws RealmsServiceException {
        String s = this.url("subscriptions" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = this.execute(Request.get(s));
        return Subscription.parse(s1);
    }

    public int pendingInvitesCount() throws RealmsServiceException {
        return this.pendingInvites().pendingInvites.size();
    }

    public PendingInvitesList pendingInvites() throws RealmsServiceException {
        String s = this.url("invites/pending");
        String s1 = this.execute(Request.get(s));
        PendingInvitesList pendinginviteslist = PendingInvitesList.parse(s1);
        pendinginviteslist.pendingInvites.removeIf(this::isBlocked);
        return pendinginviteslist;
    }

    private boolean isBlocked(PendingInvite pendingInvite) {
        return this.minecraft.getPlayerSocialManager().isBlocked(pendingInvite.realmOwnerUuid);
    }

    public void acceptInvitation(String inviteId) throws RealmsServiceException {
        String s = this.url("invites" + "/accept/$INVITATION_ID".replace("$INVITATION_ID", inviteId));
        this.execute(Request.put(s, ""));
    }

    public WorldDownload requestDownloadInfo(long worldId, int slotId) throws RealmsServiceException {
        String s = this.url(
            "worlds" + "/$WORLD_ID/slot/$SLOT_ID/download".replace("$WORLD_ID", String.valueOf(worldId)).replace("$SLOT_ID", String.valueOf(slotId))
        );
        String s1 = this.execute(Request.get(s));
        return WorldDownload.parse(s1);
    }

    @Nullable
    public UploadInfo requestUploadInfo(long worldId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID/backups/upload".replace("$WORLD_ID", String.valueOf(worldId)));
        String s1 = UploadTokenCache.get(worldId);
        UploadInfo uploadinfo = UploadInfo.parse(this.execute(Request.put(s, UploadInfo.createRequest(s1))));
        if (uploadinfo != null) {
            UploadTokenCache.put(worldId, uploadinfo.getToken());
        }

        return uploadinfo;
    }

    public void rejectInvitation(String inviteId) throws RealmsServiceException {
        String s = this.url("invites" + "/reject/$INVITATION_ID".replace("$INVITATION_ID", inviteId));
        this.execute(Request.put(s, ""));
    }

    public void agreeToTos() throws RealmsServiceException {
        String s = this.url("mco/tos/agreed");
        this.execute(Request.post(s, ""));
    }

    public RealmsNews getNews() throws RealmsServiceException {
        String s = this.url("mco/v1/news");
        String s1 = this.execute(Request.get(s, 5000, 10000));
        return RealmsNews.parse(s1);
    }

    public void sendPingResults(PingResult pingResult) throws RealmsServiceException {
        String s = this.url("regions/ping/stat");
        this.execute(Request.post(s, GSON.toJson(pingResult)));
    }

    public Boolean trialAvailable() throws RealmsServiceException {
        String s = this.url("trial");
        String s1 = this.execute(Request.get(s));
        return Boolean.valueOf(s1);
    }

    public void deleteRealm(long worldId) throws RealmsServiceException {
        String s = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(worldId)));
        this.execute(Request.delete(s));
    }

    private String url(String path) throws RealmsServiceException {
        return this.url(path, null);
    }

    private String url(String path, @Nullable String query) throws RealmsServiceException {
        return url(path, query, this.getFeatureFlags().contains("realms_in_aks"));
    }

    private static String url(String path, @Nullable String query, boolean useAlternativeUrl) {
        try {
            return new URI(ENVIRONMENT.protocol, useAlternativeUrl ? ENVIRONMENT.alternativeUrl : ENVIRONMENT.baseUrl, "/" + path, query, null)
                .toASCIIString();
        } catch (URISyntaxException urisyntaxexception) {
            throw new IllegalArgumentException(path, urisyntaxexception);
        }
    }

    private String execute(Request<?> request) throws RealmsServiceException {
        request.cookie("sid", this.sessionId);
        request.cookie("user", this.username);
        request.cookie("version", SharedConstants.getCurrentVersion().name());
        request.addSnapshotHeader(RealmsMainScreen.isSnapshot());

        try {
            int i = request.responseCode();
            if (i != 503 && i != 277) {
                String s1 = request.text();
                if (i >= 200 && i < 300) {
                    return s1;
                } else if (i == 401) {
                    String s2 = request.getHeader("WWW-Authenticate");
                    LOGGER.info("Could not authorize you against Realms server: {}", s2);
                    throw new RealmsServiceException(new RealmsError.AuthenticationError(s2));
                } else {
                    String s = request.connection.getContentType();
                    if (s != null && s.startsWith("text/html")) {
                        throw new RealmsServiceException(RealmsError.CustomError.htmlPayload(i, s1));
                    } else {
                        RealmsError realmserror = RealmsError.parse(i, s1);
                        throw new RealmsServiceException(realmserror);
                    }
                }
            } else {
                int j = request.getRetryAfterHeader();
                throw new RetryCallException(j, i);
            }
        } catch (RealmsHttpException realmshttpexception) {
            throw new RealmsServiceException(RealmsError.CustomError.connectivityError(realmshttpexception));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum CompatibleVersionResponse {
        COMPATIBLE,
        OUTDATED,
        OTHER;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Environment {
        PRODUCTION("pc.realms.minecraft.net", "java.frontendlegacy.realms.minecraft-services.net", "https"),
        STAGE("pc-stage.realms.minecraft.net", "java.frontendlegacy.stage-c2a40e62.realms.minecraft-services.net", "https"),
        LOCAL("localhost:8080", "localhost:8080", "http");

        public final String baseUrl;
        public final String alternativeUrl;
        public final String protocol;

        private Environment(String baseUrl, String alternativeUrl, String protocol) {
            this.baseUrl = baseUrl;
            this.alternativeUrl = alternativeUrl;
            this.protocol = protocol;
        }

        public static Optional<RealmsClient.Environment> byName(String name) {
            String s = name.toLowerCase(Locale.ROOT);

            return switch (s) {
                case "production" -> Optional.of(PRODUCTION);
                case "local" -> Optional.of(LOCAL);
                case "stage", "staging" -> Optional.of(STAGE);
                default -> Optional.empty();
            };
        }
    }
}
