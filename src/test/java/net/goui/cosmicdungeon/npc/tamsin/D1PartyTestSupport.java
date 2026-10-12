package net.goui.cosmicdungeon.npc.tamsin;
import java.util.UUID;
/** Older readiness fixtures now make the required explicit personal NO choice. */
final class D1PartyTestSupport {
    private D1PartyTestSupport() {}
    static void consentAll(D1PartyLobby lobby, UUID member) {
        var party=lobby.party(member);
        for(var id:party.members()) {
            var error=lobby.resourceSharing(id,party.revision(),party.id().toString(),false);
            if(error!=null)throw new AssertionError(error);
        }
    }
}
