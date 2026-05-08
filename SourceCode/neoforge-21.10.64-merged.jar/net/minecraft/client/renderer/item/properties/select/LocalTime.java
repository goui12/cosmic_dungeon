package net.minecraft.client.renderer.item.properties.select;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocalTime implements SelectItemModelProperty<String> {
    public static final String ROOT_LOCALE = "";
    private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1L);
    public static final Codec<String> VALUE_CODEC = Codec.STRING;
    private static final Codec<TimeZone> TIME_ZONE_CODEC = VALUE_CODEC.comapFlatMap(p_389548_ -> {
        TimeZone timezone = TimeZone.getTimeZone(p_389548_);
        return timezone.equals(TimeZone.UNKNOWN_ZONE) ? DataResult.error(() -> "Unknown timezone: " + p_389548_) : DataResult.success(timezone);
    }, TimeZone::getID);
    private static final MapCodec<LocalTime.Data> DATA_MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_389646_ -> p_389646_.group(
                Codec.STRING.fieldOf("pattern").forGetter(p_390095_ -> p_390095_.format),
                Codec.STRING.optionalFieldOf("locale", "").forGetter(p_390090_ -> p_390090_.localeId),
                TIME_ZONE_CODEC.optionalFieldOf("time_zone").forGetter(p_390093_ -> p_390093_.timeZone)
            )
            .apply(p_389646_, LocalTime.Data::new)
    );
    public static final SelectItemModelProperty.Type<LocalTime, String> TYPE = SelectItemModelProperty.Type.create(
        DATA_MAP_CODEC.flatXmap(LocalTime::create, p_390089_ -> DataResult.success(p_390089_.data)), VALUE_CODEC
    );
    private final LocalTime.Data data;
    private final DateFormat parsedFormat;
    private long nextUpdateTimeMs;
    private String lastResult = "";

    private LocalTime(LocalTime.Data data, DateFormat parsedFormat) {
        this.data = data;
        this.parsedFormat = parsedFormat;
    }

    public static LocalTime create(String format, String localeId, Optional<TimeZone> timeZone) {
        return create(new LocalTime.Data(format, localeId, timeZone))
            .getOrThrow(p_390094_ -> new IllegalStateException("Failed to validate format: " + p_390094_));
    }

    private static DataResult<LocalTime> create(LocalTime.Data data) {
        ULocale ulocale = new ULocale(data.localeId);
        Calendar calendar = data.timeZone
            .<Calendar>map(p_389490_ -> Calendar.getInstance(p_389490_, ulocale))
            .orElseGet(() -> Calendar.getInstance(ulocale));
        SimpleDateFormat simpledateformat = new SimpleDateFormat(data.format, ulocale);
        simpledateformat.setCalendar(calendar);

        try {
            simpledateformat.format(new Date());
        } catch (Exception exception) {
            return DataResult.error(() -> "Invalid time format '" + simpledateformat + "': " + exception.getMessage());
        }

        return DataResult.success(new LocalTime(data, simpledateformat));
    }

    @Nullable
    public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
        long i = Util.getMillis();
        if (i > this.nextUpdateTimeMs) {
            this.lastResult = this.update();
            this.nextUpdateTimeMs = i + UPDATE_INTERVAL_MS;
        }

        return this.lastResult;
    }

    private String update() {
        return this.parsedFormat.format(new Date());
    }

    @Override
    public SelectItemModelProperty.Type<LocalTime, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }

    @OnlyIn(Dist.CLIENT)
    record Data(String format, String localeId, Optional<TimeZone> timeZone) {
    }
}
