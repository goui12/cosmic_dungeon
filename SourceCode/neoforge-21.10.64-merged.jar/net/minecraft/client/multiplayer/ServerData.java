package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ICON_SIZE = 1024;
    public String name;
    public String ip;
    public Component status;
    public Component motd;
    @Nullable
    public ServerStatus.Players players;
    public long ping;
    public int protocol = SharedConstants.getCurrentVersion().protocolVersion();
    public Component version = Component.literal(SharedConstants.getCurrentVersion().name());
    public List<Component> playerList = Collections.emptyList();
    private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
    @Nullable
    private byte[] iconBytes;
    private ServerData.Type type;
    private int acceptedCodeOfConduct;
    private ServerData.State state = ServerData.State.INITIAL;
    public net.neoforged.neoforge.client.ExtendedServerListData neoForgeData = null;

    public ServerData(String name, String ip, ServerData.Type type) {
        this.name = name;
        this.ip = ip;
        this.type = type;
    }

    public CompoundTag write() {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("name", this.name);
        compoundtag.putString("ip", this.ip);
        compoundtag.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
        compoundtag.store(ServerData.ServerPackStatus.FIELD_CODEC, this.packStatus);
        if (this.acceptedCodeOfConduct != 0) {
            compoundtag.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
        }

        return compoundtag;
    }

    public ServerData.ServerPackStatus getResourcePackStatus() {
        return this.packStatus;
    }

    public void setResourcePackStatus(ServerData.ServerPackStatus packStatus) {
        this.packStatus = packStatus;
    }

    /**
     * Takes an NBTTagCompound with 'name' and 'ip' keys, returns a ServerData instance.
     */
    public static ServerData read(CompoundTag nbtCompound) {
        ServerData serverdata = new ServerData(nbtCompound.getStringOr("name", ""), nbtCompound.getStringOr("ip", ""), ServerData.Type.OTHER);
        serverdata.setIconBytes(nbtCompound.read("icon", ExtraCodecs.BASE64_STRING).orElse(null));
        serverdata.setResourcePackStatus(nbtCompound.read(ServerData.ServerPackStatus.FIELD_CODEC).orElse(ServerData.ServerPackStatus.PROMPT));
        serverdata.acceptedCodeOfConduct = nbtCompound.getIntOr("acceptedCodeOfConduct", 0);
        return serverdata;
    }

    @Nullable
    public byte[] getIconBytes() {
        return this.iconBytes;
    }

    public void setIconBytes(@Nullable byte[] iconBytes) {
        this.iconBytes = iconBytes;
    }

    public boolean isLan() {
        return this.type == ServerData.Type.LAN;
    }

    public boolean isRealm() {
        return this.type == ServerData.Type.REALM;
    }

    public ServerData.Type type() {
        return this.type;
    }

    public boolean hasAcceptedCodeOfConduct(String codeOfConduct) {
        return this.acceptedCodeOfConduct == codeOfConduct.hashCode();
    }

    public void acceptCodeOfConduct(String codeOfConduct) {
        this.acceptedCodeOfConduct = codeOfConduct.hashCode();
    }

    public void clearCodeOfConduct() {
        this.acceptedCodeOfConduct = 0;
    }

    public void copyNameIconFrom(ServerData other) {
        this.ip = other.ip;
        this.name = other.name;
        this.iconBytes = other.iconBytes;
    }

    public void copyFrom(ServerData serverData) {
        this.copyNameIconFrom(serverData);
        this.setResourcePackStatus(serverData.getResourcePackStatus());
        this.type = serverData.type;
    }

    public ServerData.State state() {
        return this.state;
    }

    public void setState(ServerData.State state) {
        this.state = state;
    }

    @Nullable
    public static byte[] validateIcon(@Nullable byte[] icon) {
        if (icon != null) {
            try {
                PngInfo pnginfo = PngInfo.fromBytes(icon);
                if (pnginfo.width() <= 1024 && pnginfo.height() <= 1024) {
                    return icon;
                }
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to decode server icon", (Throwable)ioexception);
            }
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ServerPackStatus {
        ENABLED("enabled"),
        DISABLED("disabled"),
        PROMPT("prompt");

        public static final MapCodec<ServerData.ServerPackStatus> FIELD_CODEC = Codec.BOOL
            .optionalFieldOf("acceptTextures")
            .xmap(p_404910_ -> p_404910_.<ServerData.ServerPackStatus>map(p_404952_ -> p_404952_ ? ENABLED : DISABLED).orElse(PROMPT), p_405078_ -> {
                return switch (p_405078_) {
                    case ENABLED -> Optional.of(true);
                    case DISABLED -> Optional.of(false);
                    case PROMPT -> Optional.empty();
                };
            });
        private final Component name;

        private ServerPackStatus(String name) {
            this.name = Component.translatable("manageServer.resourcePack." + name);
        }

        public Component getName() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum State {
        INITIAL,
        PINGING,
        UNREACHABLE,
        INCOMPATIBLE,
        SUCCESSFUL;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        LAN,
        REALM,
        OTHER;
    }
}
