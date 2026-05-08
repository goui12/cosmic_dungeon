package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StringUtil;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.slf4j.Logger;

public abstract class ServerTextFilter implements AutoCloseable {
    protected static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static final ThreadFactory THREAD_FACTORY = p_361370_ -> {
        Thread thread = new Thread(p_361370_);
        thread.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
        return thread;
    };
    private final URL chatEndpoint;
    private final ServerTextFilter.MessageEncoder chatEncoder;
    final ServerTextFilter.IgnoreStrategy chatIgnoreStrategy;
    final ExecutorService workerPool;

    protected static ExecutorService createWorkerPool(int size) {
        return Executors.newFixedThreadPool(size, THREAD_FACTORY);
    }

    protected ServerTextFilter(URL chatEndpoint, ServerTextFilter.MessageEncoder chatEncoder, ServerTextFilter.IgnoreStrategy chatIgnoreStrategy, ExecutorService workerPool) {
        this.chatIgnoreStrategy = chatIgnoreStrategy;
        this.workerPool = workerPool;
        this.chatEndpoint = chatEndpoint;
        this.chatEncoder = chatEncoder;
    }

    protected static URL getEndpoint(URI apiServer, @Nullable JsonObject json, String key, String fallback) throws MalformedURLException {
        String s = getEndpointFromConfig(json, key, fallback);
        return apiServer.resolve("/" + s).toURL();
    }

    protected static String getEndpointFromConfig(@Nullable JsonObject json, String key, String fallback) {
        return json != null ? GsonHelper.getAsString(json, key, fallback) : fallback;
    }

    @Nullable
    public static ServerTextFilter createFromConfig(DedicatedServerProperties config) {
        String s = config.textFilteringConfig;
        if (StringUtil.isBlank(s)) {
            return null;
        } else {
            return switch (config.textFilteringVersion) {
                case 0 -> LegacyTextFilter.createTextFilterFromConfig(s);
                case 1 -> PlayerSafetyServiceTextFilter.createTextFilterFromConfig(s);
                default -> {
                    LOGGER.warn("Could not create text filter - unsupported text filtering version used");
                    yield null;
                }
            };
        }
    }

    protected CompletableFuture<FilteredText> requestMessageProcessing(
        GameProfile profile, String filter, ServerTextFilter.IgnoreStrategy chatIgnoreStrategy, Executor streamExecutor
    ) {
        return filter.isEmpty() ? CompletableFuture.completedFuture(FilteredText.EMPTY) : CompletableFuture.supplyAsync(() -> {
            JsonObject jsonobject = this.chatEncoder.encode(profile, filter);

            try {
                JsonObject jsonobject1 = this.processRequestResponse(jsonobject, this.chatEndpoint);
                return this.filterText(filter, chatIgnoreStrategy, jsonobject1);
            } catch (Exception exception) {
                LOGGER.warn("Failed to validate message '{}'", filter, exception);
                return FilteredText.fullyFiltered(filter);
            }
        }, streamExecutor);
    }

    protected abstract FilteredText filterText(String text, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject response);

    protected FilterMask parseMask(String text, JsonArray hashes, ServerTextFilter.IgnoreStrategy ignoreStrategy) {
        if (hashes.isEmpty()) {
            return FilterMask.PASS_THROUGH;
        } else if (ignoreStrategy.shouldIgnore(text, hashes.size())) {
            return FilterMask.FULLY_FILTERED;
        } else {
            FilterMask filtermask = new FilterMask(text.length());

            for (int i = 0; i < hashes.size(); i++) {
                filtermask.setFiltered(hashes.get(i).getAsInt());
            }

            return filtermask;
        }
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
    }

    protected void drainStream(InputStream stream) throws IOException {
        byte[] abyte = new byte[1024];

        while (stream.read(abyte) != -1) {
        }
    }

    private JsonObject processRequestResponse(JsonObject request, URL endpoint) throws IOException {
        HttpURLConnection httpurlconnection = this.makeRequest(request, endpoint);

        JsonObject jsonobject;
        try (InputStream inputstream = httpurlconnection.getInputStream()) {
            if (httpurlconnection.getResponseCode() == 204) {
                return new JsonObject();
            }

            try {
                jsonobject = LenientJsonParser.parse(new InputStreamReader(inputstream, StandardCharsets.UTF_8)).getAsJsonObject();
            } finally {
                this.drainStream(inputstream);
            }
        }

        return jsonobject;
    }

