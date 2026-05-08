package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCertificate;
import com.mojang.authlib.GameProfile;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;

public class PlayerSafetyServiceTextFilter extends ServerTextFilter {
    private final ConfidentialClientApplication client;
    private final ClientCredentialParameters clientParameters;
    private final Set<String> fullyFilteredEvents;
    private final int connectionReadTimeoutMs;

    private PlayerSafetyServiceTextFilter(
        URL chatEndpoint,
        ServerTextFilter.MessageEncoder chatEncoder,
        ServerTextFilter.IgnoreStrategy chatIgnoreStrategy,
        ExecutorService workerPool,
        ConfidentialClientApplication client,
        ClientCredentialParameters clientParameters,
        Set<String> fullyFilteredEvents,
        int connectionReadTimeoutMs
    ) {
        super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
        this.client = client;
        this.clientParameters = clientParameters;
        this.fullyFilteredEvents = fullyFilteredEvents;
        this.connectionReadTimeoutMs = connectionReadTimeoutMs;
    }

    @Nullable
    public static ServerTextFilter createTextFilterFromConfig(String config) {
        JsonObject jsonobject = GsonHelper.parse(config);
        URI uri = URI.create(GsonHelper.getAsString(jsonobject, "apiServer"));
        String s = GsonHelper.getAsString(jsonobject, "apiPath");
        String s1 = GsonHelper.getAsString(jsonobject, "scope");
        String s2 = GsonHelper.getAsString(jsonobject, "serverId", "");
        String s3 = GsonHelper.getAsString(jsonobject, "applicationId");
        String s4 = GsonHelper.getAsString(jsonobject, "tenantId");
        String s5 = GsonHelper.getAsString(jsonobject, "roomId", "Java:Chat");
        String s6 = GsonHelper.getAsString(jsonobject, "certificatePath");
        String s7 = GsonHelper.getAsString(jsonobject, "certificatePassword", "");
        int i = GsonHelper.getAsInt(jsonobject, "hashesToDrop", -1);
        int j = GsonHelper.getAsInt(jsonobject, "maxConcurrentRequests", 7);
        JsonArray jsonarray = GsonHelper.getAsJsonArray(jsonobject, "fullyFilteredEvents");
        Set<String> set = new HashSet<>();
        jsonarray.forEach(p_361597_ -> set.add(GsonHelper.convertToString(p_361597_, "filteredEvent")));
        int k = GsonHelper.getAsInt(jsonobject, "connectionReadTimeoutMs", 2000);

        URL url;
        try {
            url = uri.resolve(s).toURL();
        } catch (MalformedURLException malformedurlexception) {
            throw new RuntimeException(malformedurlexception);
        }

        ServerTextFilter.MessageEncoder servertextfilter$messageencoder = (p_442427_, p_442428_) -> {
            JsonObject jsonobject1 = new JsonObject();
            jsonobject1.addProperty("userId", p_442427_.id().toString());
            jsonobject1.addProperty("userDisplayName", p_442427_.name());
            jsonobject1.addProperty("server", s2);
            jsonobject1.addProperty("room", s5);
            jsonobject1.addProperty("area", "JavaChatRealms");
            jsonobject1.addProperty("data", p_442428_);
            jsonobject1.addProperty("language", "*");
            return jsonobject1;
        };
        ServerTextFilter.IgnoreStrategy servertextfilter$ignorestrategy = ServerTextFilter.IgnoreStrategy.select(i);
        ExecutorService executorservice = createWorkerPool(j);

        IClientCertificate iclientcertificate;
        try (InputStream inputstream = Files.newInputStream(Path.of(s6))) {
            iclientcertificate = ClientCredentialFactory.createFromCertificate(inputstream, s7);
        } catch (Exception exception1) {
            LOGGER.warn("Failed to open certificate file");
            return null;
        }

        ConfidentialClientApplication confidentialclientapplication;
        try {
            confidentialclientapplication = ConfidentialClientApplication.builder(s3, iclientcertificate)
                .sendX5c(true)
                .executorService(executorservice)
                .authority(String.format(Locale.ROOT, "https://login.microsoftonline.com/%s/", s4))
                .build();
        } catch (Exception exception) {
            LOGGER.warn("Failed to create confidential client application");
            return null;
        }

        ClientCredentialParameters clientcredentialparameters = ClientCredentialParameters.builder(Set.of(s1)).build();
        return new PlayerSafetyServiceTextFilter(
            url,
            servertextfilter$messageencoder,
            servertextfilter$ignorestrategy,
            executorservice,
            confidentialclientapplication,
            clientcredentialparameters,
            set,
            k
        );
    }

    private IAuthenticationResult aquireIAuthenticationResult() {
        return this.client.acquireToken(this.clientParameters).join();
    }

    @Override
    protected void setAuthorizationProperty(HttpURLConnection connection) {
        IAuthenticationResult iauthenticationresult = this.aquireIAuthenticationResult();
        connection.setRequestProperty("Authorization", "Bearer " + iauthenticationresult.accessToken());
    }

    @Override
    protected FilteredText filterText(String text, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject response) {
        JsonObject jsonobject = GsonHelper.getAsJsonObject(response, "result", null);
        if (jsonobject == null) {
            return FilteredText.fullyFiltered(text);
        } else {
            boolean flag = GsonHelper.getAsBoolean(jsonobject, "filtered", true);
            if (!flag) {
                return FilteredText.passThrough(text);
            } else {
                for (JsonElement jsonelement : GsonHelper.getAsJsonArray(jsonobject, "events", new JsonArray())) {
                    JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                    String s = GsonHelper.getAsString(jsonobject1, "id", "");
                    if (this.fullyFilteredEvents.contains(s)) {
                        return FilteredText.fullyFiltered(text);
                    }
                }

                JsonArray jsonarray = GsonHelper.getAsJsonArray(jsonobject, "redactedTextIndex", new JsonArray());
                return new FilteredText(text, this.parseMask(text, jsonarray, ignoreStrategy));
            }
        }
    }

    @Override
    protected int connectionReadTimeout() {
        return this.connectionReadTimeoutMs;
    }
}
