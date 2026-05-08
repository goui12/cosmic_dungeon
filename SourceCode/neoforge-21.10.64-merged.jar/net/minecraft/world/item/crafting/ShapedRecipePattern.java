package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public final class ShapedRecipePattern {
    /** @deprecated Neo: use {@link #getMaxWidth} and {@link #getMaxHeight} */ @Deprecated
    private static final int MAX_SIZE = 3;
    public static final char EMPTY_SLOT = ' ';
    static int maxWidth = 3;
    static int maxHeight = 3;

    public static int getMaxWidth() {
        return maxWidth;
    }

    public static int getMaxHeight() {
        return maxHeight;
    }

    /**
     * Expand the max width and height allowed in the deserializer.
     * This should be called by modders who add custom crafting tables that are larger than the vanilla 3x3.
     * @param width your max recipe width
     * @param height your max recipe height
     */
    public static void setCraftingSize(int width, int height) {
        if (maxWidth < width) maxWidth = width;
        if (maxHeight < height) maxHeight = height;
    }

    public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
        .flatXmap(
            ShapedRecipePattern::unpack,
            p_344423_ -> p_344423_.data.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        p_360067_ -> p_360067_.width,
        ByteBufCodecs.VAR_INT,
        p_360066_ -> p_360066_.height,
        Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
        p_360065_ -> p_360065_.ingredients,
        ShapedRecipePattern::createFromNetwork
    );
    private final int width;
    private final int height;
    private final List<Optional<Ingredient>> ingredients;
    private final Optional<ShapedRecipePattern.Data> data;
    private final int ingredientCount;
    private final boolean symmetrical;

    public ShapedRecipePattern(int width, int height, List<Optional<Ingredient>> ingredients, Optional<ShapedRecipePattern.Data> data) {
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.data = data;
        this.ingredientCount = (int)ingredients.stream().flatMap(Optional::stream).count();
        this.symmetrical = Util.isSymmetrical(width, height, ingredients);
    }

    private static ShapedRecipePattern createFromNetwork(Integer width, Integer height, List<Optional<Ingredient>> ingredients) {
        return new ShapedRecipePattern(width, height, ingredients, Optional.empty());
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> key, String... pattern) {
        return of(key, List.of(pattern));
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> key, List<String> pattern) {
        ShapedRecipePattern.Data shapedrecipepattern$data = new ShapedRecipePattern.Data(key, pattern);
        return unpack(shapedrecipepattern$data).getOrThrow();
    }

    private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data data) {
        String[] astring = shrink(data.pattern);
        int i = astring[0].length();
        int j = astring.length;
        List<Optional<Ingredient>> list = new ArrayList<>(i * j);
        CharSet charset = new CharArraySet(data.key.keySet());

        for (String s : astring) {
            for (int k = 0; k < s.length(); k++) {
                char c0 = s.charAt(k);
                Optional<Ingredient> optional;
                if (c0 == ' ') {
                    optional = Optional.empty();
                } else {
                    Ingredient ingredient = data.key.get(c0);
                    if (ingredient == null) {
                        return DataResult.error(() -> "Pattern references symbol '" + c0 + "' but it's not defined in the key");
                    }

                    optional = Optional.of(ingredient);
                }

                charset.remove(c0);
                list.add(optional);
            }
        }

        return !charset.isEmpty()
            ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
            : DataResult.success(new ShapedRecipePattern(i, j, list, Optional.of(data)));
    }

    @VisibleForTesting
    static String[] shrink(List<String> pattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for (int i1 = 0; i1 < pattern.size(); i1++) {
            String s = pattern.get(i1);
            i = Math.min(i, firstNonEmpty(s));
            int j1 = lastNonEmpty(s);
            j = Math.max(j, j1);
            if (j1 < 0) {
                if (k == i1) {
                    k++;
                }

                l++;
            } else {
                l = 0;
            }
        }

        if (pattern.size() == l) {
            return new String[0];
        } else {
            String[] astring = new String[pattern.size() - l - k];

            for (int k1 = 0; k1 < astring.length; k1++) {
                astring[k1] = pattern.get(k1 + k).substring(i, j + 1);
            }

            return astring;
        }
    }

    private static int firstNonEmpty(String row) {
        int i = 0;

        while (i < row.length() && row.charAt(i) == ' ') {
            i++;
        }

        return i;
    }

    private static int lastNonEmpty(String row) {
        int i = row.length() - 1;

        while (i >= 0 && row.charAt(i) == ' ') {
            i--;
        }

        return i;
    }

    public boolean matches(CraftingInput input) {
        if (input.ingredientCount() != this.ingredientCount) {
            return false;
        } else {
            if (input.width() == this.width && input.height() == this.height) {
                if (!this.symmetrical && this.matches(input, true)) {
                    return true;
                }

                if (this.matches(input, false)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean matches(CraftingInput input, boolean symmetrical) {
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                Optional<Ingredient> optional;
                if (symmetrical) {
                    optional = this.ingredients.get(this.width - j - 1 + i * this.width);
                } else {
                    optional = this.ingredients.get(j + i * this.width);
                }

                ItemStack itemstack = input.getItem(j, i);
                if (!Ingredient.testOptionalIngredient(optional, itemstack)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public List<Optional<Ingredient>> ingredients() {
        return this.ingredients;
    }

    public record Data(Map<Character, Ingredient> key, List<String> pattern) {
        private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(p_312085_ -> {
            if (p_312085_.size() > maxHeight) {
                return DataResult.error(() -> "Invalid pattern: too many rows, %s is maximum".formatted(maxHeight));
            } else if (p_312085_.isEmpty()) {
                return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
            } else {
                int i = p_312085_.getFirst().length();

                for (String s : p_312085_) {
                    if (s.length() > maxWidth) {
                        return DataResult.error(() -> "Invalid pattern: too many columns, %s is maximum".formatted(maxWidth));
                    }

                    if (i != s.length()) {
                        return DataResult.error(() -> "Invalid pattern: each row must be the same width");
                    }
                }

                return DataResult.success(p_312085_);
            }
        }, Function.identity());
        private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(p_312250_ -> {
            if (p_312250_.length() != 1) {
                return DataResult.error(() -> "Invalid key entry: '" + p_312250_ + "' is an invalid symbol (must be 1 character only).");
            } else {
                return " ".equals(p_312250_) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(p_312250_.charAt(0));
            }
        }, String::valueOf);
        public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_360068_ -> p_360068_.group(
                    ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(p_312509_ -> p_312509_.key),
                    PATTERN_CODEC.fieldOf("pattern").forGetter(p_312713_ -> p_312713_.pattern)
                )
                .apply(p_360068_, ShapedRecipePattern.Data::new)
        );
    }
}
