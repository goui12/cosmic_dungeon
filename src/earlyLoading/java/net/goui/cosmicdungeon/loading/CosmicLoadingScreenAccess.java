package net.goui.cosmicdungeon.loading;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.render.LoadingScreenRenderer;
import net.neoforged.fml.earlydisplay.render.elements.RenderElement;

/** Isolates the two FML 10.0.32 internals needed for background ordering. */
public final class CosmicLoadingScreenAccess {
    public static final String BACKGROUND_ID = "cosmicBackground";
    private final Field schedulerField;
    private final Field elementsField;

    public CosmicLoadingScreenAccess() {
        try {
            schedulerField = DisplayWindow.class.getDeclaredField("renderScheduler");
            elementsField = LoadingScreenRenderer.class.getDeclaredField("elements");
            if (schedulerField.getType() != ScheduledExecutorService.class
                    || elementsField.getType() != List.class
                    || !schedulerField.trySetAccessible() || !elementsField.trySetAccessible()) {
                throw new IllegalStateException("Unsupported NeoForge early-loading internals");
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cosmic loading background requires FML 10.0.32 compatibility", e);
        }
    }

    void wrapRendererConstruction(DisplayWindow window) {
        try {
            var scheduler = (ScheduledExecutorService) schedulerField.get(window);
            schedulerField.set(window, new CosmicLoadingScheduler(scheduler, this::rendererCreated));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot prepare Cosmic loading background", e);
        }
    }

    private void rendererCreated(Object value) {
        if (!(value instanceof LoadingScreenRenderer renderer)) {
            return;
        }
        try {
            // Runs on the same executor immediately after construction, before its first frame.
            @SuppressWarnings("unchecked")
            var elements = (List<RenderElement>) elementsField.get(renderer);
            placeBackgroundFirst(elements, RenderElement::id);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot order Cosmic loading background", e);
        }
    }

    public <T> void placeBackgroundFirst(List<T> elements, Function<T, String> identifier) {
        for (int index = 0; index < elements.size(); index++) {
            if (BACKGROUND_ID.equals(identifier.apply(elements.get(index)))) {
                if (index != 0) {
                    elements.addFirst(elements.remove(index));
                }
                return;
            }
        }
        // Another selected theme may have no Cosmic background; preserve its native order.
    }
}
