package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.util.GsonHelper;

public class LegacyTextFilter extends ServerTextFilter {
    private static final String ENDPOINT = "v1/chat";
    final URL joinEndpoint;
    final LegacyTextFilter.JoinOrLeaveEncoder joinEncoder;
    final URL leaveEndpoint;
    final LegacyTextFilter.JoinOrLeaveEncoder leaveEncoder;
    private final String authKey;

    private LegacyTextFilter(
        URL chatEndpoint,
        ServerTextFilter.MessageEncoder chatEncoder,
        URL joinEndpoint,
        LegacyTextFilter.JoinOrLeaveEncoder joinEncoder,
        URL leaveEndpoint,
        LegacyTextFilter.JoinOrLeaveEncoder leaveEncoder,
        String authKey,
        ServerTextFilter.IgnoreStrategy chatIgnoreStrategy,
        ExecutorService workerPool
    ) {
        super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
        this.joinEndpoint = joinEndpoint;
        this.joinEncoder = joinEncoder;
        this.leaveEndpoint = leaveEndpoint;
        this.leaveEncoder = leaveEncoder;
        this.authKey = authKey;
    }

    @Nullable
    public static ServerTextFilter createTextFilterFromConfig(String config) {
        try {
            JsonObject jsonobject = GsonHelper.parse(config);
            URI uri = new URI(GsonHelper.getAsString(jsonobject, "apiServer"));
            String s = GsonHelper.getAsString(jsonobject, "apiKey");
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Missing API key");
            } else {
                int i = GsonHelper.getAsInt(jsonobject, "ruleId", 1);
                String s1 = GsonHelper.getAsString(jsonobject, "serverId", "");
                String s2 = GsonHelper.getAsString(jsonobject, "roomId", "Java:Chat");
                int j = GsonHelper.getAsInt(jsonobject, "hashesToDrop", -1);
                int k = GsonHelper.getAsInt(jsonobject, "maxConcurrentRequests", 7);
                JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "endpoints", null);
                String s3 = getEndpointFromConfig(jsonobject1, "chat", "v1/chat");
                boolean flag = s3.equals("v1/chat");
                URL url = uri.resolve("/" + s3).toURL();
                URL url1 = getEndpoint(uri, jsonobject1, "join", "v1/join");
                URL url2 = getEndpoint(uri, jsonobject1, "leave", "v1/leave");
                LegacyTextFilter.JoinOrLeaveEncoder legacytextfilter$joinorleaveencoder = p_442419_ -> {
                    JsonObject jsonobject2 = new JsonObject();
                    jsonobject2.addProperty("server", s1);
                    jsonobject2.addProperty("room", s2);
                    jsonobject2.addProperty("user_id", p_442419_.id().toString());
                    jsonobject2.addProperty("user_display_name", p_442419_.name());
                    return jsonobject2;
                };
                ServerTextFilter.MessageEncoder servertextfilter$messageencoder;
                if (flag) {
                    servertextfilter$messageencoder = (p_442415_, p_442416_) -> {
                        JsonObject jsonobject2 = new JsonObject();
                        jsonobject2.addProperty("rule", i);
                        jsonobject2.addProperty("server", s1);
                        jsonobject2.addProperty("room", s2);
                        jsonobject2.addProperty("player", p_442415_.id().toString());
                        jsonobject2.addProperty("player_display_name", p_442415_.name());
                        jsonobject2.addProperty("text", p_442416_);
                        jsonobject2.addProperty("language", "*");
                        return jsonobject2;
                    };
                } else {
                    String s4 = String.valueOf(i);
                    servertextfilter$messageencoder = (p_442423_, p_442424_) -> {
                        JsonObject jsonobject2 = new JsonObject();
                        jsonobject2.addProperty("rule_id", s4);
                        jsonobject2.addProperty("category", s1);
                        jsonobject2.addProperty("subcategory", s2);
                        jsonobject2.addProperty("user_id", p_442423_.id().toString());
                        jsonobject2.addProperty("user_display_name", p_442423_.name());
                        jsonobject2.addProperty("text", p_442424_);
                        jsonobject2.addProperty("language", "*");
                        return jsonobject2;
                    };
                }

                ServerTextFilter.IgnoreStrategy servertextfilter$ignorestrategy = ServerTextFilter.IgnoreStrategy.select(j);
                ExecutorService executorservice = createWorkerPool(k);
                String s5 = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.US_ASCII));
                return new LegacyTextFilter(
                    url,
                    servertextfilter$messageencoder,
                    url1,
                    legacytextfilter$joinorleaveencoder,
                    url2,
                    legacytextfilter$joinorleaveencoder,
                    s5,
                    servertextfilter$ignorestrategy,
                    executorservice
                );
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse chat filter config {}", config, exception);
            return null;
        }
    }

    @Override
    public TextFilter createContext(GameProfile p_profile) {
        return new ServerTextFilter.PlayerContext(p_profile) {
            @Override
            public void join() {
                LegacyTextFilter.this.processJoinOrLeave(
                    this.profile, LegacyTextFilter.this.joinEndpoint, LegacyTextFilter.this.joinEncoder, this.streamExecutor
                );
            }

            @Override
            public void leave() {
                LegacyTextFilter.this.processJoinOrLeave(
                    this.profile, LegacyTextFilter.this.leaveEndpoint, LegacyTextFilter.this.leaveEncoder, this.streamExecutor
                );
            }
        };
    }

    void processJoinOrLeave(GameProfile profile, URL endpoint, LegacyTextFilter.JoinOrLeaveEncoder encoder, Executor streamExecutor) {
        streamExecutor.execute(() -> {
            JsonObject jsonobject = encoder.encode(profile);

            try {
                this.processRequest(jsonobject, endpoint);
            } catch (Exception exception) {
                LOGGER.warn("Failed to send join/leave packet to {} for player {}", endpoint, profile, exception);
            }
        });
    }

    private void processRequest(JsonObject request, URL endpoint) throws IOException {
        HttpURLConnection httpurlconnection = this.makeRequest(request, endpoint);

        try (InputStream inputstream = httpurlconnection.getInputStream()) {
            this.drainStream(inputstream);
        }
    }

    @Override
    protected void setAuthorizationProperty(HttpURLConnection connection) {
        connection.setRequestProperty("Authorization", "Basic " + this.authKey);
    }

    @Override
    protected FilteredText filterText(String text, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject response) {
        boolean flag = GsonHelper.getAsBoolean(response, "response", false);
        if (flag) {
            return FilteredText.passThrough(text);
        } else {
            String s = GsonHelper.getAsString(response, "hashed", null);
            if (s == null) {
                return FilteredText.fullyFiltered(text);
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(response, "hashes");
                FilterMask filtermask = this.parseMask(text, jsonarray, ignoreStrategy);
                return new FilteredText(text, filtermask);
            }
        }
    }

    @FunctionalInterface
    interface JoinOrLeaveEncoder {
        JsonObject encode(GameProfile profile);
    }
}
