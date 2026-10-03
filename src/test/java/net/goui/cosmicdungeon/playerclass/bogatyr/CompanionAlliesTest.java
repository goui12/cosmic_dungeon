package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanionAlliesTest {
    @Test void playersAndFriendlyNpcKindsDoNotRequirePartyMembership() {
        assertTrue(CompanionAllies.friendlyType(ServerPlayer.class));
        assertTrue(CompanionAllies.friendlyType(Villager.class));
        assertTrue(CompanionAllies.friendlyType(WanderingTrader.class));
        assertTrue(CompanionAllies.friendlyType(Allay.class));
        assertTrue(CompanionAllies.friendlyType(IronGolem.class));
        assertTrue(CompanionAllies.friendlyType(SnowGolem.class));
    }
    @Test void hostileMobsAreNotFriendlyAndWildWolvesStillNeedOwnership() {
        assertFalse(CompanionAllies.friendlyType(Creeper.class));
        assertFalse(CompanionAllies.friendlyType(Wolf.class));
        assertFalse(CompanionAllies.friendly(null));
    }
}
