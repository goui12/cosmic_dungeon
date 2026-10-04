package net.goui.cosmicdungeon.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider;

/** Checks native access and first-frame ordering without creating a window or GL context. */
public final class LoadingBackgroundChecks {
    private int checks;

    public void run() throws Exception {
        var access = new CosmicLoadingScreenAccess();
        var nativeOrder = List.of("performance", "startupLog", "progressBars", "version", "fox",
                "cosmicTitle", CosmicLoadingScreenAccess.BACKGROUND_ID, "neoforgeCredit");
        var expected = new ArrayList<>(nativeOrder);
        expected.remove(CosmicLoadingScreenAccess.BACKGROUND_ID);
        expected.addFirst(CosmicLoadingScreenAccess.BACKGROUND_ID);
        var actual = new ArrayList<>(nativeOrder);
        access.placeBackgroundFirst(actual, value -> value);
        require(actual.equals(expected), "Background precedes every native control and credit");
        access.placeBackgroundFirst(actual, value -> value);
        require(actual.equals(expected), "Repeated ordering is stable");
        var noBackground = new ArrayList<>(List.of("progressBars", "fox"));
        access.placeBackgroundFirst(noBackground, value -> value);
        require(noBackground.equals(List.of("progressBars", "fox")), "Other themes retain their order");
        require(ServiceLoader.load(ImmediateWindowProvider.class).stream()
                .anyMatch(provider -> provider.type() == CosmicLoadingWindow.class), "Early service is discoverable");

        var delegate = Executors.newSingleThreadScheduledExecutor();
        var frame = new CompletableFuture<List<String>>();
        var construction = new ArrayList<>(nativeOrder);
        var scheduler = new CosmicLoadingScheduler(delegate,
                ignored -> access.placeBackgroundFirst(construction, value -> value));
        try {
            Object token = new Object();
            var created = scheduler.schedule(() -> {
                // Model NeoForge queuing rendering while its constructor is still running.
                scheduler.schedule(() -> frame.complete(List.copyOf(construction)), 0, TimeUnit.MILLISECONDS);
                return token;
            }, 0, TimeUnit.MILLISECONDS);
            require(created.get(5, TimeUnit.SECONDS) == token, "Native constructor result retained");
            require(frame.get(5, TimeUnit.SECONDS).equals(expected), "Reordered before even an immediate first frame");
            scheduler.shutdown();
            require(delegate.isShutdown(), "Native executor receives shutdown");
            require(scheduler.awaitTermination(5, TimeUnit.SECONDS), "Native executor terminates");
        } finally {
            scheduler.shutdownNow();
        }
        System.out.println("Loading background: " + checks + " offline checks passed; no graphics context created.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
