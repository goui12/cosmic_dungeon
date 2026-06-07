package net.goui.cosmicdungeon.vendor;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VendorProfileResolver {
    private VendorProfileResolver() {}

    public static Result resolve(String raw) {
        if (raw == null || raw.isBlank()) return Result.unknown(raw == null ? "" : raw);

        ResourceLocation exact = ResourceLocation.tryParse(raw);
        if (exact != null && VendorProfileManager.INSTANCE.get(exact) != null) return Result.resolved(exact);
        if (exact == null && (raw.contains(":") || raw.contains("/"))) return Result.invalid(raw);

        List<ResourceLocation> matches = VendorProfileManager.INSTANCE.listProfileIds().stream()
                .filter(id -> shortName(id).equals(raw))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        if (matches.size() == 1) return Result.resolved(matches.getFirst());
        if (matches.size() > 1) return Result.ambiguous(raw, matches);
        return Result.unknown(raw);
    }

    public static String shortName(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    public static List<String> suggestions() {
        List<String> values = new ArrayList<>();
        VendorProfileManager.INSTANCE.listProfileIds().stream()
                .map(VendorProfileResolver::shortName)
                .distinct()
                .sorted()
                .forEach(values::add);
        VendorProfileManager.INSTANCE.listProfileIds().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(values::add);
        return values;
    }

    public record Result(Status status, ResourceLocation id, String raw, List<ResourceLocation> matches) {
        static Result resolved(ResourceLocation id) { return new Result(Status.RESOLVED, id, id.toString(), List.of()); }
        static Result invalid(String raw) { return new Result(Status.INVALID, null, raw, List.of()); }
        static Result unknown(String raw) { return new Result(Status.UNKNOWN, null, raw, List.of()); }
        static Result ambiguous(String raw, List<ResourceLocation> matches) { return new Result(Status.AMBIGUOUS, null, raw, List.copyOf(matches)); }

        public String matchList() {
            return String.join(", ", matches.stream().map(ResourceLocation::toString).toList());
        }
    }

    public enum Status { RESOLVED, INVALID, UNKNOWN, AMBIGUOUS }
}
