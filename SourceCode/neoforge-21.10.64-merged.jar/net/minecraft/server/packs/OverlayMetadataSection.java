package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.InclusiveRange;

public record OverlayMetadataSection(List<OverlayMetadataSection.OverlayEntry> overlays) {
    private static final Pattern DIR_VALIDATOR = Pattern.compile("[-_a-zA-Z0-9.]+");
    public static final MetadataSectionType<OverlayMetadataSection> CLIENT_TYPE = new MetadataSectionType<>(
        "overlays", codecForPackType(PackType.CLIENT_RESOURCES)
    );
    public static final MetadataSectionType<OverlayMetadataSection> SERVER_TYPE = new MetadataSectionType<>("overlays", codecForPackType(PackType.SERVER_DATA));
    // Neo: alternative metadata section types that will only be loaded on neoforged. Useful for making datapacks with special
    // logic on different modloaders, or when running on neo vs in vanilla, without having to invert the main pack and overlays
    public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_CLIENT_TYPE = new MetadataSectionType<>("neoforge:overlays", CLIENT_TYPE.codec());
    public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_SERVER_TYPE = new MetadataSectionType<>("neoforge:overlays", SERVER_TYPE.codec());

    private static DataResult<String> validateOverlayDir(String directoryName) {
        return !DIR_VALIDATOR.matcher(directoryName).matches()
            ? DataResult.error(() -> directoryName + " is not accepted directory name")
            : DataResult.success(directoryName);
    }

    @VisibleForTesting
    public static Codec<OverlayMetadataSection> codecForPackType(PackType packType) {
        return RecordCodecBuilder.create(
            p_432471_ -> p_432471_.group(
                    OverlayMetadataSection.OverlayEntry.listCodecForPackType(packType).fieldOf("entries").forGetter(OverlayMetadataSection::overlays)
                )
                .apply(p_432471_, OverlayMetadataSection::new)
        );
    }

    public static MetadataSectionType<OverlayMetadataSection> forPackType(PackType packType) {
        return switch (packType) {
            case CLIENT_RESOURCES -> CLIENT_TYPE;
            case SERVER_DATA -> SERVER_TYPE;
        };
    }

    public static MetadataSectionType<OverlayMetadataSection> forPackTypeNeoForge(PackType p_434837_) {
        return switch (p_434837_) {
            case CLIENT_RESOURCES -> NEOFORGE_CLIENT_TYPE;
            case SERVER_DATA -> NEOFORGE_SERVER_TYPE;
        };
    }

    public List<String> overlaysForVersion(PackFormat packFormat) {
        return this.overlays.stream().filter(p_432473_ -> p_432473_.isApplicable(packFormat)).map(OverlayMetadataSection.OverlayEntry::overlay).toList();
    }

    public record OverlayEntry(InclusiveRange<PackFormat> format, String overlay) {
        static Codec<List<OverlayMetadataSection.OverlayEntry>> listCodecForPackType(PackType packType) {
            int i = PackFormat.lastPreMinorVersion(packType);
            return net.neoforged.neoforge.common.conditions.ConditionalOps.decodeListWithElementConditions(OverlayMetadataSection.OverlayEntry.IntermediateEntry.CODEC)
                .flatXmap(
                    p_432477_ -> PackFormat.validateHolderList(
                        (List<OverlayMetadataSection.OverlayEntry.IntermediateEntry>)p_432477_,
                        i,
                        (p_432478_, p_432479_) -> new OverlayMetadataSection.OverlayEntry(p_432479_, p_432478_.overlay())
                    ),
                    p_432475_ -> DataResult.success(
                        p_432475_.stream()
                            .map(
                                p_432481_ -> new OverlayMetadataSection.OverlayEntry.IntermediateEntry(
                                    PackFormat.IntermediaryFormat.fromRange(p_432481_.format(), i), p_432481_.overlay()
                                )
                            )
                            .toList()
                    )
                );
        }

        public boolean isApplicable(PackFormat packFormat) {
            return this.format.isValueInRange(packFormat);
        }

        public record IntermediateEntry(PackFormat.IntermediaryFormat format, String overlay) implements PackFormat.IntermediaryFormatHolder {
            public static final Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> CODEC = RecordCodecBuilder.create(
                p_434215_ -> p_434215_.group(
                        PackFormat.IntermediaryFormat.OVERLAY_CODEC.forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::format),
                        Codec.STRING
                            .validate(OverlayMetadataSection::validateOverlayDir)
                            .fieldOf("directory")
                            .forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::overlay)
                    )
                    .apply(p_434215_, OverlayMetadataSection.OverlayEntry.IntermediateEntry::new)
            );

            @Override
            public String toString() {
                return this.overlay;
            }
        }
    }
}
