package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult.Error;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.item.component.ResolvableProfile;

public class FetchProfileCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("fetchprofile")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.literal("name")
                        .then(
                            Commands.argument("name", StringArgumentType.greedyString())
                                .executes(p_439240_ -> resolveName(p_439240_.getSource(), StringArgumentType.getString(p_439240_, "name")))
                        )
                )
                .then(
                    Commands.literal("id")
                        .then(
                            Commands.argument("id", UuidArgument.uuid())
                                .executes(p_440334_ -> resolveId(p_440334_.getSource(), UuidArgument.getUuid(p_440334_, "id")))
                        )
                )
        );
    }

    private static void reportResolvedProfile(CommandSourceStack source, GameProfile profile, String translationKey, Component id) {
        ResolvableProfile resolvableprofile = ResolvableProfile.createResolved(profile);
        ResolvableProfile.CODEC
            .encodeStart(NbtOps.INSTANCE, resolvableprofile)
            .ifSuccess(
                p_442370_ -> {
                    String s = p_442370_.toString();
                    MutableComponent mutablecomponent = Component.object(new PlayerSprite(resolvableprofile, true));
                    ComponentSerialization.CODEC
                        .encodeStart(NbtOps.INSTANCE, mutablecomponent)
                        .ifSuccess(
                            p_442365_ -> {
                                String s1 = p_442365_.toString();
                                source.sendSuccess(
                                    () -> {
                                        Component component = ComponentUtils.formatList(
                                            List.of(
                                                Component.translatable("commands.fetchprofile.copy_component")
                                                    .withStyle(p_439948_ -> p_439948_.withClickEvent(new ClickEvent.CopyToClipboard(s))),
                                                Component.translatable("commands.fetchprofile.give_item")
                                                    .withStyle(
                                                        p_439260_ -> p_439260_.withClickEvent(
                                                            new ClickEvent.RunCommand("give @s minecraft:player_head[profile=" + s + "]")
                                                        )
                                                    ),
                                                Component.translatable("commands.fetchprofile.summon_mannequin")
                                                    .withStyle(
                                                        p_450780_ -> p_450780_.withClickEvent(
                                                            new ClickEvent.RunCommand("summon minecraft:mannequin ~ ~ ~ {profile:" + s + "}")
                                                        )
                                                    ),
                                                Component.translatable("commands.fetchprofile.copy_text", mutablecomponent.withStyle(ChatFormatting.WHITE))
                                                    .withStyle(p_442352_ -> p_442352_.withClickEvent(new ClickEvent.CopyToClipboard(s1)))
                                            ),
                                            CommonComponents.SPACE,
                                            p_439150_ -> ComponentUtils.wrapInSquareBrackets(p_439150_.withStyle(ChatFormatting.GREEN))
                                        );
                                        return Component.translatable(translationKey, id, component);
                                    },
                                    false
                                );
                            }
                        )
                        .ifError(p_442354_ -> source.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", p_442354_.message())));
                }
            )
            .ifError(p_439929_ -> source.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", p_439929_.message())));
    }

    private static int resolveName(CommandSourceStack source, String name) {
        MinecraftServer minecraftserver = source.getServer();
        ProfileResolver profileresolver = minecraftserver.services().profileResolver();
        Util.nonCriticalIoPool()
            .execute(
                () -> {
                    Component component = Component.literal(name);
                    Optional<GameProfile> optional = profileresolver.fetchByName(name);
                    minecraftserver.execute(
                        () -> optional.ifPresentOrElse(
                            p_439646_ -> reportResolvedProfile(source, p_439646_, "commands.fetchprofile.name.success", component),
                            () -> source.sendFailure(Component.translatable("commands.fetchprofile.name.failure", component))
                        )
                    );
                }
            );
        return 1;
    }

    private static int resolveId(CommandSourceStack source, UUID id) {
        MinecraftServer minecraftserver = source.getServer();
        ProfileResolver profileresolver = minecraftserver.services().profileResolver();
        Util.nonCriticalIoPool()
            .execute(
                () -> {
                    Component component = Component.translationArg(id);
                    Optional<GameProfile> optional = profileresolver.fetchById(id);
                    minecraftserver.execute(
                        () -> optional.ifPresentOrElse(
                            p_439626_ -> reportResolvedProfile(source, p_439626_, "commands.fetchprofile.id.success", component),
                            () -> source.sendFailure(Component.translatable("commands.fetchprofile.id.failure", component))
                        )
                    );
                }
            );
        return 1;
    }
}