    protected HttpURLConnection makeRequest(JsonObject request, URL endpoint) throws IOException {
        HttpURLConnection httpurlconnection = this.getURLConnection(endpoint);
        this.setAuthorizationProperty(httpurlconnection);
        OutputStreamWriter outputstreamwriter = new OutputStreamWriter(httpurlconnection.getOutputStream(), StandardCharsets.UTF_8);

        try (JsonWriter jsonwriter = new JsonWriter(outputstreamwriter)) {
            Streams.write(request, jsonwriter);
        } catch (Throwable throwable1) {
            try {
                outputstreamwriter.close();
            } catch (Throwable throwable) {
                throwable1.addSuppressed(throwable);
            }

            throw throwable1;
        }

        outputstreamwriter.close();
        int i = httpurlconnection.getResponseCode();
        if (i >= 200 && i < 300) {
            return httpurlconnection;
        } else {
            throw new ServerTextFilter.RequestFailedException(i + " " + httpurlconnection.getResponseMessage());
        }
    }

    protected abstract void setAuthorizationProperty(HttpURLConnection connection);

    protected int connectionReadTimeout() {
        return 2000;
    }

    protected HttpURLConnection getURLConnection(URL url) throws IOException {
        HttpURLConnection httpurlconnection = (HttpURLConnection)url.openConnection();
        httpurlconnection.setConnectTimeout(15000);
        httpurlconnection.setReadTimeout(this.connectionReadTimeout());
        httpurlconnection.setUseCaches(false);
        httpurlconnection.setDoOutput(true);
        httpurlconnection.setDoInput(true);
        httpurlconnection.setRequestMethod("POST");
        httpurlconnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        httpurlconnection.setRequestProperty("Accept", "application/json");
        httpurlconnection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().name());
        return httpurlconnection;
    }

    public TextFilter createContext(GameProfile profile) {
        return new ServerTextFilter.PlayerContext(profile);
    }

    @FunctionalInterface
    public interface IgnoreStrategy {
        ServerTextFilter.IgnoreStrategy NEVER_IGNORE = (p_361637_, p_365001_) -> false;
        ServerTextFilter.IgnoreStrategy IGNORE_FULLY_FILTERED = (p_361951_, p_362976_) -> p_361951_.length() == p_362976_;

        static ServerTextFilter.IgnoreStrategy ignoreOverThreshold(int threshold) {
            return (p_363255_, p_363924_) -> p_363924_ >= threshold;
        }

        static ServerTextFilter.IgnoreStrategy select(int threshold) {
            return switch (threshold) {
                case -1 -> NEVER_IGNORE;
                case 0 -> IGNORE_FULLY_FILTERED;
                default -> ignoreOverThreshold(threshold);
            };
        }

        boolean shouldIgnore(String text, int numHashes);
    }

    @FunctionalInterface
    protected interface MessageEncoder {
        JsonObject encode(GameProfile profile, String message);
    }

    protected class PlayerContext implements TextFilter {
        protected final GameProfile profile;
        protected final Executor streamExecutor;

        protected PlayerContext(GameProfile profile) {
            this.profile = profile;
            ConsecutiveExecutor consecutiveexecutor = new ConsecutiveExecutor(ServerTextFilter.this.workerPool, "chat stream for " + profile.name());
            this.streamExecutor = consecutiveexecutor::schedule;
        }

        @Override
        public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> texts) {
            List<CompletableFuture<FilteredText>> list = texts.stream()
                .map(
                    p_361744_ -> ServerTextFilter.this.requestMessageProcessing(
                        this.profile, p_361744_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor
                    )
                )
                .collect(ImmutableList.toImmutableList());
            return Util.sequenceFailFast(list).exceptionally(p_365427_ -> ImmutableList.of());
        }

        @Override
        public CompletableFuture<FilteredText> processStreamMessage(String text) {
            return ServerTextFilter.this.requestMessageProcessing(this.profile, text, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor);
        }
    }

    protected static class RequestFailedException extends RuntimeException {
        protected RequestFailedException(String message) {
            super(message);
        }
    }
}
