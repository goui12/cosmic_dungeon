package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.goui.cosmicdungeon.network.PartySkills;
import net.goui.cosmicdungeon.network.PartyVitals;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Read-only formatting of server snapshots; local clocks never authorize an action. */
public final class PartySkillPresentation {
    private PartySkillPresentation() {}
    public static long now(long sampledAt,long receivedAt,long clientTicks) {
        long elapsed=Math.max(0,clientTicks-receivedAt);
        return sampledAt>Long.MAX_VALUE-elapsed?Long.MAX_VALUE:sampledAt+elapsed;
    }
    public static long remaining(long deadline,long now) { return deadline<=now?0:deadline-now; }
    public static String duration(long ticks) {
        long seconds=Math.max(0,ticks)/20+(ticks>0&&ticks%20!=0?1:0);
        if(seconds>=3600)return seconds/3600+"h "+seconds%3600/60+"m "+seconds%60+"s";
        if(seconds>=60)return seconds/60+"m "+seconds%60+"s";
        return seconds+"s";
    }
    public static Component identity(String name,Component className,PartySkills skills,boolean leader) {
        var result=Component.literal((leader?"* ":"")+name).withStyle(ChatFormatting.WHITE);
        if(!className.getString().isBlank())result.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(className.copy().withStyle(ChatFormatting.YELLOW));
        if(!skills.abbreviation().isBlank())result.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(skills.abbreviation()+" – "+skills.level()).withStyle(ChatFormatting.BLUE));
        return result;
    }
    public static void drawClipped(GuiGraphics graphics,Font font,Component text,int x,int y,int width) {
        graphics.drawString(font,Language.getInstance().getVisualOrder(font.substrByWidth(text,Math.max(1,width))),
                x,y,0xFFFFFFFF,false);
    }
    public static List<Component> effect(PartyVitals.Effect effect,long now) {
        Component name=effect.displayName().isBlank()
                ?BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effect.id()))
                    .<Component>map(h->h.value().getDisplayName()).orElse(Component.literal(effect.id()))
                :Component.literal(effect.displayName());
        String amount=effect.amount().isBlank()?Integer.toString(effect.amplifier()+1):effect.amount();
        String remaining=effect.expiresAt()<0?"Infinite":effect.expiresAt()==0?"Unknown":duration(remaining(effect.expiresAt(),now));
        return List.of(name.copy().append(" — "+amount).withStyle(ChatFormatting.GREEN),
                Component.literal("Cast by "+(effect.caster().isBlank()?"Unknown":effect.caster())).withStyle(ChatFormatting.BLUE),
                Component.literal("Remaining: "+remaining).withStyle(ChatFormatting.YELLOW));
    }
    public static String ability(PartySkills.Ability ability,long now) {
        if(ability.maxCharge()>0)return ability.title()+": "+ability.charge()+"/"+ability.maxCharge()
                +(ability.armed()?" — Armed":ability.charge()==ability.maxCharge()?" — Ready":" charge");
        long remaining=remaining(ability.readyAt(),now);
        return ability.title()+": "+(remaining>0?duration(remaining)+" / "+duration(ability.cooldownTicks()):"Ready");
    }
    public static double fraction(PartySkills.Ability ability,long now) {
        if(ability.maxCharge()>0)return (double)ability.charge()/ability.maxCharge();
        if(ability.cooldownTicks()==0)return 1;
        return Math.max(0,Math.min(1,1-(double)remaining(ability.readyAt(),now)/ability.cooldownTicks()));
    }
    public static int segments(PartySkills.Ability ability) { return ability.maxCharge()>0?10:5; }
    public static void bar(GuiGraphics g,PartySkills.Ability ability,long now,int x,int y,int width) {
        int segments=segments(ability);double fill=fraction(ability,now)*segments;
        for(int i=0;i<segments;i++) {
            int left=x+i*width/segments,right=x+(i+1)*width/segments-1;
            if(right<=left)continue;
            g.fill(left,y,right,y+3,0xFF42424E);
            int amount=(int)Math.round((right-left)*Math.max(0,Math.min(1,fill-i)));
            if(amount>0)g.fill(left,y,left+amount,y+3,ability.armed()?0xFF90CAF9:0xFF66CC88);
        }
    }
}
