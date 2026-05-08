package net.minecraft.server.players;

import com.google.gson.JsonObject;

public class UserWhiteListEntry extends StoredUserEntry<NameAndId> {
    public UserWhiteListEntry(NameAndId user) {
        super(user);
    }

    public UserWhiteListEntry(JsonObject entryData) {
        super(NameAndId.fromJson(entryData));
    }

    @Override
    protected void serialize(JsonObject data) {
        if (this.getUser() != null) {
            this.getUser().appendTo(data);
        }
    }
}
