package net.goui.cosmicdungeon.dungeon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class DungeonForfeitTest {
    private List<UUID> roster(int size) {
        return IntStream.rangeClosed(1, size).mapToObj(i -> new UUID(0, i)).toList();
    }

    @Test void thresholdsRoundUpForEverySupportedPartySize() {
        int[] thresholds = {1, 2, 2, 3, 4, 4};
        for (int size = 1; size <= 6; size++) {
            var members = roster(size);
            var ballot = new DungeonForfeitBallot(members, 1200);
            assertEquals(thresholds[size - 1], ballot.required());
            assertEquals(0, ballot.yesCount(), "Opening is not consent, including solo");
            for (int i = 0; i < thresholds[size - 1]; i++) {
                assertEquals(DungeonForfeitBallot.State.OPEN, ballot.refresh(members, 0));
                assertTrue(ballot.vote(members.get(i), true, members, 1));
            }
            assertEquals(DungeonForfeitBallot.State.PASSED, ballot.refresh(members, 2));
            assertFalse(ballot.vote(members.getFirst(), true, members, 3), "A passed vote cannot run again");
        }
    }

    @Test void duplicateAndOutsiderVotesCannotSupplyMissingConsent() {
        var members = roster(3);
        var ballot = new DungeonForfeitBallot(members, 1200);
        assertFalse(ballot.vote(new UUID(1, 1), true, members, 0));
        assertTrue(ballot.vote(members.getFirst(), true, members, 0));
        assertFalse(ballot.vote(members.getFirst(), true, members, 1));
        assertFalse(ballot.vote(members.getFirst(), false, members, 1));
        assertEquals(1, ballot.yesCount());
        assertEquals(DungeonForfeitBallot.State.OPEN, ballot.refresh(members, 1));
    }

    @Test void offlineAbstentionsRemainInTheFixedDenominator() {
        var members = roster(4);
        var ballot = new DungeonForfeitBallot(members, 1200);
        // Only two members respond; the other two remain on the run roster while disconnected.
        assertTrue(ballot.vote(members.get(0), true, members, 0));
        assertTrue(ballot.vote(members.get(1), true, members, 1));
        assertEquals(3, ballot.required());
        assertEquals(DungeonForfeitBallot.State.OPEN, ballot.refresh(members, 1199));
        assertEquals(DungeonForfeitBallot.State.EXPIRED, ballot.refresh(members, 1200));
        assertFalse(ballot.vote(members.get(2), true, members, 1200));
    }

    @Test void membershipChangesCancelWithoutSilentlyReducingThreshold() {
        var members = roster(4);
        var ballot = new DungeonForfeitBallot(members, 1200);
        ballot.vote(members.get(0), true, members, 0);
        ballot.vote(members.get(1), true, members, 1);
        assertFalse(ballot.vote(members.get(2), true, roster(3), 2));
        assertEquals(DungeonForfeitBallot.State.CANCELLED, ballot.refresh(members, 3));
        assertEquals(2, ballot.yesCount());
        assertEquals(3, ballot.required());
    }

    @Test void staleButtonsAndSeparateInstancesCannotShareVotes() {
        var members = roster(3);
        var first = new DungeonForfeitBallot(members, 1200);
        var second = new DungeonForfeitBallot(members, 2400);
        assertTrue(first.matches(first.id().toString()));
        assertTrue(second.matches(null), "Typed /ff yes addresses the current ballot");
        assertFalse(second.matches(first.id().toString()));
        assertFalse(second.matches("not-a-ballot"));
        first.vote(members.getFirst(), true, members, 0);
        assertEquals(0, second.yesCount());
    }

    @Test void noVotesRejectOnlyWhenTwoThirdsIsImpossible() {
        var members = roster(3);
        var ballot = new DungeonForfeitBallot(members, 1200);
        assertTrue(ballot.vote(members.get(0), false, members, 0));
        assertEquals(DungeonForfeitBallot.State.OPEN, ballot.refresh(members, 1));
        assertTrue(ballot.vote(members.get(1), true, members, 2));
        assertTrue(ballot.vote(members.get(2), true, members, 3));
        assertEquals(DungeonForfeitBallot.State.PASSED, ballot.refresh(members, 3));
        var rejected = new DungeonForfeitBallot(members, 1200);
        rejected.vote(members.get(0), false, members, 0);
        rejected.vote(members.get(1), false, members, 1);
        assertEquals(DungeonForfeitBallot.State.REJECTED, rejected.refresh(members, 2));
        assertFalse(rejected.vote(members.get(2), true, members, 2));
    }

    @Test void rosterIsImmutableAndInvalidRostersAreRejected() {
        var input = new ArrayList<>(roster(2));
        var ballot = new DungeonForfeitBallot(input, 1200);
        input.clear();
        assertEquals(2, ballot.members().size());
        assertThrows(UnsupportedOperationException.class, () -> ballot.members().clear());
        assertThrows(IllegalArgumentException.class, () -> new DungeonForfeitBallot(List.of(), 1200));
        var owner = new UUID(0, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new DungeonForfeitBallot(List.of(owner, owner), 1200));
    }

    @Test void failedRunInventoryRecoveryRetainsItsNativeCodecAndInterruptionGuarantees() throws Exception {
        InventoryHandoffChecks.main(new String[0]);
    }
}
