package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.block.custom.CosmicMobSpawnerBlock;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SpawnerCommand {
    private SpawnerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("spawner").requires(s -> s.hasPermission(2))
                .executes(c -> help(c.getSource()))
                .then(Commands.literal("help").executes(c -> help(c.getSource())))
                .then(Commands.literal("set").executes(c -> helpSet(c.getSource()))
                        .then(Commands.argument("entity_type", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestEntities)
                                .executes(c -> withPreset(c.getSource(), p -> {
                                    ResourceLocation rl = ResourceLocationArgument.getId(c, "entity_type");
                                    if (CosmicSpawnerPreset.ILLAGER_CAPTAIN_ID.equals(rl)) p.setIllagerCaptainVariant();
                                    else p.setEntityTypeId(rl);
                                }))))
                .then(Commands.literal("name").executes(c -> helpName(c.getSource())).then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> withPreset(c.getSource(), p -> p.setCustomName(Component.literal(StringArgumentType.getString(c, "name")))))))
                        .then(Commands.literal("clear").executes(c -> withPreset(c.getSource(), p -> p.setCustomName(null)))))
                .then(Commands.literal("flag").executes(c -> helpFlag(c.getSource()))
                        .then(Commands.literal("boss").executes(c -> setBossFlag(c.getSource(), true))
                                .then(Commands.argument("value", BoolArgumentType.bool()).executes(c -> setBossFlag(c.getSource(), BoolArgumentType.getBool(c, "value")))))
                        .then(Commands.argument("flag", StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(Arrays.asList("persistent","name_visible","silent","glowing","no_ai","no_gravity"),b))
                                .then(Commands.argument("value", BoolArgumentType.bool()).executes(c -> withPreset(c.getSource(), p -> applyFlag(p, StringArgumentType.getString(c,"flag"), BoolArgumentType.getBool(c,"value")))))))
                .then(Commands.literal("cap").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(c -> setCapFlag(c.getSource(), IntegerArgumentType.getInteger(c, "amount")))))
                .then(Commands.literal("equip").executes(c -> helpEquip(c.getSource())).then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots)
                                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestItems).executes(c -> withPreset(c.getSource(), p -> p.setEquipment(slot(c), BuiltInRegistries.ITEM.get(ResourceLocationArgument.getId(c,"item")).map(ItemStack::new).orElse(ItemStack.EMPTY)))))
                                .then(Commands.literal("fromhand").executes(c -> equipFromHand(c.getSource(), slot(c)))))
                        .then(Commands.literal("clear").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).executes(c -> withPreset(c.getSource(), p -> p.setEquipment(slot(c), ItemStack.EMPTY))))
                                .then(Commands.literal("all").executes(c -> withPreset(c.getSource(), p -> { for (var s : CosmicSpawnerPreset.Slot.values()) p.setEquipment(s, ItemStack.EMPTY);})))) )
                .then(Commands.literal("enchant")
                        .then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots)
                                .then(Commands.argument("enchantment", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestEnchantments)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                                .executes(c -> enchant(c.getSource(), slot(c), ResourceLocationArgument.getId(c, "enchantment"), IntegerArgumentType.getInteger(c, "level"), false))
                                                .then(Commands.argument("allowUnsafe", BoolArgumentType.bool())
                                                        .executes(c -> enchant(c.getSource(), slot(c), ResourceLocationArgument.getId(c, "enchantment"), IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "allowUnsafe")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots)
                                        .then(Commands.argument("enchantment", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestEnchantments)
                                                .executes(c -> removeEnchant(c.getSource(), slot(c), ResourceLocationArgument.getId(c, "enchantment"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots)
                                        .executes(c -> clearEnchantments(c.getSource(), slot(c))))))
                 .then(Commands.literal("drop").executes(c -> helpDrop(c.getSource())).then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> p.setDropChance(slot(c), FloatArgumentType.getFloat(c,"chance"))))))
                        .then(Commands.literal("armor").then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> {float f=FloatArgumentType.getFloat(c,"chance"); p.setDropChance(CosmicSpawnerPreset.Slot.HEAD,f);p.setDropChance(CosmicSpawnerPreset.Slot.CHEST,f);p.setDropChance(CosmicSpawnerPreset.Slot.LEGS,f);p.setDropChance(CosmicSpawnerPreset.Slot.FEET,f);}))))
                        .then(Commands.literal("hands").then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> {float f=FloatArgumentType.getFloat(c,"chance"); p.setDropChance(CosmicSpawnerPreset.Slot.MAINHAND,f);p.setDropChance(CosmicSpawnerPreset.Slot.OFFHAND,f);}))))
                        .then(Commands.literal("all").then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> {float f=FloatArgumentType.getFloat(c,"chance"); for (var s: CosmicSpawnerPreset.Slot.values()) p.setDropChance(s,f);}))))
                        .then(Commands.literal("intrinsic")
                                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestItems)
                                        .then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> p.setIntrinsicDropChance(ResourceLocationArgument.getId(c, "item"), FloatArgumentType.getFloat(c, "chance"))))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestItems).executes(c -> withPreset(c.getSource(), p -> p.clearIntrinsicDropChance(ResourceLocationArgument.getId(c, "item")))))))
                )
                                .then(Commands.literal("drops").executes(c -> info(c.getSource())))
                .then(Commands.literal("delay").then(Commands.argument("ticks", IntegerArgumentType.integer(1)).executes(c -> setDelay(c.getSource(), IntegerArgumentType.getInteger(c, "ticks")))))
                .then(Commands.literal("adjustdrop").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).then(Commands.argument("delta", FloatArgumentType.floatArg(-1f,1f)).executes(c -> adjustDrop(c.getSource(), slot(c), FloatArgumentType.getFloat(c, "delta"))))))
                .then(Commands.literal("adjustintrinsic").then(Commands.argument("item", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestItems).then(Commands.argument("delta", FloatArgumentType.floatArg(-1f,1f)).executes(c -> adjustIntrinsic(c.getSource(), ResourceLocationArgument.getId(c, "item"), FloatArgumentType.getFloat(c, "delta"))))))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
                .then(Commands.argument("invalid", StringArgumentType.greedyString()).suggests(SpawnerCommand::suggestRoot).executes(c -> invalid(c.getSource(), StringArgumentType.getString(c, "invalid"))))
        );
    }

    private static int help(CommandSourceStack src) { return syntax(src, "Spawner command guide", new String[]{
            "/spawner set <namespace:entity>", "/spawner equip <slot> <namespace:item>", "/spawner drop <slot> <0.0-1.0>",
            "/spawner drops", "/spawner info", "/spawner reset", "/spawner help"
    }); }
    private static int helpSet(CommandSourceStack src){ return syntax(src, "Set syntax", new String[]{"/spawner set <namespace:entity>","Tip: Use TAB to browse entities."}); }
    private static int helpName(CommandSourceStack src){ return syntax(src, "Name syntax", new String[]{"/spawner name set <display name>","/spawner name clear"}); }
    private static int helpFlag(CommandSourceStack src){ return syntax(src, "Flag syntax", new String[]{"/spawner flag boss [true|false]","/spawner cap <0+>","/spawner flag <persistent|name_visible|silent|glowing|no_ai|no_gravity> <true|false>"}); }
    private static int helpEquip(CommandSourceStack src){ return syntax(src, "Equip syntax", new String[]{"/spawner equip <slot> <namespace:item>","/spawner equip <slot> fromhand","/spawner equip clear <slot>","/spawner equip clear all"}); }
    private static int helpDrop(CommandSourceStack src){ return syntax(src, "Drop syntax", new String[]{"/spawner drop <slot> <0.0-1.0>","/spawner drop armor <0.0-1.0>","/spawner drop hands <0.0-1.0>","/spawner drop all <0.0-1.0>","/spawner drop intrinsic <namespace:item> <0.0-1.0>","/spawner drop intrinsic clear <namespace:item>","Drop controls are also clickable in /spawner info"}); }
    private static int invalid(CommandSourceStack src, String invalid){ src.sendFailure(Component.literal("Unknown /spawner syntax: " + invalid)); return help(src); }
    private static int syntax(CommandSourceStack src, String title, String[] lines){
        src.sendSuccess(() -> Component.literal(title).withStyle(ChatFormatting.GOLD), false);
        for (String line : lines) src.sendSuccess(() -> Component.literal(" - " + line).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int equipFromHand(CommandSourceStack src, CosmicSpawnerPreset.Slot slot) throws com.mojang.brigadier.exceptions.CommandSyntaxException { ItemStack stack = src.getPlayerOrException().getMainHandItem().copy(); return withPreset(src, p -> p.setEquipment(slot, stack)); }
    private static int enchant(CommandSourceStack src, CosmicSpawnerPreset.Slot slot, ResourceLocation enchId, int level, boolean allowUnsafe) { return withPreset(src, p -> { ItemStack stack = p.getEquipment(slot).copy(); if (stack.isEmpty()) stack = new ItemStack(Items.STICK); var enchOpt = src.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchId); if (enchOpt.isPresent()) { Holder<Enchantment> ench = enchOpt.get(); if (allowUnsafe || stack.supportsEnchantment(ench)) stack.enchant(ench, level); } p.setEquipment(slot, stack); });}
    private static int removeEnchant(CommandSourceStack src, CosmicSpawnerPreset.Slot slot, ResourceLocation enchId) { return withPreset(src, p -> { ItemStack stack = p.getEquipment(slot).copy(); var enchOpt = src.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchId); enchOpt.ifPresent(enchantment -> EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(enchantment, 0))); p.setEquipment(slot, stack); });}
    private static int clearEnchantments(CommandSourceStack src, CosmicSpawnerPreset.Slot slot) { return withPreset(src, p -> { ItemStack stack = p.getEquipment(slot).copy(); EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.removeIf(enchantment -> true)); p.setEquipment(slot, stack); });}
    private static int setBossFlag(CommandSourceStack src, boolean enabled) { try { var player = src.getPlayerOrException(); var be = getTargetSpawnerBE(src, player, player.level()); if (be == null) return 0; be.setBossOneShot(enabled); src.sendSuccess(() -> Component.literal("Spawner boss one-shot flag " + (enabled ? "enabled." : "disabled.")), false); return 1; } catch (Exception e) { src.sendFailure(Component.literal("Failed: " + e.getMessage())); return 0; } }
    private static int setCapFlag(CommandSourceStack src, int cap) { try { var player = src.getPlayerOrException(); var be = getTargetSpawnerBE(src, player, player.level()); if (be == null) return 0; be.setSpawnerMobCap(cap); src.sendSuccess(() -> Component.literal(cap <= 0 ? "Spawner mob cap disabled." : "Spawner mob cap set to " + cap + "."), false); return 1; } catch (Exception e) { src.sendFailure(Component.literal("Failed: " + e.getMessage())); return 0; } }
    private static int setDelay(CommandSourceStack src, int ticks) { try { var player = src.getPlayerOrException(); var be = getTargetSpawnerBE(src, player, player.level()); if (be == null) return 0; int deadband = Math.max(1, ticks / 2); int min = Math.max(1, ticks - deadband); int max = Math.max(min, ticks + deadband); be.setSpawnerDelayRange(min, max); be.setSpawnerDelayTicks(ticks); src.sendSuccess(() -> Component.literal("Spawner delay set to " + ticks + " ticks (range " + min + "-" + max + ")."), false); return 1; } catch (Exception e) { src.sendFailure(Component.literal("Failed: " + e.getMessage())); return 0; } }

    private static void applyFlag(CosmicSpawnerPreset p, String f, boolean v){ switch (f){case"persistent"->p.setPersistent(v);case"name_visible"->p.setCustomNameVisible(v);case"silent"->p.setSilent(v);case"glowing"->p.setGlowing(v);case"no_ai"->p.setNoAi(v);case"no_gravity"->p.setNoGravity(v);} }
    private static CosmicSpawnerPreset.Slot slot(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c){ return CosmicSpawnerPreset.Slot.fromId(StringArgumentType.getString(c,"slot")); }
    private static int withPreset(CommandSourceStack src, java.util.function.Consumer<CosmicSpawnerPreset> op) { return withPreset(src, op, true, false); }
    private static int withPreset(CommandSourceStack src, java.util.function.Consumer<CosmicSpawnerPreset> op, boolean confirm, boolean refreshInfo) { try { var be=getTargetSpawnerBE(src,src.getPlayerOrException(),src.getPlayerOrException().level()); if(be==null)return 0; var p=be.getSpawnerPreset(); if(p==null){ p=new CosmicSpawnerPreset(); ResourceLocation rl = ResourceLocation.tryParse(be.getSpawnerEntityId()); if(rl!=null)p.setEntityTypeId(rl);} op.accept(p); be.setSpawnerPreset(p); if (confirm) src.sendSuccess(()->Component.literal("Spawner preset updated."),false); if (refreshInfo) info(src); return 1;} catch(Exception e){ src.sendFailure(Component.literal("Failed: "+e.getMessage())); return 0; } }
    private static int info(CommandSourceStack src) {
        try {
            var be = getTargetSpawnerBE(src, src.getPlayerOrException(), src.getPlayerOrException().level());
            if (be == null) return 0;
            var p = be.getSpawnerPreset();
            if (p == null) {
                src.sendSuccess(() -> Component.literal("No preset set. Use /spawner set <entity_type> first.").withStyle(ChatFormatting.RED), false);
                return 1;
            }
            src.sendSuccess(() -> Component.literal("=== Cosmic Spawner Info ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            src.sendSuccess(() -> Component.literal("Mob: ").withStyle(ChatFormatting.YELLOW).append(Component.literal(p.getDisplayEntityTypeId().toString()).withStyle(ChatFormatting.WHITE)), false);
            src.sendSuccess(() -> Component.literal("Spawner Coordinates: ").withStyle(ChatFormatting.YELLOW).append(Component.literal(be.getBlockPos().getX()+", "+be.getBlockPos().getY()+", "+be.getBlockPos().getZ()+" <x,y,z>").withStyle(ChatFormatting.AQUA)), false);

            sendEquipmentInfo(src, p, CosmicSpawnerPreset.Slot.HEAD, "Armor: Helmet");
            sendEquipmentInfo(src, p, CosmicSpawnerPreset.Slot.CHEST, "Armor: Chestplate");
            sendEquipmentInfo(src, p, CosmicSpawnerPreset.Slot.LEGS, "Armor: Leggings");
            sendEquipmentInfo(src, p, CosmicSpawnerPreset.Slot.FEET, "Armor: Boots");
            sendEquipmentInfo(src, p, CosmicSpawnerPreset.Slot.MAINHAND, "Mainhand");
            sendEquipmentInfo(src, p, CosmicSpawnerPreset.Slot.OFFHAND, "Offhand");

            src.sendSuccess(() -> Component.literal("Spawner Properties:").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            src.sendSuccess(() -> Component.literal("  Boss: ").withStyle(ChatFormatting.GRAY).append(Component.literal(Boolean.toString(be.isBossOneShot())).withStyle(ChatFormatting.WHITE)), false);
            src.sendSuccess(() -> Component.literal("  Cap: ").withStyle(ChatFormatting.GRAY).append(Component.literal(Integer.toString(be.getSpawnerMobCap())).withStyle(ChatFormatting.WHITE)), false);
            src.sendSuccess(() -> Component.literal("  Delay: ").withStyle(ChatFormatting.GRAY).append(Component.literal(be.getSpawnerMinSpawnDelay()+"-"+be.getSpawnerMaxSpawnDelay()).withStyle(ChatFormatting.WHITE)), false);

            src.sendSuccess(() -> Component.literal("Drops:").withStyle(ChatFormatting.GREEN), false);
            for (var s : CosmicSpawnerPreset.Slot.values()) src.sendSuccess(() -> dropRow(p, s), false);

            src.sendSuccess(() -> Component.literal("Intrinsic Drops (from NBT overrides):").withStyle(ChatFormatting.AQUA), false);
            if (p.getIntrinsicDropChances().isEmpty()) src.sendSuccess(() -> Component.literal("  None set in spawner NBT.").withStyle(ChatFormatting.DARK_GRAY), false);
            else p.getIntrinsicDropChances().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(e -> src.sendSuccess(() -> intrinsicDropRow(e.getKey(), e.getValue()), false));
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int adjustDrop(CommandSourceStack src, CosmicSpawnerPreset.Slot slot, float delta) {
        return withPreset(src, p -> p.setDropChance(slot, p.getDropChance(slot) + delta), false, true);
    }

    private static int adjustIntrinsic(CommandSourceStack src, ResourceLocation itemId, float delta) {
        return withPreset(src, p -> p.setIntrinsicDropChance(itemId, p.getIntrinsicDropChances().getOrDefault(itemId, 0f) + delta), false, true);
    }

    private static int reset(CommandSourceStack src){ try{ var be=getTargetSpawnerBE(src,src.getPlayerOrException(),src.getPlayerOrException().level()); if(be==null)return 0; be.clearSpawnerPreset(); src.sendSuccess(()->Component.literal("Spawner preset reset."),false); return 1;}catch(Exception e){return 0;}}
    private static java.util.stream.Stream<ResourceLocation> srcRegistryEnchantments(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c){ return c.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElementIds().map(k->k.location()); }
    private static CompletableFuture<Suggestions> suggestEntities(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggestResource(Stream.concat(BuiltInRegistries.ENTITY_TYPE.keySet().stream(), Stream.of(CosmicSpawnerPreset.ILLAGER_CAPTAIN_ID)),b);}
    private static CompletableFuture<Suggestions> suggestItems(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(),b);}
    private static CompletableFuture<Suggestions> suggestEnchantments(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggestResource(srcRegistryEnchantments(c),b);}
    private static CompletableFuture<Suggestions> suggestSlots(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggest(Arrays.stream(CosmicSpawnerPreset.Slot.values()).map(s->s.id),b);}
    private static CompletableFuture<Suggestions> suggestRoot(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){ return SharedSuggestionProvider.suggest(Arrays.asList("help","set","name","flag","equip","enchant","drop","drops","delay","info","reset"), b); }
    private static void sendEquipmentInfo(CommandSourceStack src, CosmicSpawnerPreset p, CosmicSpawnerPreset.Slot slot, String title) {
        ItemStack st = p.getEquipment(slot);
        src.sendSuccess(() -> Component.literal(title + ":").withStyle(ChatFormatting.BLUE), false);
        if (st.isEmpty()) {
            src.sendSuccess(() -> Component.literal("  (empty)").withStyle(ChatFormatting.DARK_GRAY), false);
            return;
        }
        String custom = st.hasCustomHoverName() ? st.getHoverName().getString() : "unnamed";
        src.sendSuccess(() -> Component.literal("  " + st.getItemHolder().unwrapKey().map(k -> k.location().toString()).orElse(st.getDisplayName().getString()) + " (" + custom + ")").withStyle(ChatFormatting.WHITE), false);
        var enchMap = EnchantmentHelper.getEnchantmentsForCrafting(st);
        if (enchMap.isEmpty()) {
            src.sendSuccess(() -> Component.literal("    No enchantments").withStyle(ChatFormatting.DARK_GRAY), false);
        } else {
            String enchLine = enchMap.entrySet().stream()
                    .map(e -> e.getKey().value().description().getString() + " " + e.getIntValue())
                    .sorted()
                    .reduce((a, b) -> a + ", " + b).orElse("");
            src.sendSuccess(() -> Component.literal("    " + enchLine).withStyle(ChatFormatting.AQUA), false);
        }
    }

    private static MutableComponent dropRow(CosmicSpawnerPreset p, CosmicSpawnerPreset.Slot slot) {
        float chance = p.getDropChance(slot);
        MutableComponent row = Component.literal("  " + slot.id + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(Math.round(chance * 100f) + "% ").withStyle(ChatFormatting.WHITE));
        row.append(chanceButton(slot, 0.05f, "+")).append(Component.literal(" ")).append(chanceButton(slot, -0.05f, "-"));
        return row;
    }

    private static MutableComponent intrinsicDropRow(ResourceLocation itemId, float current) {
        float pct = Math.round(current * 100f);
        MutableComponent row = Component.literal("  " + itemId + ": ").withStyle(ChatFormatting.GRAY).append(Component.literal((int) pct + "% ").withStyle(ChatFormatting.WHITE));
        row.append(intrinsicChanceButton(itemId, 0.05f, "+")).append(Component.literal(" ")).append(intrinsicChanceButton(itemId, -0.05f, "-"));
        return row;
    }

    private static MutableComponent intrinsicChanceButton(ResourceLocation itemId, float delta, String symbol) { String cmd = "/spawner adjustintrinsic " + itemId + " " + String.format(java.util.Locale.ROOT, "%.2f", delta); return Component.literal("["+symbol+"]").withStyle(style -> style.withColor(delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED).withClickEvent(new ClickEvent.RunCommand(cmd)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Adjust " + itemId + " by " + Math.round(delta*100f) + "% and refresh info")))); }

    private static MutableComponent chanceButton(CosmicSpawnerPreset.Slot slot, float delta, String symbol) { String cmd = "/spawner adjustdrop " + slot.id + " " + String.format(java.util.Locale.ROOT, "%.2f", delta); return Component.literal("["+symbol+"]").withStyle(style -> style.withColor(delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED).withClickEvent(new ClickEvent.RunCommand(cmd)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Adjust "+slot.id+" by "+Math.round(delta*100f)+"% and refresh info")))); }

    private static CosmicSpawnerBlockEntity getTargetSpawnerBE(CommandSourceStack src, ServerPlayer player, Level level) { final BlockHitResult hit = raycast(player, 5.0D); if (hit == null || hit.getType() == HitResult.Type.MISS) { src.sendFailure(Component.literal("Look at a Cosmic Spawner within 5 blocks.")); return null; } final BlockPos pos = hit.getBlockPos(); if (!(level.getBlockState(pos).getBlock() instanceof CosmicMobSpawnerBlock)) { src.sendFailure(Component.literal("Target block is not a Cosmic Spawner.")); return null; } if (!(level.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be)) { src.sendFailure(Component.literal("Cosmic Spawner block entity missing at target.")); return null; } return be; }
    private static BlockHitResult raycast(ServerPlayer p, double range) { ClipContext ctx = new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p); HitResult hr = p.level().clip(ctx); return hr instanceof BlockHitResult bhr ? bhr : null; }
}
