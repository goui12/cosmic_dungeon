# D1 in-game NPC placement and unique identities

Cameron reversed the coordinate-config proposal on 2026-09-20. Regions, boundaries,
quest points, selector anchors, NPC locations and Watson placement remain authored
in-game and stored with the world. CosmicDungeon.config remains the server's
modifier/settings file; this pass adds no coordinate configuration.

Parent: 604f6656f56c541c124f214fb196842feba35413.
Branch: feature/d1-canon-config-20260916.
Final local commit follows validation, completion notes and staged diff review.
No push, deployment, active world/config edit or Minecraft launch occurred.

## Implemented behavior

- One current global NPC per exact vendor profile ID. Explicit successful spawn or
  assign selects the replacement. Names never serve as identity.
- A loaded previous copy despawns by UUID lookup, even in another dimension.
  Unloaded copies fail admission when their chunks later load. No forced chunk
  loads, entity scans, background watcher or tick loop is introduced for vendors.
- New insertion reserves identity before the NeoForge join callback. A rejected
  or throwing insertion restores the previous identity and removes the candidate.
- Beluzon's default shell is minecraft:creaking; explicit incompatible types
  remain denied. Replacing him does not place or change First Heart blocks.
- Binding a new Tamsin selects her current global identity and retires the old one.
  Existing agreement records and selector bindings remain; historical NPC binding
  records identify stale chunks and are not counts of active Tamsins.
- First loaded legacy roles are adopted automatically. The operator can select
  the intended copy with explicit spawn/assign/bind. Mixed or malformed roles
  still require developer review; this pass does not guess their intended role.
- Clearing the current role leaves a retirement marker. An older saved NPC
  cannot fill the vacancy. A later explicit placement can replace that marker.
- Personal vendor unlocks, stock, Trace, Inn bonds and home beds retain their
  existing storage and authority. Old UUID-based sessions become invalid when
  their NPC is removed; no progress is granted by globally placing an NPC.

## Watson authoring

Stand at the intended point in a D1 template or active D1 instance:

    /d1 watson set ~ ~ ~
    /d1 watson status

The command normalizes the current physical instance to its D1 template.
Every active and future D1 run maps that same template coordinate into its own
leased dimension. Each run owns one Watson UUID independently.

Setting another point retires old Watson UUIDs, including unloaded copies.
Old-position, wrong-dimension, wrong-instance, ended-run and sealed-outcome joins
are rejected. Existing party objective and gathering conditions still determine
when a replacement appears. No run progress or lifetime statistics are reset.

The previous implementation could retain a loaded Watson in the old dimension
after changing the binding. Lookup and retirement now follow the saved UUID
across loaded dimensions. Failed insertion restores that run's previous marker.

## Other placement commands

    /vendor spawn save_teleport_npc
    /vendor assign save_teleport_npc
    /vendor info
    /d1 tamsin bind <npc> <selectorX> <selectorY> <selectorZ>
    /d1 tamsin status

Spawn uses the executing developer's position. Assign uses the crosshair-selected
compatible mob. Tamsin bind requires a living nonplayer in the starting area
beside an actual D1 selector. Existing AccessPolicy checks remain server-side.
A vanilla name tag or an ordinary summoned mob is not an NPC role assignment.

