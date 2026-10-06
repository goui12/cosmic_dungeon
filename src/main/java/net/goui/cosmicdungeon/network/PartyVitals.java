package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.*;
import net.minecraft.resources.ResourceLocation;

/** Immutable, bounded HUD data. No ticking durations: unchanged effects produce no packet churn. */
public record PartyVitals(float health, float maxHealth, String state, List<Effect> effects, int omittedEffects) {
    public static final int MAX_EFFECTS = 64;
    public static final PartyVitals OFFLINE = unavailable("OFFLINE");
    public static final PartyVitals UNLOADED = unavailable("UNLOADED");
    public static final PartyVitals DEAD = unavailable("DEAD");
    public PartyVitals {
        effects = List.copyOf(effects);
        if (!List.of("ACTIVE", "OFFLINE", "UNLOADED", "DEAD").contains(state)
                || !Float.isFinite(health) || !Float.isFinite(maxHealth) || health < 0 || maxHealth < health
                || effects.size() > MAX_EFFECTS || omittedEffects < 0
                || effects.stream().map(Effect::id).distinct().count() != effects.size()
                || !state.equals("ACTIVE") && (health != 0 || maxHealth != 0 || !effects.isEmpty() || omittedEffects != 0))
            throw new IllegalArgumentException("Invalid party vitals");
    }
    public static PartyVitals unavailable(String state) { return new PartyVitals(0, 0, state, List.of(), 0); }
    public record Effect(String id, int amplifier) {
        public Effect {
            if (id == null || id.length() > 256 || ResourceLocation.tryParse(id) == null || amplifier < 0 || amplifier > 255)
                throw new IllegalArgumentException("Invalid party effect");
        }
        public static final StreamCodec<ByteBuf, Effect> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(256), Effect::id, ByteBufCodecs.VAR_INT, Effect::amplifier, Effect::new);
    }
    public static final StreamCodec<ByteBuf, PartyVitals> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PartyVitals::health, ByteBufCodecs.FLOAT, PartyVitals::maxHealth,
            ByteBufCodecs.stringUtf8(16), PartyVitals::state,
            Effect.CODEC.apply(ByteBufCodecs.list(MAX_EFFECTS)), PartyVitals::effects,
            ByteBufCodecs.VAR_INT, PartyVitals::omittedEffects, PartyVitals::new);
}
