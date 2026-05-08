package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.server.players.ProfileResolver;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocalPlayerResolver implements ProfileResolver {
    private final Minecraft minecraft;
    private final ProfileResolver parentResolver;

    public LocalPlayerResolver(Minecraft minecraft, ProfileResolver parentResolver) {
        this.minecraft = minecraft;
        this.parentResolver = parentResolver;
    }

    @Override
    public Optional<GameProfile> fetchByName(String name) {
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            PlayerInfo playerinfo = clientpacketlistener.getPlayerInfoIgnoreCase(name);
            if (playerinfo != null) {
                return Optional.of(playerinfo.getProfile());
            }
        }

        return this.parentResolver.fetchByName(name);
    }

    @Override
    public Optional<GameProfile> fetchById(UUID id) {
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            PlayerInfo playerinfo = clientpacketlistener.getPlayerInfo(id);
            if (playerinfo != null) {
                return Optional.of(playerinfo.getProfile());
            }
        }

        return this.parentResolver.fetchById(id);
    }
}
