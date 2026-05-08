package net.minecraft.server.players;

import com.google.gson.JsonObject;

public class ServerOpListEntry extends StoredUserEntry<NameAndId> {
    private final int level;
    private final boolean bypassesPlayerLimit;

    public ServerOpListEntry(NameAndId user, int level, boolean bypassesPlayerLimit) {
        super(user);
        this.level = level;
        this.bypassesPlayerLimit = bypassesPlayerLimit;
    }

    public ServerOpListEntry(JsonObject entryData) {
        super(NameAndId.fromJson(entryData));
        this.level = entryData.has("level") ? entryData.get("level").getAsInt() : 0;
        this.bypassesPlayerLimit = entryData.has("bypassesPlayerLimit") && entryData.get("bypassesPlayerLimit").getAsBoolean();
    }

    public int getLevel() {
        return this.level;
    }

    public boolean getBypassesPlayerLimit() {
        return this.bypassesPlayerLimit;
    }

    @Override
    protected void serialize(JsonObject data) {
        if (this.getUser() != null) {
            this.getUser().appendTo(data);
            data.addProperty("level", this.level);
            data.addProperty("bypassesPlayerLimit", this.bypassesPlayerLimit);
        }
    }
}
