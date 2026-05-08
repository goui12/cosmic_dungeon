package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public class UserBanListEntry extends BanListEntry<NameAndId> {
    private static final Component MESSAGE_UNKNOWN_USER = Component.translatable("commands.banlist.entry.unknown");

    public UserBanListEntry(@Nullable NameAndId user) {
        this(user, null, null, null, null);
    }

    public UserBanListEntry(
        @Nullable NameAndId user, @Nullable Date created, @Nullable String source, @Nullable Date expires, @Nullable String reason
    ) {
        super(user, created, source, expires, reason);
    }

    public UserBanListEntry(JsonObject entryData) {
        super(NameAndId.fromJson(entryData), entryData);
    }

    @Override
    protected void serialize(JsonObject data) {
        if (this.getUser() != null) {
            this.getUser().appendTo(data);
            super.serialize(data);
        }
    }

    @Override
    public Component getDisplayName() {
        NameAndId nameandid = this.getUser();
        return (Component)(nameandid != null ? Component.literal(nameandid.name()) : MESSAGE_UNKNOWN_USER);
    }
}
