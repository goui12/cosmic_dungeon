package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.util.UUIDTypeAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServer extends ValueObject implements ReflectionBasedSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_VALUE = -1;
    public static final Component WORLD_CLOSED_COMPONENT = Component.translatable("mco.play.button.realm.closed");
    @SerializedName("id")
    public long id = -1L;
    @Nullable
    @SerializedName("remoteSubscriptionId")
    public String remoteSubscriptionId;
    @Nullable
    @SerializedName("name")
    public String name;
    @SerializedName("motd")
    public String motd = "";
    @SerializedName("state")
    public RealmsServer.State state = RealmsServer.State.CLOSED;
    @Nullable
    @SerializedName("owner")
    public String owner;
    @SerializedName("ownerUUID")
    @JsonAdapter(UUIDTypeAdapter.class)
    public UUID ownerUUID = Util.NIL_UUID;
    @SerializedName("players")
    public List<PlayerInfo> players = Lists.newArrayList();
    @SerializedName("slots")
    private List<RealmsSlot> slotList = createEmptySlots();
    @Exclude
    public Map<Integer, RealmsSlot> slots = new HashMap<>();
    @SerializedName("expired")
    public boolean expired;
    @SerializedName("expiredTrial")
    public boolean expiredTrial = false;
    @SerializedName("daysLeft")
    public int daysLeft;
    @SerializedName("worldType")
    public RealmsServer.WorldType worldType = RealmsServer.WorldType.NORMAL;
    @SerializedName("isHardcore")
    public boolean isHardcore = false;
    @SerializedName("gameMode")
    public int gameMode = -1;
    @SerializedName("activeSlot")
    public int activeSlot = -1;
    @Nullable
    @SerializedName("minigameName")
    public String minigameName;
    @SerializedName("minigameId")
    public int minigameId = -1;
    @Nullable
    @SerializedName("minigameImage")
    public String minigameImage;
    @SerializedName("parentWorldId")
    public long parentRealmId = -1L;
    @Nullable
    @SerializedName("parentWorldName")
    public String parentWorldName;
    @SerializedName("activeVersion")
    public String activeVersion = "";
    @SerializedName("compatibility")
    public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
    @Nullable
    @SerializedName("regionSelectionPreference")
    public RegionSelectionPreferenceDto regionSelectionPreference;

    public String getDescription() {
        return this.motd;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getMinigameName() {
        return this.minigameName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String motd) {
        this.motd = motd;
    }

    public static RealmsServer parse(GuardedSerializer serializer, String json) {
        try {
            RealmsServer realmsserver = serializer.fromJson(json, RealmsServer.class);
            if (realmsserver == null) {
                LOGGER.error("Could not parse McoServer: {}", json);
                return new RealmsServer();
            } else {
                finalize(realmsserver);
                return realmsserver;
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServer: {}", exception.getMessage());
            return new RealmsServer();
        }
    }

    public static void finalize(RealmsServer server) {
        if (server.players == null) {
            server.players = Lists.newArrayList();
        }

        if (server.slotList == null) {
            server.slotList = createEmptySlots();
        }

        if (server.slots == null) {
            server.slots = new HashMap<>();
        }

        if (server.worldType == null) {
            server.worldType = RealmsServer.WorldType.NORMAL;
        }

        if (server.activeVersion == null) {
            server.activeVersion = "";
        }

        if (server.compatibility == null) {
            server.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
        }

        if (server.regionSelectionPreference == null) {
            server.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
        }

        sortInvited(server);
        finalizeSlots(server);
    }

    private static void sortInvited(RealmsServer server) {
        server.players
            .sort(
                (p_87502_, p_87503_) -> ComparisonChain.start()
                    .compareFalseFirst(p_87503_.getAccepted(), p_87502_.getAccepted())
                    .compare(p_87502_.getName().toLowerCase(Locale.ROOT), p_87503_.getName().toLowerCase(Locale.ROOT))
                    .result()
            );
    }

    private static void finalizeSlots(RealmsServer server) {
        server.slotList.forEach(p_419400_ -> server.slots.put(p_419400_.slotId, p_419400_));

        for (int i = 1; i <= 3; i++) {
            if (!server.slots.containsKey(i)) {
                server.slots.put(i, RealmsSlot.defaults(i));
            }
        }
    }

    private static List<RealmsSlot> createEmptySlots() {
        List<RealmsSlot> list = new ArrayList<>();
        list.add(RealmsSlot.defaults(1));
        list.add(RealmsSlot.defaults(2));
        list.add(RealmsSlot.defaults(3));
        return list;
    }

    public boolean isCompatible() {
        return this.compatibility.isCompatible();
    }

    public boolean needsUpgrade() {
        return this.compatibility.needsUpgrade();
    }

    public boolean needsDowngrade() {
        return this.compatibility.needsDowngrade();
    }

    public boolean shouldPlayButtonBeActive() {
        boolean flag = !this.expired && this.state == RealmsServer.State.OPEN;
        return flag && (this.isCompatible() || this.needsUpgrade() || this.isSelfOwnedServer());
    }

    private boolean isSelfOwnedServer() {
        return Minecraft.getInstance().isLocalPlayer(this.ownerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (other == this) {
            return true;
        } else if (other.getClass() != this.getClass()) {
            return false;
        } else {
            RealmsServer realmsserver = (RealmsServer)other;
            return new EqualsBuilder()
                .append(this.id, realmsserver.id)
                .append(this.name, realmsserver.name)
                .append(this.motd, realmsserver.motd)
                .append(this.state, realmsserver.state)
                .append(this.owner, realmsserver.owner)
                .append(this.expired, realmsserver.expired)
                .append(this.worldType, this.worldType)
                .isEquals();
        }
    }

    public RealmsServer clone() {
        RealmsServer realmsserver = new RealmsServer();
        realmsserver.id = this.id;
        realmsserver.remoteSubscriptionId = this.remoteSubscriptionId;
        realmsserver.name = this.name;
        realmsserver.motd = this.motd;
        realmsserver.state = this.state;
        realmsserver.owner = this.owner;
        realmsserver.players = this.players;
        realmsserver.slotList = this.slotList.stream().map(RealmsSlot::clone).toList();
        realmsserver.slots = this.cloneSlots(this.slots);
        realmsserver.expired = this.expired;
        realmsserver.expiredTrial = this.expiredTrial;
        realmsserver.daysLeft = this.daysLeft;
        realmsserver.worldType = this.worldType;
        realmsserver.isHardcore = this.isHardcore;
        realmsserver.gameMode = this.gameMode;
        realmsserver.ownerUUID = this.ownerUUID;
        realmsserver.minigameName = this.minigameName;
        realmsserver.activeSlot = this.activeSlot;
        realmsserver.minigameId = this.minigameId;
        realmsserver.minigameImage = this.minigameImage;
        realmsserver.parentWorldName = this.parentWorldName;
        realmsserver.parentRealmId = this.parentRealmId;
        realmsserver.activeVersion = this.activeVersion;
        realmsserver.compatibility = this.compatibility;
        realmsserver.regionSelectionPreference = this.regionSelectionPreference != null ? this.regionSelectionPreference.clone() : null;
        return realmsserver;
    }

    public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> slots) {
        Map<Integer, RealmsSlot> map = Maps.newHashMap();

        for (Entry<Integer, RealmsSlot> entry : slots.entrySet()) {
            map.put(entry.getKey(), new RealmsSlot(entry.getKey(), entry.getValue().options.clone(), entry.getValue().settings));
        }

        return map;
    }

    public boolean isSnapshotRealm() {
        return this.parentRealmId != -1L;
    }

    public boolean isMinigameActive() {
        return this.worldType == RealmsServer.WorldType.MINIGAME;
    }

    public String getWorldName(int slot) {
        return this.name == null
            ? this.slots.get(slot).options.getSlotName(slot)
            : this.name + " (" + this.slots.get(slot).options.getSlotName(slot) + ")";
    }

    public ServerData toServerData(String ip) {
        return new ServerData(Objects.requireNonNullElse(this.name, "unknown server"), ip, ServerData.Type.REALM);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Compatibility {
        UNVERIFIABLE,
        INCOMPATIBLE,
        RELEASE_TYPE_INCOMPATIBLE,
        NEEDS_DOWNGRADE,
        NEEDS_UPGRADE,
        COMPATIBLE;

        public boolean isCompatible() {
            return this == COMPATIBLE;
        }

        public boolean needsUpgrade() {
            return this == NEEDS_UPGRADE;
        }

        public boolean needsDowngrade() {
            return this == NEEDS_DOWNGRADE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class McoServerComparator implements Comparator<RealmsServer> {
        private final String refOwner;

        public McoServerComparator(String refOwner) {
            this.refOwner = refOwner;
        }

        public int compare(RealmsServer first, RealmsServer second) {
            return ComparisonChain.start()
                .compareTrueFirst(first.isSnapshotRealm(), second.isSnapshotRealm())
                .compareTrueFirst(first.state == RealmsServer.State.UNINITIALIZED, second.state == RealmsServer.State.UNINITIALIZED)
                .compareTrueFirst(first.expiredTrial, second.expiredTrial)
                .compareTrueFirst(Objects.equals(first.owner, this.refOwner), Objects.equals(second.owner, this.refOwner))
                .compareFalseFirst(first.expired, second.expired)
                .compareTrueFirst(first.state == RealmsServer.State.OPEN, second.state == RealmsServer.State.OPEN)
                .compare(first.id, second.id)
                .result();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum State {
        CLOSED,
        OPEN,
        UNINITIALIZED;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum WorldType {
        NORMAL,
        MINIGAME,
        ADVENTUREMAP,
        EXPERIENCE,
        INSPIRATION;
    }
}