See [command reference](../commands/In_Game_Commands.md#d1-npc-placement-developer-authoring)
and [vendor authoring](../Vendor.md#vendor-entity-shells).

## Authority and source freshness

Scope is the local 20-tab Debloat workbook and local Questions and answers.xlsx,
with retained linked documents. The removed original MASTER tabs are excluded.
Both workbook hashes still match the preceding verified checkpoint.

Four relevant retained Google Doc revisions and all cached artifact hashes were
revalidated on 2026-09-20; none changed:

- Vendor Info 2.0: 1aDUTh-_AmrB3kMHKeyTDQKdIeJHBtqdyp11FPBg3vmY,
  modified 2026-08-23T20:33:27.547Z, version145: global persistent NPCs; personal access.
- Beluzon: 1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA,
  modified 2026-08-29T14:49:42.290Z, version44: native Creaking and independent Inn bonds.
- Watson: 1e1po-TpjWQpTJ6ueIfnpTDNRZ1za3J7434Y4nFXvSmo,
  modified 2026-07-10T20:39:40.976Z, version21: six-Bloom recovery; D2 remains deferred.
- Tamsin: 1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4,
  modified 2026-08-19T22:35:48.808Z, version46: starting-area entry broker.

Cameron's current instruction supplies the explicit replacement rule and keeps
in-game authoring authoritative; no new lore tables or source coordinates were invented.

## Persistence, backup and rollback

New independent overworld SavedData: cosmicdungeon_npc_identities_v1.dat.
It stores role-to-UUID ownership and explicit retirement markers, never positions.
Existing vendor profile NBT, original-state snapshots, Tamsin bindings/agreements,
Watson template_dimension/position, objective storage IDs and registry IDs remain.

Existing worlds automatically register legacy NPC identities when those entities
load. Missing new storage starts empty; malformed identity data fails validation
and preserves the original file for review. Unloaded duplicates are covered by
the current owner comparison on their later join. No offline region-file rewrite.

Watson uses the existing watson_entity string slot with a new retired marker.
Its owner remains run-scoped; ordinary run retirement clears its instance state.
No spawner, preset, door/key, region, inventory or currency schema changes;
no migration or recreation of those authored objects is required.

Before eventual TEST deployment, stop the server and back up the entire world and
current jars together. Roll back the full matched backup and prior jar if needed.
Restoring only an old jar cannot restore despawned NPCs; it also lacks the new
identity enforcement. This uses Minecraft SavedData persistence and is not an
atomic power-loss transaction across entity chunks and separate data files.

## Validation

Java21 offline d1OfflineChecks plus build passed. 16,875 checks in46 groups;
931 new checks (838 NPC identity,93 Watson placement), plus two config round trips.
All1,964 source JSON files parse, and git diff --check passes.

New coverage includes adoption, same-role replacement, independent roles,
failed/throwing insertions, empty/retired state, repeated replacements with
out-of-order old UUIDs, malformed persisted UUIDs, native compressed NBT readback,
both D1 template dimensions in all10 slots, wrong positions/instances,
reauthoring, retirement and existing run-state preservation.

The first offline run compiled successfully but its Watson fixture initialized
native Level.OVERWORLD without an FML loader. The fixture now constructs the same
dimension ResourceKey directly; the complete suite then passed. No valid assertion
was removed to obtain the passing result.

Native join-hook source was inspected to confirm cancellation precedes entity
registration. This is code review, not an executed Minecraft event integration test.
No datagen applies: resources, IDs, models, recipes and packets are unchanged.
No new required PNG. Existing optional tamsin_d1_map.png remains512x256.

Candidate1.5.1 SHA256: 260c430cfb7eadbdedec3e770e793105ae1e9afee63f71a4ce6ee11bf257e609

## Licensed TEST acceptance still required

1. Spawn Beluzon, move elsewhere, spawn again; only the new Creaking remains.
2. Repeat with the original chunk unloaded and from another dimension. Revisit it.
3. Save/restart after replacement, then revisit the old chunk. No old copy returns.
4. Attempt an invalid or refused new spawn; the current NPC and services remain.
5. Assign a replacement vendor; verify the old menu/offer cannot transact and
   personal unlocks, morning stock, balances, Inn bonds and beds remain unchanged.
6. Bind another Tamsin beside a selector; old conversations/ready anchors invalidate,
   agreements persist, and the current binding survives restart. Check unbind too.
7. Set Watson once, use two qualifying D1 parties simultaneously, and verify both
   receive their own Watson at corresponding coordinates. Move the placement,
   reload old chunks and restart; verify old copies cannot return or cross runs.
8. Confirm current First Heart protection/night behavior, source-authored regions,
   chests, keys, doors and encounters separately. No world binding was edited here.

## Remaining work

Numbered batches scheduled:0. Five readiness areas remain: R01 service-only repair
compatibility (automatic chest marking removed); R04 approved-recipes policy; R05 balance panels and treasure-claim
decision; R07 legacy model textures; R08 native world/binding acceptance.
This pass resolves R08's NPC placement/identity implementation subtask only.
The101 audit dispositions and broader cumulative TEST requirements remain open
at their previous reviewed statuses; build success does not close acceptance.

A useful later improvement is a read-only in-game placement report that lists
current NPC identities and highlights missing world bindings.

## Exact changed files

- docs/Vendor.md
- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_IN_GAME_NPC_PLACEMENT_2026-09-20.md
- docs/ai/D1_REMAINING.md
- docs/commands/In_Game_Commands.md
- docs/releases/fragments/d1-in-game-npc-identities.md
- src/main/java/net/goui/cosmicdungeon/command/D1Command.java
- src/main/java/net/goui/cosmicdungeon/command/VendorCommand.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonService.java
- src/main/java/net/goui/cosmicdungeon/npc/NpcIdentityData.java
- src/main/java/net/goui/cosmicdungeon/npc/NpcIdentityService.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinService.java
- src/main/java/net/goui/cosmicdungeon/vendor/VendorAssignmentService.java
- src/main/java/net/goui/cosmicdungeon/vendor/VendorBindingRules.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/WatsonPlacementChecks.java
- src/test/java/net/goui/cosmicdungeon/npc/NpcIdentityChecks.java
