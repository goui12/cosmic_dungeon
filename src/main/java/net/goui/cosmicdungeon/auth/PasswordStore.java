package net.goui.cosmicdungeon.auth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PasswordStore extends SavedData {

    public static final String SAVE_ID = "cosmicdungeon_dev_password";
    private static final String DEFAULT_PASSWORD = "changeme";

    public static final Codec<PasswordStore> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("password", DEFAULT_PASSWORD).forGetter(s -> s.password)
    ).apply(inst, PasswordStore::fromCodec));

    public static final SavedDataType<PasswordStore> TYPE = new SavedDataType<>(
            SAVE_ID,
            PasswordStore::new,
            CODEC
    );

    private String password = DEFAULT_PASSWORD;

    public PasswordStore() {}

    private static PasswordStore fromCodec(String password) {
        PasswordStore s = new PasswordStore();
        s.password = (password == null || password.isBlank()) ? DEFAULT_PASSWORD : password;
        return s;
    }

    public static PasswordStore get(MinecraftServer server) {
        final ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public static PasswordStore get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }



    public String getPassword() {
        return password;
    }

    public void setPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) return;
        this.password = newPassword;
        setDirty();
    }

    public boolean matches(String candidate) {
        if (candidate == null) return false;
        return candidate.equals(password);
    }
}
