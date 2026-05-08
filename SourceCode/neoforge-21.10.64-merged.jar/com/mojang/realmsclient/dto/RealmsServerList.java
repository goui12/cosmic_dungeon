package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServerList extends ValueObject implements ReflectionBasedSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();
    @SerializedName("servers")
    public List<RealmsServer> servers = new ArrayList<>();

    public static RealmsServerList parse(GuardedSerializer serializer, String json) {
        try {
            RealmsServerList realmsserverlist = serializer.fromJson(json, RealmsServerList.class);
            if (realmsserverlist == null) {
                LOGGER.error("Could not parse McoServerList: {}", json);
                return new RealmsServerList();
            } else {
                realmsserverlist.servers.forEach(RealmsServer::finalize);
                return realmsserverlist;
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse McoServerList: {}", exception.getMessage());
            return new RealmsServerList();
        }
    }
}
