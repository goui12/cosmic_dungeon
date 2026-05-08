package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.SignatureValidator;

public record Services(
    MinecraftSessionService sessionService,
    ServicesKeySet servicesKeySet,
    GameProfileRepository profileRepository,
    UserNameToIdResolver nameToIdCache,
    ProfileResolver profileResolver
) {
    private static final String USERID_CACHE_FILE = "usercache.json";

    public static Services create(YggdrasilAuthenticationService authenticationService, File profileRepository) {
        MinecraftSessionService minecraftsessionservice = authenticationService.createMinecraftSessionService();
        GameProfileRepository gameprofilerepository = authenticationService.createProfileRepository();
        UserNameToIdResolver usernametoidresolver = new CachedUserNameToIdResolver(gameprofilerepository, new File(profileRepository, "usercache.json"));
        ProfileResolver profileresolver = new ProfileResolver.Cached(minecraftsessionservice, usernametoidresolver);
        return new Services(minecraftsessionservice, authenticationService.getServicesKeySet(), gameprofilerepository, usernametoidresolver, profileresolver);
    }

    @Nullable
    public SignatureValidator profileKeySignatureValidator() {
        return SignatureValidator.from(this.servicesKeySet, ServicesKeyType.PROFILE_KEY);
    }

    public boolean canValidateProfileKeys() {
        return !this.servicesKeySet.keys(ServicesKeyType.PROFILE_KEY).isEmpty();
    }
}
