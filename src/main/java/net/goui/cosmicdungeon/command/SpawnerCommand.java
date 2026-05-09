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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

public final class SpawnerCommand {
    private SpawnerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("spawner").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.literal("entity")
                        .then(Commands.argument("entity_type", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestEntities)
                                .executes(c -> withPreset(c.getSource(), p -> {
                                    ResourceLocation rl = ResourceLocationArgument.getId(c, "entity_type");
                                    p.setEntityTypeId(rl);
                                })))) )
                .then(Commands.literal("name").then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> withPreset(c.getSource(), p -> p.setCustomName(Component.literal(StringArgumentType.getString(c, "name")))))))
                        .then(Commands.literal("clear").executes(c -> withPreset(c.getSource(), p -> p.setCustomName(null)))))
                .then(Commands.literal("flag").then(Commands.argument("flag", StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(Arrays.asList("persistent","name_visible","silent","glowing","no_ai","no_gravity"),b))
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(c -> withPreset(c.getSource(), p -> applyFlag(p, StringArgumentType.getString(c,"flag"), BoolArgumentType.getBool(c,"value")))))))
                .then(Commands.literal("equip").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots)
                                .then(Commands.argument("item", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestItems).executes(c -> withPreset(c.getSource(), p -> p.setEquipment(slot(c), BuiltInRegistries.ITEM.get(ResourceLocationArgument.getId(c,"item")).map(ItemStack::new).orElse(ItemStack.EMPTY)))))
                                .then(Commands.literal("fromhand").executes(c -> withPreset(c.getSource(), p -> p.setEquipment(slot(c), c.getSource().getPlayerOrException().getMainHandItem().copy())))))
                        .then(Commands.literal("clear").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).executes(c -> withPreset(c.getSource(), p -> p.setEquipment(slot(c), ItemStack.EMPTY))))
                                .then(Commands.literal("all").executes(c -> withPreset(c.getSource(), p -> { for (var s : CosmicSpawnerPreset.Slot.values()) p.setEquipment(s, ItemStack.EMPTY);})))) )
                .then(Commands.literal("enchant").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots)
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestEnchantments)
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 255)).executes(c -> enchant(c.getSource(), slot(c), ResourceLocationArgument.getId(c,"enchantment"), IntegerArgumentType.getInteger(c,"level"), false))
                                        .then(Commands.argument("allowUnsafe", BoolArgumentType.bool()).executes(c -> enchant(c.getSource(), slot(c), ResourceLocationArgument.getId(c,"enchantment"), IntegerArgumentType.getInteger(c,"level"), BoolArgumentType.getBool(c,"allowUnsafe")))))))
                        .then(Commands.literal("remove").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).then(Commands.argument("enchantment", ResourceLocationArgument.id()).suggests(SpawnerCommand::suggestEnchantments).executes(c -> removeEnchant(c.getSource(), slot(c), ResourceLocationArgument.getId(c, "enchantment")))))
                        .then(Commands.literal("clear").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).executes(c -> clearEnchantments(c.getSource(), slot(c))))))
                .then(Commands.literal("drop").then(Commands.argument("slot", StringArgumentType.word()).suggests(SpawnerCommand::suggestSlots).then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> p.setDropChance(slot(c), FloatArgumentType.getFloat(c,"chance"))))))
                        .then(Commands.literal("armor").then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> {float f=FloatArgumentType.getFloat(c,"chance"); p.setDropChance(CosmicSpawnerPreset.Slot.HEAD,f);p.setDropChance(CosmicSpawnerPreset.Slot.CHEST,f);p.setDropChance(CosmicSpawnerPreset.Slot.LEGS,f);p.setDropChance(CosmicSpawnerPreset.Slot.FEET,f);}))))
                        .then(Commands.literal("hands").then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> {float f=FloatArgumentType.getFloat(c,"chance"); p.setDropChance(CosmicSpawnerPreset.Slot.MAINHAND,f);p.setDropChance(CosmicSpawnerPreset.Slot.OFFHAND,f);}))))
                        .then(Commands.literal("all").then(Commands.argument("chance", FloatArgumentType.floatArg(0f,1f)).executes(c -> withPreset(c.getSource(), p -> {float f=FloatArgumentType.getFloat(c,"chance"); for (var s: CosmicSpawnerPreset.Slot.values()) p.setDropChance(s,f);})))) )
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
        );
    }

    private static int enchant(CommandSourceStack src, CosmicSpawnerPreset.Slot slot, ResourceLocation enchId, int level, boolean allowUnsafe) { return withPreset(src, p -> {
        ItemStack stack = p.getEquipment(slot).copy(); if (stack.isEmpty()) stack = new ItemStack(Items.STICK);
        var enchOpt = src.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchId);
        if (enchOpt.isPresent()) { Holder<Enchantment> ench = enchOpt.get(); if (allowUnsafe || stack.supportsEnchantment(ench)) stack.enchant(ench, level); }
        p.setEquipment(slot, stack);
    });}
    private static int removeEnchant(CommandSourceStack src, CosmicSpawnerPreset.Slot slot, ResourceLocation enchId) { return withPreset(src, p -> {
        ItemStack stack = p.getEquipment(slot).copy();
        var enchOpt = src.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchId);
        enchOpt.ifPresent(enchantment -> EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(enchantment, 0)));
        p.setEquipment(slot, stack);
    });}
    private static int clearEnchantments(CommandSourceStack src, CosmicSpawnerPreset.Slot slot) { return withPreset(src, p -> {
        ItemStack stack = p.getEquipment(slot).copy();
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.removeIf(enchantment -> true));
        p.setEquipment(slot, stack);
    });}
    private static void applyFlag(CosmicSpawnerPreset p, String f, boolean v){ switch (f){case"persistent"->p.setPersistent(v);case"name_visible"->p.setCustomNameVisible(v);case"silent"->p.setSilent(v);case"glowing"->p.setGlowing(v);case"no_ai"->p.setNoAi(v);case"no_gravity"->p.setNoGravity(v);} }
    private static CosmicSpawnerPreset.Slot slot(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c){ return CosmicSpawnerPreset.Slot.fromId(StringArgumentType.getString(c,"slot")); }
    private static int withPreset(CommandSourceStack src, java.util.function.Consumer<CosmicSpawnerPreset> op) { try { var be=getTargetSpawnerBE(src,src.getPlayerOrException(),src.getPlayerOrException().level()); if(be==null)return 0; var p=be.getSpawnerPreset(); if(p==null){ p=new CosmicSpawnerPreset(); ResourceLocation rl = ResourceLocation.tryParse(be.getSpawnerEntityId()); if(rl!=null)p.setEntityTypeId(rl);} op.accept(p); be.setSpawnerPreset(p); src.sendSuccess(()->Component.literal("Spawner preset updated."),false); return 1;} catch(Exception e){ src.sendFailure(Component.literal("Failed: "+e.getMessage())); return 0; } }
    private static int info(CommandSourceStack src){ try{ var be=getTargetSpawnerBE(src,src.getPlayerOrException(),src.getPlayerOrException().level()); if(be==null)return 0; var p=be.getSpawnerPreset(); if(p==null){ src.sendSuccess(()->Component.literal("No preset set."),false); return 1;} src.sendSuccess(()->Component.literal("Preset entity: "+p.getEntityTypeId()),false); for(var s: CosmicSpawnerPreset.Slot.values()) src.sendSuccess(()->Component.literal(s.id+" drop="+p.getDropChance(s)+" item="+p.getEquipment(s)),false); return 1;}catch(Exception e){return 0;}}
    private static int reset(CommandSourceStack src){ try{ var be=getTargetSpawnerBE(src,src.getPlayerOrException(),src.getPlayerOrException().level()); if(be==null)return 0; be.clearSpawnerPreset(); src.sendSuccess(()->Component.literal("Spawner preset reset."),false); return 1;}catch(Exception e){return 0;}}
    private static java.util.stream.Stream<ResourceLocation> srcRegistryEnchantments(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c){ return c.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElementIds().map(k->k.location()); }
    private static CompletableFuture<Suggestions> suggestEntities(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(),b);}    private static CompletableFuture<Suggestions> suggestItems(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(),b);}    private static CompletableFuture<Suggestions> suggestEnchantments(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggestResource(srcRegistryEnchantments(c),b);}    private static CompletableFuture<Suggestions> suggestSlots(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b){return SharedSuggestionProvider.suggest(Arrays.stream(CosmicSpawnerPreset.Slot.values()).map(s->s.id),b);}    
    private static CosmicSpawnerBlockEntity getTargetSpawnerBE(CommandSourceStack src, ServerPlayer player, Level level) { final BlockHitResult hit = raycast(player, 5.0D); if (hit == null || hit.getType() == HitResult.Type.MISS) { src.sendFailure(Component.literal("Look at a Cosmic Spawner within 5 blocks.")); return null; } final BlockPos pos = hit.getBlockPos(); if (!(level.getBlockState(pos).getBlock() instanceof CosmicMobSpawnerBlock)) { src.sendFailure(Component.literal("Target block is not a Cosmic Spawner.")); return null; } if (!(level.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be)) { src.sendFailure(Component.literal("Cosmic Spawner block entity missing at target.")); return null; } return be; }
    private static BlockHitResult raycast(ServerPlayer p, double range) { ClipContext ctx = new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p); HitResult hr = p.level().clip(ctx); return hr instanceof BlockHitResult bhr ? bhr : null; }
}
