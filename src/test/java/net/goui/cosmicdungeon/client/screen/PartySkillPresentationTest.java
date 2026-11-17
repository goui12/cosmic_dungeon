package net.goui.cosmicdungeon.client.screen;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PartySkillPresentationTest {
    @Test void identityPreservesNameYellowClassAndBlueSignatureForEveryClass() {
        var classes=Map.of("Theurgist","PP","Venefex","NP","Bogatyr","WLF","Pyroclast","FW","Dragoon","CL","Judicator","SM");
        classes.forEach((name,abbreviation)->{
            var identity=PartySkillPresentation.identity("Cameron",Component.literal(name),
                    new PartySkills(abbreviation,35,List.of()),false);
            assertEquals("Cameron / "+name+" / "+abbreviation+" – 35",identity.getString());
            Map<String,Integer> colors=new HashMap<>();
            identity.visit((style,text)->{if(!text.isBlank())colors.put(text,style.getColor().getValue());return Optional.empty();},Style.EMPTY);
            assertEquals(ChatFormatting.WHITE.getColor(),colors.get("Cameron"));
            assertEquals(ChatFormatting.YELLOW.getColor(),colors.get(name));
            assertEquals(ChatFormatting.BLUE.getColor(),colors.get(abbreviation+" – 35"));
        });
        assertEquals("Alaric",PartySkillPresentation.identity("Alaric",Component.empty(),PartySkills.NONE,false).getString());
    }
    @Test void effectDetailsUseApprovedLabelsWinningCasterAndReadableLocalCountdown() {
        var effect=new PartyVitals.Effect("cosmicdungeon:vitality",1,"Favilla Vitas","+15% maximum health","Alaric",40000);
        var lines=PartySkillPresentation.effect(effect,1240);
        assertEquals(List.of("Favilla Vitas — +15% maximum health","Cast by Alaric","Remaining: 32m 18s"),
                lines.stream().map(Component::getString).toList());
        assertEquals(ChatFormatting.GREEN.getColor(),lines.get(0).getStyle().getColor().getValue());
        assertEquals(ChatFormatting.BLUE.getColor(),lines.get(1).getStyle().getColor().getValue());
        assertEquals(ChatFormatting.YELLOW.getColor(),lines.get(2).getStyle().getColor().getValue());
        assertEquals("Remaining: 32m 17s",PartySkillPresentation.effect(effect,1260).get(2).getString());
    }
    @Test void unknownProvenanceAndDurationAreNeverGuessedAndInfiniteIsExplicit() {
        var old=new PartyVitals.Effect("minecraft:regeneration",0,"Regeneration","I","Unknown",0);
        assertEquals("Cast by Unknown",PartySkillPresentation.effect(old,500).get(1).getString());
        assertEquals("Remaining: Unknown",PartySkillPresentation.effect(old,500).get(2).getString());
        var infinite=new PartyVitals.Effect("minecraft:regeneration",0,"Regeneration","I","Unknown",-1);
        assertEquals("Remaining: Infinite",PartySkillPresentation.effect(infinite,Long.MAX_VALUE).get(2).getString());
    }
    @Test void clockAdvancesBetweenPacketsAndReanchorsWithoutCrossSnapshotDrift() {
        assertEquals(1000,PartySkillPresentation.now(1000,80,80));
        assertEquals(1020,PartySkillPresentation.now(1000,80,100));
        assertEquals(2010,PartySkillPresentation.now(2000,110,120));
        assertEquals(2000,PartySkillPresentation.now(2000,110,0));
        assertEquals(Long.MAX_VALUE,PartySkillPresentation.now(Long.MAX_VALUE-2,0,5));
        assertEquals("0s",PartySkillPresentation.duration(-1));
        assertEquals("1s",PartySkillPresentation.duration(1));
        assertEquals("1m 0s",PartySkillPresentation.duration(1200));
        assertEquals("1h 2m 3s",PartySkillPresentation.duration(74460));
    }
    @Test void fiveMinuteBuffDisplayIsIndependentOfEffectDurationAndCharge() {
        var buff=new PartySkills.Ability("Flurry of Fangs",7000,6000,0,0,false);
        assertEquals("Flurry of Fangs: 5m 0s / 5m 0s",PartySkillPresentation.ability(buff,1000));
        assertEquals(5,PartySkillPresentation.segments(buff));
        assertEquals(0,PartySkillPresentation.fraction(buff,1000));
        assertEquals(.5,PartySkillPresentation.fraction(buff,4000));
        assertEquals("Flurry of Fangs: Ready",PartySkillPresentation.ability(buff,7000));
        assertEquals(1,PartySkillPresentation.fraction(buff,9000));
        var charging=new PartySkills.Ability("Chain Lightning",0,0,99,100,false);
        assertEquals("Chain Lightning: 99/100 charge",PartySkillPresentation.ability(charging,Long.MAX_VALUE));
        assertEquals(.99,PartySkillPresentation.fraction(charging,Long.MAX_VALUE));
        assertEquals(10,PartySkillPresentation.segments(charging));
        var armed=new PartySkills.Ability("Chain Lightning",0,0,100,100,true);
        assertEquals("Chain Lightning: 100/100 — Armed",PartySkillPresentation.ability(armed,0));
    }
    @Test void everyAbilityRowIsIncludedInScrollGeometryAtNarrowWidths() {
        var abilities=List.of(new PartySkills.Ability("Gravehammer",0,0,10,10,false),
                new PartySkills.Ability("Aegis",7000,6000,0,0,false));
        var v=new PartyVitals(20,20,"ACTIVE",List.of(new PartyVitals.Effect("minecraft:strength",0)),0,
                1000,new PartySkills("SM",1,abilities));
        for(int width:List.of(24,80,148,224)) {
            var box=new PartyHealthLayout(8,8,width,180,1,0);
            assertEquals(box.rowHeight(1)+32,PartyHealthHud.rowHeight(v,box));
            int content=9*PartyHealthHud.rowHeight(v,box);
            assertTrue(box.maxScroll(content)>0);
            assertEquals(box.maxScroll(content),box.scrollAt(box.y()+box.height()-box.thumbHeight(content),content));
        }
    }
}
