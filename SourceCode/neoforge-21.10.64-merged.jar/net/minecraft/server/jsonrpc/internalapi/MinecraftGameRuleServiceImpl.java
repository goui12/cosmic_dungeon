package net.minecraft.server.jsonrpc.internalapi;

import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import net.minecraft.world.flag.FeatureFlagSet;

public class MinecraftGameRuleServiceImpl implements MinecraftGameRuleService {
    private final DedicatedServer server;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftGameRuleServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public GameRulesService.TypedRule updateGameRule(GameRulesService.UntypedRule rule, ClientInfo clientInfo) {
        net.minecraft.world.level.GameRules.Value<?> value = this.getRuleValue(rule.key());
        String s = value.serialize();
        if (value instanceof net.minecraft.world.level.GameRules.BooleanValue gamerules$booleanvalue) {
            gamerules$booleanvalue.set(Boolean.parseBoolean(rule.value()), this.server);
        } else {
            if (!(value instanceof net.minecraft.world.level.GameRules.IntegerValue gamerules$integervalue)) {
                throw new InvalidParameterJsonRpcException("Unknown rule type for key: " + rule.key());
            }

            gamerules$integervalue.set(Integer.parseInt(rule.value()), this.server);
        }

        GameRulesService.TypedRule gamerulesservice$typedrule = this.getTypedRule(rule.key(), value);
        this.jsonrpcLogger.log(clientInfo, "Game rule '{}' updated from '{}' to '{}'", gamerulesservice$typedrule.key(), s, gamerulesservice$typedrule.value());
        this.server.onGameRuleChanged(rule.key(), value);
        return gamerulesservice$typedrule;
    }

    @Override
    public <T extends net.minecraft.world.level.GameRules.Value<T>> T getRule(net.minecraft.world.level.GameRules.Key<T> key) {
        return this.server.getGameRules().getRule(key);
    }

    @Override
    public GameRulesService.TypedRule getTypedRule(String name, net.minecraft.world.level.GameRules.Value<?> value) {
        return switch (value) {
            case net.minecraft.world.level.GameRules.BooleanValue gamerules$booleanvalue -> new GameRulesService.TypedRule(
                name, String.valueOf(gamerules$booleanvalue.get()), GameRulesService.RuleType.BOOL
            );
            case net.minecraft.world.level.GameRules.IntegerValue gamerules$integervalue -> new GameRulesService.TypedRule(
                name, String.valueOf(gamerules$integervalue.get()), GameRulesService.RuleType.INT
            );
            default -> throw new InvalidParameterJsonRpcException("Unknown rule type");
        };
    }

    @Override
    public Stream<Entry<net.minecraft.world.level.GameRules.Key<?>, net.minecraft.world.level.GameRules.Type<?>>> getAvailableGameRules() {
        FeatureFlagSet featureflagset = this.server.getWorldData().getLevelSettings().getDataConfiguration().enabledFeatures();
        return net.minecraft.world.level.GameRules.availableRules(featureflagset);
    }

    private Optional<net.minecraft.world.level.GameRules.Key<?>> getRuleKey(String gamerule) {
        Stream<Entry<net.minecraft.world.level.GameRules.Key<?>, net.minecraft.world.level.GameRules.Type<?>>> stream = this.getAvailableGameRules();
        return stream.filter(p_449355_ -> p_449355_.getKey().getId().equals(gamerule)).findFirst().map(Entry::getKey);
    }

    private net.minecraft.world.level.GameRules.Value<?> getRuleValue(String gamerule) {
        net.minecraft.world.level.GameRules.Key<?> key = this.getRuleKey(gamerule)
            .orElseThrow(() -> new InvalidParameterJsonRpcException("Game rule '" + gamerule + "' does not exist"));
        return this.server.getGameRules().getRule(key);
    }
}
