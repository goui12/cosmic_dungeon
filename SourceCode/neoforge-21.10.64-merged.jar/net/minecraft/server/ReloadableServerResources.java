package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

public class ReloadableServerResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private final ReloadableServerRegistries.Holder fullRegistryHolder;
    private final Commands commands;
    private final RecipeManager recipes;
    private final ServerAdvancementManager advancements;
    private final ServerFunctionLibrary functionLibrary;
    private final List<Registry.PendingTags<?>> postponedTags;

    private ReloadableServerResources(
        LayeredRegistryAccess<RegistryLayer> registryAccess,
        HolderLookup.Provider registries,
        FeatureFlagSet enabledFeatures,
        Commands.CommandSelection commandSelection,
        List<Registry.PendingTags<?>> postponedTags,
        int functionCompilationLevel
    ) {
        this.fullRegistryHolder = new ReloadableServerRegistries.Holder(registryAccess.compositeAccess());
        this.postponedTags = postponedTags;
        this.recipes = new RecipeManager(registries);
        this.commands = new Commands(commandSelection, CommandBuildContext.simple(registries, enabledFeatures));
        this.advancements = new ServerAdvancementManager(registries);
        this.functionLibrary = new ServerFunctionLibrary(functionCompilationLevel, this.commands.getDispatcher());
        // Neo: Store registries and create context object
        this.registryLookup = registries;
        this.context = new net.neoforged.neoforge.common.conditions.ConditionContext(this.postponedTags, registryAccess.compositeAccess(), enabledFeatures);
    }

    public ServerFunctionLibrary getFunctionLibrary() {
        return this.functionLibrary;
    }

    public ReloadableServerRegistries.Holder fullRegistries() {
        return this.fullRegistryHolder;
    }

    public RecipeManager getRecipeManager() {
        return this.recipes;
    }

    public Commands getCommands() {
        return this.commands;
    }

    public ServerAdvancementManager getAdvancements() {
        return this.advancements;
    }

    public List<PreparableReloadListener> listeners() {
        return List.of(this.recipes, this.functionLibrary, this.advancements);
    }

    private final HolderLookup.Provider registryLookup;
    private final net.neoforged.neoforge.common.conditions.ConditionContext context;

    /**
     * Exposes the current condition context for usage in other reload listeners.<br>
     * This is not useful outside the reloading stage.
     * @return The condition context for the currently active reload.
     */
    public net.neoforged.neoforge.common.conditions.ICondition.IContext getConditionContext() {
        return this.context;
    }

    /**
      * {@return the lookup provider access for the currently active reload}
      */
    public HolderLookup.Provider getRegistryLookup() {
        return this.registryLookup;
    }

    public static CompletableFuture<ReloadableServerResources> loadResources(
        ResourceManager resourceManager,
        LayeredRegistryAccess<RegistryLayer> registryAccess,
        List<Registry.PendingTags<?>> postponedTags,
        FeatureFlagSet enabledFeatures,
        Commands.CommandSelection commandSelection,
        int functionCompilationLevel,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        return ReloadableServerRegistries.reload(registryAccess, postponedTags, resourceManager, backgroundExecutor)
            .thenCompose(
                p_359514_ -> {
                    ReloadableServerResources reloadableserverresources = new ReloadableServerResources(
                        p_359514_.layers(), p_359514_.lookupWithUpdatedTags(), enabledFeatures, commandSelection, postponedTags, functionCompilationLevel
                    );

                    // Neo: Fire the AddServerReloadListenersEvent and use the resulting listeners instead of the vanilla listener list.
                    List<PreparableReloadListener> listeners = net.neoforged.neoforge.event.EventHooks.onResourceReload(reloadableserverresources, p_359514_.layers().compositeAccess());

                    // Neo: Inject the ConditionContext and RegistryLookup to any resource listener that requests it.
                    for (PreparableReloadListener rl : listeners) {
                        if (rl instanceof net.neoforged.neoforge.resource.ContextAwareReloadListener carl) {
                            carl.injectContext(reloadableserverresources.context, reloadableserverresources.registryLookup);
                        }
                    }

                    return SimpleReloadInstance.create(
                            resourceManager, listeners, backgroundExecutor, gameExecutor, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled()
                        )
                        .done()
                        .thenRun(() -> {
                            // Neo: Clear context after reload completes
                            reloadableserverresources.context.clear();
                            listeners.forEach(rl -> {
                                if (rl instanceof net.neoforged.neoforge.resource.ContextAwareReloadListener srl) {
                                    srl.injectContext(net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY, net.minecraft.core.RegistryAccess.EMPTY);
                                }
                            });
                        })
                        .thenApply(p_214306_ -> reloadableserverresources);
                }
            );
    }

    public void updateStaticRegistryTags() {
        this.postponedTags.forEach(Registry.PendingTags::apply);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.TagsUpdatedEvent(registryLookup, false, false));
    }
}
