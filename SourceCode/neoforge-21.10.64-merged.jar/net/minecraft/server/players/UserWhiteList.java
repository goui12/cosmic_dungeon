package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserWhiteList extends StoredUserList<NameAndId, UserWhiteListEntry> {
    public UserWhiteList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject entryData) {
        return new UserWhiteListEntry(entryData);
    }

    public boolean isWhiteListed(NameAndId user) {
        return this.contains(user);
    }

    public boolean add(UserWhiteListEntry entry) {
        if (super.add(entry)) {
            if (entry.getUser() != null) {
                this.notificationService.playerAddedToAllowlist(entry.getUser());
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(NameAndId key) {
        if (super.remove(key)) {
            this.notificationService.playerRemovedFromAllowlist(key);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (UserWhiteListEntry userwhitelistentry : this.getEntries()) {
            if (userwhitelistentry.getUser() != null) {
                this.notificationService.playerRemovedFromAllowlist(userwhitelistentry.getUser());
            }
        }

        super.clear();
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    /**
     * Gets the key value for the given object
     */
    protected String getKeyForUser(NameAndId obj) {
        return obj.id().toString();
    }
}
