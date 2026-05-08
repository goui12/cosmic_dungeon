package net.minecraft.client.server;

import com.mojang.logging.LogUtils;
import java.net.SocketAddress;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class IntegratedPlayerList extends PlayerList {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private CompoundTag playerData;

    public IntegratedPlayerList(IntegratedServer server, LayeredRegistryAccess<RegistryLayer> registries, PlayerDataStorage playerIo) {
        super(server, registries, playerIo, server.notificationManager());
        this.setViewDistance(10);
    }

    /**
     * Also stores the NBTTags if this is an IntegratedPlayerList.
     */
    @Override
    protected void save(ServerPlayer player) {
        if (this.getServer().isSingleplayerOwner(player.nameAndId())) {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
                TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, player.registryAccess());
                player.saveWithoutId(tagvalueoutput);
                this.playerData = tagvalueoutput.buildResult();
            }
        }

        super.save(player);
    }

    @Override
    public Component canPlayerLogin(SocketAddress address, NameAndId nameAndId) {
        return (Component)(this.getServer().isSingleplayerOwner(nameAndId) && this.getPlayerByName(nameAndId.name()) != null
            ? Component.translatable("multiplayer.disconnect.name_taken")
            : super.canPlayerLogin(address, nameAndId));
    }

    public IntegratedServer getServer() {
        return (IntegratedServer)super.getServer();
    }

    @Nullable
    @Override
    public CompoundTag getSingleplayerData() {
        return this.playerData;
    }
}
