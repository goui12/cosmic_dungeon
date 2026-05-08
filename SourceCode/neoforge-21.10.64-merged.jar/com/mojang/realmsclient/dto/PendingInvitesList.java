package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PendingInvitesList extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    public List<PendingInvite> pendingInvites = Lists.newArrayList();

    public static PendingInvitesList parse(String json) {
        PendingInvitesList pendinginviteslist = new PendingInvitesList();

        try {
            JsonObject jsonobject = LenientJsonParser.parse(json).getAsJsonObject();
            if (jsonobject.get("invites").isJsonArray()) {
                for (JsonElement jsonelement : jsonobject.get("invites").getAsJsonArray()) {
                    pendinginviteslist.pendingInvites.add(PendingInvite.parse(jsonelement.getAsJsonObject()));
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse PendingInvitesList: {}", exception.getMessage());
        }

        return pendinginviteslist;
    }
}
