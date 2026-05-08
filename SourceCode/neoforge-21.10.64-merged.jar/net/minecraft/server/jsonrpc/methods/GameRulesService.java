package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameRules;

public class GameRulesService {
    public static List<GameRulesService.TypedRule> get(MinecraftApi api) {
        List<? extends GameRules.Key<?>> list = api.gameRuleService().getAvailableGameRules().map(Entry::getKey).toList();
        List<GameRulesService.TypedRule> list1 = new ArrayList<>();

        for (GameRules.Key<?> key : list) {
            GameRules.Value<?> value = api.gameRuleService().getRule(key);
            list1.add(getTypedRule(api, key.getId(), value));
        }

        return list1;
    }

    public static GameRulesService.TypedRule getTypedRule(MinecraftApi api, String name, GameRules.Value<?> value) {
        return api.gameRuleService().getTypedRule(name, value);
    }

    public static GameRulesService.TypedRule update(MinecraftApi api, GameRulesService.UntypedRule rule, ClientInfo clientInfo) {
        return api.gameRuleService().updateGameRule(rule, clientInfo);
    }

    public static enum RuleType implements StringRepresentable {
        INT("integer"),
        BOOL("boolean");

        private final String name;

        private RuleType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public record TypedRule(String key, String value, GameRulesService.RuleType type) {
        public static final MapCodec<GameRulesService.TypedRule> CODEC = RecordCodecBuilder.mapCodec(
            p_443057_ -> p_443057_.group(
                    Codec.STRING.fieldOf("key").forGetter(GameRulesService.TypedRule::key),
                    Codec.STRING.fieldOf("value").forGetter(GameRulesService.TypedRule::value),
                    StringRepresentable.fromEnum(GameRulesService.RuleType::values).fieldOf("type").forGetter(GameRulesService.TypedRule::type)
                )
                .apply(p_443057_, GameRulesService.TypedRule::new)
        );
    }

    public record UntypedRule(String key, String value) {
        public static final MapCodec<GameRulesService.UntypedRule> CODEC = RecordCodecBuilder.mapCodec(
            p_443600_ -> p_443600_.group(
                    Codec.STRING.fieldOf("key").forGetter(GameRulesService.UntypedRule::key),
                    Codec.STRING.fieldOf("value").forGetter(GameRulesService.UntypedRule::value)
                )
                .apply(p_443600_, GameRulesService.UntypedRule::new)
        );
    }
}
