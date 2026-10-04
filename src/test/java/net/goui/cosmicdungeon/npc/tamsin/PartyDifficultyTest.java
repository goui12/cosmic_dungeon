package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.dungeon.DungeonDifficulty;
import net.goui.cosmicdungeon.network.PartyPayloads;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class PartyDifficultyTest {
    private final UUID leader = UUID.randomUUID(), member = UUID.randomUUID();
    private D1PartyLobby lobby() {
        var lobby = new D1PartyLobby();
        var anchor = new D1PartyLobby.Anchor(UUID.randomUUID(), "minecraft:overworld", 0);
        assertNull(lobby.create(leader, anchor, 0, "Difficulty group"));
        assertNull(lobby.invite(leader, member, anchor, 0, 100, 6));
        assertNull(lobby.accept(member, lobby.invitation(member).token(), 1));
        assertNull(lobby.joinAccepted(member, 2, 6, true));
        return lobby;
    }
    @Test void onlyLeaderWithCurrentRevisionCanChooseAValidTier() {
        var l = lobby(); var p = l.party(leader);
        assertEquals(DungeonDifficulty.HARD, p.difficulty());
        assertNotNull(l.difficulty(member, p.revision(), "EASY"));
        assertNotNull(l.difficulty(leader, p.revision() - 1, "EASY"));
        assertNotNull(l.difficulty(leader, p.revision(), "FAKE"));
        assertEquals(DungeonDifficulty.HARD, p.difficulty());
        assertNull(l.difficulty(leader, p.revision(), "INSANE"));
        assertEquals(DungeonDifficulty.INSANE, p.difficulty());
    }
    @Test void difficultyChangeClearsReadinessAndRejectsOldConfirmations() {
        var l = lobby(); var p = l.party(leader);
        assertNull(l.begin(leader, p.revision(), Map.of(leader, "bogatyr", member, "pyroclast")));
        assertNull(l.ready(member, p.revision())); long previous = p.revision();
        assertNull(l.difficulty(leader, previous, "RIDICULOUS"));
        assertTrue(p.ready().isEmpty()); assertEquals(D1PartyLobby.Phase.ASSEMBLY, p.phase());
        assertNotNull(l.ready(member, previous));
    }
    @Test void queueAndPreparationFreezeDifficulty() {
        var l = lobby(); var p = l.party(leader);
        assertNull(l.begin(leader, p.revision(), Map.of(leader, "bogatyr", member, "pyroclast")));
        assertNull(l.ready(leader, p.revision())); assertNull(l.ready(member, p.revision()));
        assertNull(l.queue(leader, p.revision()));
        assertNotNull(l.difficulty(leader, p.revision(), "EASY"));
        l.startCountdown(p, 0, 1); assertTrue(l.prepare(p, 1));
        assertNotNull(l.difficulty(leader, p.revision(), "EASY"));
        assertEquals(DungeonDifficulty.HARD, p.difficulty());
    }
    @Test void protocolViewCarriesDifficultyForMenuAndHud() {
        for (int container : List.of(-1, 5)) for (var tier : DungeonDifficulty.values()) {
            var view = new PartyPayloads.View(container, new PartyPayloads.State(42, "READY_CHECK", true, 6, 0, -1),
                    List.of(), new PartyPayloads.Invite("", "", false, false), PartyPayloads.Recruitment.EMPTY, tier.name());
            var buffer = Unpooled.buffer();
            try { PartyPayloads.View.STREAM_CODEC.encode(buffer, view); assertEquals(view, PartyPayloads.View.STREAM_CODEC.decode(buffer)); }
            finally { buffer.release(); }
        }
    }
}
