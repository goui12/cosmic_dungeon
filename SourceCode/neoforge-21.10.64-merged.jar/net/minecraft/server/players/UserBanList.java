package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserBanList extends StoredUserList<NameAndId, UserBanListEntry> {
    public UserBanList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject entryData) {
        return new UserBanListEntry(entryData);
    }

    public boolean isBanned(NameAndId user) {
        return this.contains(user);
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

    public boolean add(UserBanListEntry entry) {
        if (super.add(entry)) {
            if (entry.getUser() != null) {
                this.notificationService.playerBanned(entry);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(NameAndId key) {
        if (super.remove(key)) {
            this.notificationService.playerUnbanned(key);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (UserBanListEntry userbanlistentry : this.getEntries()) {
            if (userbanlistentry.getUser() != null) {
                this.notificationService.playerUnbanned(userbanlistentry.getUser());
            }
        }

        super.clear();
    }
}
