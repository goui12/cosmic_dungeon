package net.minecraft.client.gui.font;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontSet implements AutoCloseable {
    private static final float LARGE_FORWARD_ADVANCE = 32.0F;
    private static final BakedGlyph INVISIBLE_MISSING_GLYPH = new BakedGlyph() {
        @Override
        public GlyphInfo info() {
            return SpecialGlyphs.MISSING;
        }

        @Nullable
        @Override
        public TextRenderable createGlyph(float p_439583_, float p_440409_, int p_439199_, int p_439804_, Style p_440068_, float p_439635_, float p_439960_) {
            return null;
        }
    };
    final GlyphStitcher stitcher;
    final UnbakedGlyph.Stitcher wrappedStitcher = new UnbakedGlyph.Stitcher() {
        @Override
        public BakedGlyph stitch(GlyphInfo p_439553_, GlyphBitmap p_439186_) {
            return Objects.requireNonNullElse(FontSet.this.stitcher.stitch(p_439553_, p_439186_), FontSet.this.missingGlyph);
        }

        @Override
        public BakedGlyph getMissing() {
            return FontSet.this.missingGlyph;
        }
    };
    private List<GlyphProvider.Conditional> allProviders = List.of();
    private List<GlyphProvider> activeProviders = List.of();
    private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
    private final CodepointMap<FontSet.SelectedGlyphs> glyphCache = new CodepointMap<>(FontSet.SelectedGlyphs[]::new, FontSet.SelectedGlyphs[][]::new);
    private final IntFunction<FontSet.SelectedGlyphs> glyphGetter = this::computeGlyphInfo;
    BakedGlyph missingGlyph = INVISIBLE_MISSING_GLYPH;
    private final Supplier<BakedGlyph> missingGlyphGetter = () -> this.missingGlyph;
    private final FontSet.SelectedGlyphs missingSelectedGlyphs = new FontSet.SelectedGlyphs(this.missingGlyphGetter, this.missingGlyphGetter);
    @Nullable
    private EffectGlyph whiteGlyph;
    private final GlyphSource anyGlyphs = new FontSet.Source(false);
    private final GlyphSource nonFishyGlyphs = new FontSet.Source(true);

    public FontSet(GlyphStitcher stitcher) {
        this.stitcher = stitcher;
    }

    public void reload(List<GlyphProvider.Conditional> allProviders, Set<FontOption> options) {
        this.allProviders = allProviders;
        this.reload(options);
    }

    public void reload(Set<FontOption> options) {
        this.activeProviders = List.of();
        this.resetTextures();
        this.activeProviders = this.selectProviders(this.allProviders, options);
    }

    private void resetTextures() {
        this.stitcher.reset();
        this.glyphCache.clear();
        this.glyphsByWidth.clear();
        this.missingGlyph = Objects.requireNonNull(SpecialGlyphs.MISSING.bake(this.stitcher));
        this.whiteGlyph = SpecialGlyphs.WHITE.bake(this.stitcher);
    }

    private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
        IntSet intset = new IntOpenHashSet();
        List<GlyphProvider> list = new ArrayList<>();

        for (GlyphProvider.Conditional glyphprovider$conditional : providers) {
            if (glyphprovider$conditional.filter().apply(options)) {
                list.add(glyphprovider$conditional.provider());
                intset.addAll(glyphprovider$conditional.provider().getSupportedGlyphs());
            }
        }

        Set<GlyphProvider> set = Sets.newHashSet();
        intset.forEach(p_438729_ -> {
            for (GlyphProvider glyphprovider : list) {
                UnbakedGlyph unbakedglyph = glyphprovider.getGlyph(p_438729_);
                if (unbakedglyph != null) {
                    set.add(glyphprovider);
                    if (unbakedglyph.info() != SpecialGlyphs.MISSING) {
                        this.glyphsByWidth.computeIfAbsent(Mth.ceil(unbakedglyph.info().getAdvance(false)), p_232567_ -> new IntArrayList()).add(p_438729_);
                    }
                    break;
                }
            }
        });
        return list.stream().filter(set::contains).toList();
    }

    @Override
    public void close() {
        this.stitcher.close();
    }

    private static boolean hasFishyAdvance(GlyphInfo glyph) {
        float f = glyph.getAdvance(false);
        if (!(f < 0.0F) && !(f > 32.0F)) {
            float f1 = glyph.getAdvance(true);
            return f1 < 0.0F || f1 > 32.0F;
        } else {
            return true;
        }
    }

    private FontSet.SelectedGlyphs computeGlyphInfo(int character) {
        FontSet.DelayedBake fontset$delayedbake = null;

        for (GlyphProvider glyphprovider : this.activeProviders) {
            UnbakedGlyph unbakedglyph = glyphprovider.getGlyph(character);
            if (unbakedglyph != null) {
                if (fontset$delayedbake == null) {
                    fontset$delayedbake = new FontSet.DelayedBake(unbakedglyph);
                }

                if (!hasFishyAdvance(unbakedglyph.info())) {
                    if (fontset$delayedbake.unbaked == unbakedglyph) {
                        return new FontSet.SelectedGlyphs(fontset$delayedbake, fontset$delayedbake);
                    }

                    return new FontSet.SelectedGlyphs(fontset$delayedbake, new FontSet.DelayedBake(unbakedglyph));
                }
            }
        }

        return fontset$delayedbake != null ? new FontSet.SelectedGlyphs(fontset$delayedbake, this.missingGlyphGetter) : this.missingSelectedGlyphs;
    }

    FontSet.SelectedGlyphs getGlyph(int character) {
        return this.glyphCache.computeIfAbsent(character, this.glyphGetter);
    }

    public BakedGlyph getRandomGlyph(RandomSource random, int width) {
        IntList intlist = this.glyphsByWidth.get(width);
        return intlist != null && !intlist.isEmpty() ? this.getGlyph(intlist.getInt(random.nextInt(intlist.size()))).nonFishy().get() : this.missingGlyph;
    }

    public EffectGlyph whiteGlyph() {
        return Objects.requireNonNull(this.whiteGlyph);
    }

    public GlyphSource source(boolean filterFishyGlyphs) {
        return filterFishyGlyphs ? this.nonFishyGlyphs : this.anyGlyphs;
    }

    @OnlyIn(Dist.CLIENT)
    class DelayedBake implements Supplier<BakedGlyph> {
        final UnbakedGlyph unbaked;
        @Nullable
        private BakedGlyph baked;

        DelayedBake(UnbakedGlyph unbaked) {
            this.unbaked = unbaked;
        }

        public BakedGlyph get() {
            if (this.baked == null) {
                this.baked = this.unbaked.bake(FontSet.this.wrappedStitcher);
            }

            return this.baked;
        }
    }

    @OnlyIn(Dist.CLIENT)
    record SelectedGlyphs(Supplier<BakedGlyph> any, Supplier<BakedGlyph> nonFishy) {
        Supplier<BakedGlyph> select(boolean filterFishy) {
            return filterFishy ? this.nonFishy : this.any;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class Source implements GlyphSource {
        private final boolean filterFishyGlyphs;

        public Source(boolean filterFishyGlyphs) {
            this.filterFishyGlyphs = filterFishyGlyphs;
        }

        @Override
        public BakedGlyph getGlyph(int index) {
            return FontSet.this.getGlyph(index).select(this.filterFishyGlyphs).get();
        }

        @Override
        public BakedGlyph getRandomGlyph(RandomSource random, int width) {
            return FontSet.this.getRandomGlyph(random, width);
        }
    }
}
