package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoUpdatePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerInfoUpdatePacket> STREAM_CODEC = Packet.codec(
        ClientboundPlayerInfoUpdatePacket::write, ClientboundPlayerInfoUpdatePacket::new
    );
    private final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions;
    private final List<ClientboundPlayerInfoUpdatePacket.Entry> entries;

    public ClientboundPlayerInfoUpdatePacket(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions, Collection<ServerPlayer> players) {
        this.actions = actions;
        this.entries = players.stream().map(ClientboundPlayerInfoUpdatePacket.Entry::new).toList();
    }

    public ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action action, ServerPlayer player) {
        this.actions = EnumSet.of(action);
        this.entries = List.of(new ClientboundPlayerInfoUpdatePacket.Entry(player));
    }

    public static ClientboundPlayerInfoUpdatePacket createPlayerInitializing(Collection<ServerPlayer> players) {
        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> enumset = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
        );
        return new ClientboundPlayerInfoUpdatePacket(enumset, players);
    }

    private ClientboundPlayerInfoUpdatePacket(RegistryFriendlyByteBuf buffer) {
        this.actions = buffer.readEnumSet(ClientboundPlayerInfoUpdatePacket.Action.class);
        this.entries = buffer.readList(
            p_323148_ -> {
                ClientboundPlayerInfoUpdatePacket.EntryBuilder clientboundplayerinfoupdatepacket$entrybuilder = new ClientboundPlayerInfoUpdatePacket.EntryBuilder(
                    p_323148_.readUUID()
                );

                for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : this.actions) {
                    clientboundplayerinfoupdatepacket$action.reader.read(clientboundplayerinfoupdatepacket$entrybuilder, (RegistryFriendlyByteBuf)p_323148_);
                }

                return clientboundplayerinfoupdatepacket$entrybuilder.build();
            }
        );
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnumSet(this.actions, ClientboundPlayerInfoUpdatePacket.Action.class);
        buffer.writeCollection(this.entries, (p_323146_, p_323147_) -> {
            p_323146_.writeUUID(p_323147_.profileId());

            for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : this.actions) {
                clientboundplayerinfoupdatepacket$action.writer.write((RegistryFriendlyByteBuf)p_323146_, p_323147_);
            }
        });
    }

    @Override
    public PacketType<ClientboundPlayerInfoUpdatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener handler) {
        handler.handlePlayerInfoUpdate(this);
    }

    public EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions() {
        return this.actions;
    }

    public List<ClientboundPlayerInfoUpdatePacket.Entry> entries() {
        return this.entries;
    }

    public List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries() {
        return this.actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) ? this.entries : List.of();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("actions", this.actions).add("entries", this.entries).toString();
    }

    public static enum Action {
        ADD_PLAYER((p_442339_, p_442340_) -> {
            String s = ByteBufCodecs.PLAYER_NAME.decode(p_442340_);
            PropertyMap propertymap = ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(p_442340_);
            p_442339_.profile = new GameProfile(p_442339_.profileId, s, propertymap);
        }, (p_442341_, p_442342_) -> {
            GameProfile gameprofile = Objects.requireNonNull(p_442342_.profile());
            ByteBufCodecs.PLAYER_NAME.encode(p_442341_, gameprofile.name());
            ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(p_442341_, gameprofile.properties());
        }),
        INITIALIZE_CHAT(
            (p_323155_, p_323156_) -> p_323155_.chatSession = p_323156_.readNullable(RemoteChatSession.Data::read),
            (p_323151_, p_323152_) -> p_323151_.writeNullable(p_323152_.chatSession, RemoteChatSession.Data::write)
        ),
        UPDATE_GAME_MODE(
            (p_436529_, p_436530_) -> p_436529_.gameMode = GameType.byId(p_436530_.readVarInt()),
            (p_323157_, p_323158_) -> p_323157_.writeVarInt(p_323158_.gameMode().getId())
        ),
        UPDATE_LISTED(
            (p_323167_, p_323168_) -> p_323167_.listed = p_323168_.readBoolean(), (p_323171_, p_323172_) -> p_323171_.writeBoolean(p_323172_.listed())
        ),
        UPDATE_LATENCY(
            (p_436525_, p_436526_) -> p_436525_.latency = p_436526_.readVarInt(), (p_323153_, p_323154_) -> p_323153_.writeVarInt(p_323154_.latency())
        ),
        UPDATE_DISPLAY_NAME(
            (p_329878_, p_329879_) -> p_329878_.displayName = FriendlyByteBuf.readNullable(p_329879_, ComponentSerialization.TRUSTED_STREAM_CODEC),
            (p_329876_, p_329877_) -> FriendlyByteBuf.writeNullable(p_329876_, p_329877_.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC)
        ),
        UPDATE_LIST_ORDER(
            (p_436527_, p_436528_) -> p_436527_.listOrder = p_436528_.readVarInt(), (p_359474_, p_359475_) -> p_359474_.writeVarInt(p_359475_.listOrder)
        ),
        UPDATE_HAT((p_382658_, p_382659_) -> p_382658_.showHat = p_382659_.readBoolean(), (p_382660_, p_382661_) -> p_382660_.writeBoolean(p_382661_.showHat));

        final ClientboundPlayerInfoUpdatePacket.Action.Reader reader;
        final ClientboundPlayerInfoUpdatePacket.Action.Writer writer;

        private Action(ClientboundPlayerInfoUpdatePacket.Action.Reader reader, ClientboundPlayerInfoUpdatePacket.Action.Writer writer) {
            this.reader = reader;
            this.writer = writer;
        }

        public interface Reader {
            void read(ClientboundPlayerInfoUpdatePacket.EntryBuilder entryBuilder, RegistryFriendlyByteBuf buffer);
        }

        public interface Writer {
            void write(RegistryFriendlyByteBuf buffer, ClientboundPlayerInfoUpdatePacket.Entry entry);
        }
    }

    public record Entry(
        UUID profileId,
        @Nullable GameProfile profile,
        boolean listed,
        int latency,
        GameType gameMode,
        @Nullable Component displayName,
        boolean showHat,
        int listOrder,
        @Nullable RemoteChatSession.Data chatSession
    ) {
        Entry(ServerPlayer p_252094_) {
            this(
                p_252094_.getUUID(),
                p_252094_.getGameProfile(),
                true,
                p_252094_.connection.latency(),
                p_252094_.gameMode(),
                p_252094_.getTabListDisplayName(),
                p_252094_.isModelPartShown(PlayerModelPart.HAT),
                p_252094_.getTabListOrder(),
                Optionull.map(p_252094_.getChatSession(), RemoteChatSession::asData)
            );
        }
    }

    static class EntryBuilder {
        final UUID profileId;
        @Nullable
        GameProfile profile;
        boolean listed;
        int latency;
        GameType gameMode = GameType.DEFAULT_MODE;
        @Nullable
        Component displayName;
        boolean showHat;
        int listOrder;
        @Nullable
        RemoteChatSession.Data chatSession;

        EntryBuilder(UUID profileId) {
            this.profileId = profileId;
        }

        ClientboundPlayerInfoUpdatePacket.Entry build() {
            return new ClientboundPlayerInfoUpdatePacket.Entry(
                this.profileId, this.profile, this.listed, this.latency, this.gameMode, this.displayName, this.showHat, this.listOrder, this.chatSession
            );
        }
    }
}
