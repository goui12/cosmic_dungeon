package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String prefix, String extension) {
        this.prefix = prefix;
        this.extension = extension;
    }

    public static FileToIdConverter json(String name) {
        return new FileToIdConverter(name, ".json");
    }

    public static FileToIdConverter registry(ResourceKey<? extends Registry<?>> registryKey) {
        return json(Registries.elementsDirPath(registryKey));
    }

    public ResourceLocation idToFile(ResourceLocation id) {
        return id.withPath(this.prefix + "/" + id.getPath() + this.extension);
    }

    public ResourceLocation fileToId(ResourceLocation file) {
        String s = file.getPath();
        return file.withPath(s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
    }

    public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager resourceManager) {
        return resourceManager.listResources(this.prefix, p_251986_ -> p_251986_.getPath().endsWith(this.extension));
    }

    public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager resourceManager) {
        return resourceManager.listResourceStacks(this.prefix, p_248700_ -> p_248700_.getPath().endsWith(this.extension));
    }

    /**
     * List all resources under the given namespace which match this converter
     *
     * @param manager   The resource manager to collect the resources from
     * @param namespace The namespace to search under
     * @return All resources from the given namespace which match this converter
     */
    public Map<ResourceLocation, Resource> listMatchingResourcesFromNamespace(ResourceManager manager, String namespace) {
        return manager.listResources(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(this.extension));
    }

    /**
     * List all resource stacks under the given namespace which match this converter
     *
     * @param manager   The resource manager to collect the resources from
     * @param namespace The namespace to search under
     * @return All resource stacks from the given namespace which match this converter
     */
    public Map<ResourceLocation, List<Resource>> listMatchingResourceStacksFromNamespace(ResourceManager manager, String namespace) {
        return manager.listResourceStacks(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(this.extension));
    }
}
