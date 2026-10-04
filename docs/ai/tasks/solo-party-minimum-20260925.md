# Solo party minimum - 2026-09-25

User authorization: permit one-player testing and ship the change to TEST, with the standing
matching-client deployment workflow. Default party size remains 3; only TEST is configured to 1.

Behavior: widen minimumPartySize validation to 1-6 and expose explicit Start solo in Tamsin's
ready panel when the server allows one player. Preserve onboarding, class choice, personal
ready confirmation, proximity, revision, selector capacity, invitation ownership, queue,
countdown and dungeon entry revalidation.

Scope and ownership: Config.java and D1PartyService.java are shared configuration and pre-entry
coordination hotspots; this branch owns only the minimum-size and solo-action changes.
D1PartyLobby.java owns transient membership; D1PartyPanel.java only presents server state.
The example config, SoloPartyMinimumTest.java and this task's release fragment document/verify
the behavior. No unrelated cleanup or other agent work.

Save and network impact: no saved-data/registry changes. Existing string action/state fields
carry solo/SOLO_AVAILABLE without a packet-layout change. Both endpoints need the new code for
the new button. No datagen applies because there are no resource, registry or model changes.

Validation: native Java 21 build, JUnit config/lobby/payload checks, existing party regression
program and d1OfflineChecks. Runtime acceptance remains pending a stopped-server deployment
and licensed-client test; no dedicated/GameTest server launch is authorized.

Reload evidence: NeoForge 21.10.64 ConfigCommand registers showfile only; FML 10.0.32
ConfigWatcher reloads edits through ModConfigEvent.Reloading. Live TEST logs confirmed that
the old 3-6 range rejected and corrected the user's value 1. The new code needs one restart;
later valid config-file edits reload automatically.

Canon: use the existing mirrored Tamsin/party rules, retaining default 3 and the normal
readiness contract. The explicit user request authorizes the configurable solo exception.
Live Google metadata availability must be reported honestly; no canon mirror files are committed.

Deployment: back up and hash-verify the main JAR on TEST and the installed client only after
fresh confirmation that TEST is stopped and the client is closed. Back up the active config,
change only TamsinVane.minimumPartySize to 1 and verify it. Preserve all other server settings.

Validation completed: Java 21 build passed with 34 JUnit tests (7 new solo regressions),
including the existing D1PartyChecks invitation/readiness/queue program. d1OfflineChecks
passed, including 129 configuration checks and two .config round trips. No game server
was launched. Native end-to-end dungeon entry remains for Cameron's licensed TEST client.

Live Google metadata revalidation was attempted; the existing local OAuth authorization
has expired. No interactive authorization was started and no new canon claims were made.
The user-authorized TEST setting is independent of that access failure.
