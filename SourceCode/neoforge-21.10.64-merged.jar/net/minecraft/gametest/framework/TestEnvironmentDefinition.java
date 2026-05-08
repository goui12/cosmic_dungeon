package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition {
    Codec<TestEnvironmentDefinition> DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE
        .byNameCodec()
        .dispatch(TestEnvironmentDefinition::codec, p_397290_ -> p_397290_);
    Codec<Holder<TestEnvironmentDefinition>> CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

    static MapCodec<? extends TestEnvironmentDefinition> bootstrap(Registry<MapCodec<? extends TestEnvironmentDefinition>> registry) {
        Registry.register(registry, "all_of", TestEnvironmentDefinition.AllOf.CODEC);
        Registry.register(registry, "game_rules", TestEnvironmentDefinition.SetGameRules.CODEC);
        Registry.register(registry, "time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
        Registry.register(registry, "weather", TestEnvironmentDefinition.Weather.CODEC);
        return Registry.register(registry, "function", TestEnvironmentDefinition.Functions.CODEC);
    }

    void setup(ServerLevel level);

    default void teardown(ServerLevel level) {
    }

    MapCodec<? extends TestEnvironmentDefinition> codec();

    public record AllOf(List<Holder<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.AllOf> CODEC = RecordCodecBuilder.mapCodec(
            p_397209_ -> p_397209_.group(
                    TestEnvironmentDefinition.CODEC.listOf().fieldOf("definitions").forGetter(TestEnvironmentDefinition.AllOf::definitions)
                )
                .apply(p_397209_, TestEnvironmentDefinition.AllOf::new)
        );

        public AllOf(TestEnvironmentDefinition... p_398055_) {
            this(Arrays.stream(p_398055_).map(Holder::direct).toList());
        }

        @Override
        public void setup(ServerLevel level) {
            this.definitions.forEach(p_397273_ -> p_397273_.value().setup(level));
        }

        @Override
        public void teardown(ServerLevel level) {
            this.definitions.forEach(p_397475_ -> p_397475_.value().teardown(level));
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.AllOf> codec() {
            return CODEC;
        }
    }

    public record Functions(Optional<ResourceLocation> setupFunction, Optional<ResourceLocation> teardownFunction) implements TestEnvironmentDefinition {
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final MapCodec<TestEnvironmentDefinition.Functions> CODEC = RecordCodecBuilder.mapCodec(
            p_397358_ -> p_397358_.group(
                    ResourceLocation.CODEC.optionalFieldOf("setup").forGetter(TestEnvironmentDefinition.Functions::setupFunction),
                    ResourceLocation.CODEC.optionalFieldOf("teardown").forGetter(TestEnvironmentDefinition.Functions::teardownFunction)
                )
                .apply(p_397358_, TestEnvironmentDefinition.Functions::new)
        );

        @Override
        public void setup(ServerLevel level) {
            this.setupFunction.ifPresent(p_397245_ -> run(level, p_397245_));
        }

        @Override
        public void teardown(ServerLevel level) {
            this.teardownFunction.ifPresent(p_397509_ -> run(level, p_397509_));
        }

        private static void run(ServerLevel level, ResourceLocation function) {
            MinecraftServer minecraftserver = level.getServer();
            ServerFunctionManager serverfunctionmanager = minecraftserver.getFunctions();
            Optional<CommandFunction<CommandSourceStack>> optional = serverfunctionmanager.get(function);
            if (optional.isPresent()) {
                CommandSourceStack commandsourcestack = minecraftserver.createCommandSourceStack()
                    .withPermission(2)
                    .withSuppressedOutput()
                    .withLevel(level);
                serverfunctionmanager.execute(optional.get(), commandsourcestack);
            } else {
                LOGGER.error("Test Batch failed for non-existent function {}", function);
            }
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.Functions> codec() {
            return CODEC;
        }
    }

    public record SetGameRules(
        List<TestEnvironmentDefinition.SetGameRules.Entry<Boolean, GameRules.BooleanValue>> boolRules,
        List<TestEnvironmentDefinition.SetGameRules.Entry<Integer, GameRules.IntegerValue>> intRules
    ) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.SetGameRules> CODEC = RecordCodecBuilder.mapCodec(
            p_397782_ -> p_397782_.group(
                    TestEnvironmentDefinition.SetGameRules.Entry.codec(GameRules.BooleanValue.class, Codec.BOOL)
                        .listOf()
                        .fieldOf("bool_rules")
                        .forGetter(TestEnvironmentDefinition.SetGameRules::boolRules),
                    TestEnvironmentDefinition.SetGameRules.Entry.codec(GameRules.IntegerValue.class, Codec.INT)
                        .listOf()
                        .fieldOf("int_rules")
                        .forGetter(TestEnvironmentDefinition.SetGameRules::intRules)
                )
                .apply(p_397782_, TestEnvironmentDefinition.SetGameRules::new)
        );

        @Override
        public void setup(ServerLevel level) {
            GameRules gamerules = level.getGameRules();
            MinecraftServer minecraftserver = level.getServer();

            for (TestEnvironmentDefinition.SetGameRules.Entry<Boolean, GameRules.BooleanValue> entry : this.boolRules) {
                gamerules.getRule(entry.key()).set(entry.value(), minecraftserver);
            }

            for (TestEnvironmentDefinition.SetGameRules.Entry<Integer, GameRules.IntegerValue> entry1 : this.intRules) {
                gamerules.getRule(entry1.key()).set(entry1.value(), minecraftserver);
            }
        }

        @Override
        public void teardown(ServerLevel level) {
            GameRules gamerules = level.getGameRules();
            MinecraftServer minecraftserver = level.getServer();

            for (TestEnvironmentDefinition.SetGameRules.Entry<Boolean, GameRules.BooleanValue> entry : this.boolRules) {
                gamerules.getRule(entry.key()).setFrom(GameRules.getType(entry.key()).createRule(), minecraftserver);
            }

            for (TestEnvironmentDefinition.SetGameRules.Entry<Integer, GameRules.IntegerValue> entry1 : this.intRules) {
                gamerules.getRule(entry1.key()).setFrom(GameRules.getType(entry1.key()).createRule(), minecraftserver);
            }
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.SetGameRules> codec() {
            return CODEC;
        }

        public static <S, T extends GameRules.Value<T>> TestEnvironmentDefinition.SetGameRules.Entry<S, T> entry(GameRules.Key<T> key, S value) {
            return new TestEnvironmentDefinition.SetGameRules.Entry<>(key, value);
        }

        public record Entry<S, T extends GameRules.Value<T>>(GameRules.Key<T> key, S value) {
            public static <S, T extends GameRules.Value<T>> Codec<TestEnvironmentDefinition.SetGameRules.Entry<S, T>> codec(
                Class<T> valueClass, Codec<S> valueCodec
            ) {
                return RecordCodecBuilder.create(
                    p_397136_ -> p_397136_.group(
                            GameRules.keyCodec(valueClass).fieldOf("rule").forGetter(TestEnvironmentDefinition.SetGameRules.Entry::key),
                            valueCodec.fieldOf("value").forGetter(TestEnvironmentDefinition.SetGameRules.Entry::value)
                        )
                        .apply(p_397136_, TestEnvironmentDefinition.SetGameRules.Entry::new)
                );
            }
        }
    }

    public record TimeOfDay(int time) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.TimeOfDay> CODEC = RecordCodecBuilder.mapCodec(
            p_397416_ -> p_397416_.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TestEnvironmentDefinition.TimeOfDay::time))
                .apply(p_397416_, TestEnvironmentDefinition.TimeOfDay::new)
        );

        @Override
        public void setup(ServerLevel level) {
            level.setDayTime(this.time);
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.TimeOfDay> codec() {
            return CODEC;
        }
    }

    public record Weather(TestEnvironmentDefinition.Weather.Type weather) implements TestEnvironmentDefinition {
        public static final MapCodec<TestEnvironmentDefinition.Weather> CODEC = RecordCodecBuilder.mapCodec(
            p_397274_ -> p_397274_.group(TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(TestEnvironmentDefinition.Weather::weather))
                .apply(p_397274_, TestEnvironmentDefinition.Weather::new)
        );

        @Override
        public void setup(ServerLevel level) {
            this.weather.apply(level);
        }

        @Override
        public void teardown(ServerLevel level) {
            level.resetWeatherCycle();
        }

        @Override
        public MapCodec<TestEnvironmentDefinition.Weather> codec() {
            return CODEC;
        }

        public static enum Type implements StringRepresentable {
            CLEAR("clear", 100000, 0, false, false),
            RAIN("rain", 0, 100000, true, false),
            THUNDER("thunder", 0, 100000, true, true);

            public static final Codec<TestEnvironmentDefinition.Weather.Type> CODEC = StringRepresentable.fromEnum(
                TestEnvironmentDefinition.Weather.Type::values
            );
            private final String id;
            private final int clearTime;
            private final int rainTime;
            private final boolean raining;
            private final boolean thundering;

            private Type(String id, int clearTime, int rainTime, boolean raining, boolean thundering) {
                this.id = id;
                this.clearTime = clearTime;
                this.rainTime = rainTime;
                this.raining = raining;
                this.thundering = thundering;
            }

            void apply(ServerLevel level) {
                level.setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
            }

            @Override
            public String getSerializedName() {
                return this.id;
            }
        }
    }
}
