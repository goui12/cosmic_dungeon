package net.goui.cosmicdungeon.potion;

import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.network.CompanionshipTeleportPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

public class PotionOfCompanionshipItem extends Item {
    public static final int COOLDOWN_TICKS = 20 * 60 * 5;
    public PotionOfCompanionshipItem(Properties properties) { super(properties.stacksTo(16)); }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && !canDrink(sp)) return InteractionResult.FAIL;
        player.startUsingItem(hand); return InteractionResult.CONSUME;
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer sp)) return stack;
        if (!canDrink(sp)) return stack;
        sp.awardStat(Stats.ITEM_USED.get(this));
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        boolean emptied = stack.isEmpty();
        ItemStack result = emptied ? new ItemStack(Items.GLASS_BOTTLE) : stack;
        if (!emptied && !sp.getAbilities().instabuild && !sp.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) sp.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        sp.addEffect(new MobEffectInstance(ModMobEffects.TELEPORT_COOLDOWN, COOLDOWN_TICKS, 0, false, true, true));
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        openSelection(sp); return result;
    }
    private static boolean canDrink(ServerPlayer sp) {
        MobEffectInstance cooldown = sp.getEffect(ModMobEffects.TELEPORT_COOLDOWN);
        if (cooldown != null && cooldown.getDuration() > 0) {
            int seconds = (cooldown.getDuration() + 19) / 20;
            sp.sendSystemMessage(Component.literal("Teleportation on cooldown: " + (seconds / 60) + " minutes " + (seconds % 60) + " Seconds").withStyle(ChatFormatting.RED));
            return false;
        }
        if (DungeonLifecycleService.findActiveRunForPlayer(sp).isEmpty()) {
            sp.sendSystemMessage(Component.literal("You’re not part of an active dungeon group").withStyle(ChatFormatting.RED)); return false;
        }
        return true;
    }
    private static void openSelection(ServerPlayer sp) {
        DungeonRunRegistryData.RunRecord run = DungeonLifecycleService.findActiveRunForPlayer(sp).orElse(null); if (run == null) return;
        CompanionshipTeleportService.beginSelection(sp, COOLDOWN_TICKS);
        List<CompanionshipTeleportPayloads.PlayerEntry> entries = new ArrayList<>();
        for (var id : run.orderedPlayers()) {
            if (id.equals(sp.getUUID())) continue;
            ServerPlayer other = sp.level().getServer().getPlayerList().getPlayer(id);
            if (other != null) {
                entries.add(new CompanionshipTeleportPayloads.PlayerEntry(id, other.getGameProfile().name()));
            }
        }
        PacketDistributor.sendToPlayer(sp, new CompanionshipTeleportPayloads.S2C_Open(entries));
    }
}
