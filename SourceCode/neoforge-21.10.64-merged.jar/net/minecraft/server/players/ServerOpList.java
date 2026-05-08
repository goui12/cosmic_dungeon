package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class ServerOpList extends StoredUserList<NameAndId, ServerOpListEntry> {
    public ServerOpList(File file, NotificationService notificationService) {
        super(file, notificationService);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject entryData) {
        return new ServerOpListEntry(entryData);
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    public boolean add(ServerOpListEntry entry) {
        if (super.add(entry)) {
            if (entry.getUser() != null) {
                this.notificationService.playerOped(entry);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(NameAndId key) {
        ServerOpListEntry serveroplistentry = this.get(key);
        if (super.remove(key)) {
            if (serveroplistentry != null) {
                this.notificationService.playerDeoped(serveroplistentry);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (ServerOpListEntry serveroplistentry : this.getEntries()) {
            if (serveroplistentry.getUser() != null) {
                this.notificationService.playerDeoped(serveroplistentry);
            }
        }

        super.clear();
    }

    public boolean canBypassPlayerLimit(NameAndId nameAndId) {
        ServerOpListEntry serveroplistentry = this.get(nameAndId);
        return serveroplistentry != null ? serveroplistentry.getBypassesPlayerLimit() : false;
    }

    /**
     * Gets the key value for the given object
     */
    protected String getKeyForUser(NameAndId obj) {
        return obj.id().toString();
    }
}
