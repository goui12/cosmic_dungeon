package net.goui.cosmicdungeon.npc.tamsin;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.goui.cosmicdungeon.network.PartyVitals;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;

/** Native player and mercenary vitals; no authenticated client, inventory or world persistence changes. */
public final class PartyHealthGameTests {
    private PartyHealthGameTests() {}
    private static void check(GameTestHelper h,boolean value,String message){h.assertTrue(value,Component.literal(message));}
    public static void player(GameTestHelper h) {
        var profile=new GameProfile(UUID.randomUUID(),"HudSubject");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        var channel=new io.netty.channel.embedded.EmbeddedChannel();
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public io.netty.channel.Channel channel(){return channel;}
            @Override public boolean isConnected(){return true;}
        };
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
        };
        try {
            p.setHealth(7.5F);
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,200,1));
            p.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));
            var first=PartyVitalsSnapshot.capture(p,true);
            check(h,first.health()==7.5F&&first.maxHealth()==p.getMaxHealth()&&first.effects().size()==2,"Human HP and both effect categories are captured");
            p.removeEffect(MobEffects.STRENGTH);
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,199,1));
            check(h,first.equals(PartyVitalsSnapshot.capture(p,true)),"Duration-only changes do not resend the party snapshot");
            p.removeEffect(MobEffects.POISON);p.setHealth(15);
            var changed=PartyVitalsSnapshot.capture(p,true);
            check(h,!changed.equals(first)&&changed.effects().size()==1&&changed.health()==15,"Healing and effect removal replace the snapshot");
            check(h,PartyVitalsSnapshot.capture(p,false).equals(PartyVitals.UNLOADED),"Online members outside the run have no stale vitals");
            p.setHealth(0);
            check(h,PartyVitalsSnapshot.capture(p,true).equals(PartyVitals.DEAD),"Death clears health and effects");
            check(h,PartyVitalsSnapshot.capture(null,true).equals(PartyVitals.OFFLINE),"Offline members have no stale vitals");
            p.removeAllEffects();p.setHealth(20);
            check(h,PartyVitalsSnapshot.capture(p,true).state().equals("ACTIVE"),"Recovered online members regain live vitals");
        } finally {p.discard();channel.finishAndReleaseAll();}
        h.succeed();
    }
    public static void mercenary(GameTestHelper h) {
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),h.getLevel());
        merc.initialize(1,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"theurgist",2,50));
        try {
            merc.setHealth(5);
            merc.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0));
            var before=PartyVitalsSnapshot.capture(merc,true);
            check(h,before.health()==5&&before.effects().size()==1,"Companion effects use the same native capture");
            merc.removeAllEffects();merc.setHealth(merc.getMaxHealth());
            var after=PartyVitalsSnapshot.capture(merc,true);
            check(h,after.effects().isEmpty()&&after.health()==after.maxHealth(),"Companion recovery removes expired icons");
            merc.discard();
            check(h,PartyVitalsSnapshot.capture(merc,true).equals(PartyVitals.UNLOADED),"Removed companions are never shown alive");
        } finally {merc.discard();}
        h.succeed();
    }
}
