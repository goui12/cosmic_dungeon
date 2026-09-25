# Exact class-selector destination - 2026-09-25

Authority: Cameron explicitly requests initial dungeon placement at the authored rift destination, even if the player suffocates. Remove upward compensation; dungeon slot coordinates are authored deliberately.

Branch: fix/exact-selector-destination-20260925, based on fix/fresh-dungeon-instances-20260925 at 3940e883. This is a dependent task/PR; no merge or production deployment.

## Scope and owned hotspot

Single writer for class-selector entry teleportation in ClassSelectorEntryService. Reuse its existing private TeleportTarget record and server-authoritative orchestration. The slot destination takes precedence over the selector fallback destination, as before. Resolve its dimension into the current run, paste startup resources, then teleport to X + 0.5, exact Y, Z + 0.5. Do not search upward, use a surface heightmap, or reject an obstructed destination. Assign the same unadjusted position as the run respawn target.

This task changes initial selector entry only. Normal subsequent physics/death and other travel or overworld respawn handling keep their existing behavior; this is not a continuous position lock.

## Files

- src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java
- docs/ai/tasks/exact-selector-destination-20260925.md
- docs/releases/fragments/exact-selector-destination-20260925.md

## Compatibility and boundaries

No saved fields, registry IDs, NBT, packet codecs, configuration or generated resources change; no migration is required. Existing rift block positions remain readable. The shared mod JAR changes server-side entry behavior for both integrated and dedicated servers, with no client-only imports or new dependencies.

Retain access/class/roster validation, slot precedence, physical instance mapping, startup paste, inventory handoff and abort recovery. Do not modify authored class chest stacks. Doors/keys, vendors/currency, progression/factions/achievements, Cosmic Spawner and entity persistence are outside the changed code; their gameplay is not claimed tested. The existing overworld recovery standability helper is outside this request.

## Validation plan

- Build using Java 21 and the repository wrapper; run the existing meaningful JUnit regression suite.
- Parse src JSON and run git diff --check. Datagen is inapplicable to this Java-only behavior change.
- No implementation-mirroring source-text test or artificial runtime abstraction is added for removing a private safety scan.
- Native source review: Entity.teleportTo constructs a TeleportTransition from the supplied Vec3; ServerPlayer's final boolean controls the camera, not safe placement. setPlayerRespawnTo stores the passed BlockPos without adjusting it.
- Native GameTest/dedicated launches remain separately gated by AGENTS.md. Do not run destructive clean or overwrite the unrelated staged historical JAR deletion/caches.
- Licensed TEST QA: enter an authored slot and confirm feet Y; deliberately use an obstructed test destination and confirm no relocation to the roof (suffocation accepted); check slot-specific mapping with a second player when available; forfeit and verify inventory restoration/instance cleanup.

## Source and deployment evidence

Current AGENTS.md and external task notes were checked. Existing Google authorization was retried using the configured mirror venv on 2026-09-25T19:04:55Z; it returned RuntimeError and relevant metadata could not be revalidated. No interactive consent or mirror sync was attempted. Cameron's explicit current placement instruction is the authority for this change.

SFTP inventory contains 2,047 log/crash files. The latest initial refresh at 18:57:49Z found 50 new debug lines and no new warning/error/fatal entries; latest.log was unchanged. These logs do not record requested-versus-actual spawn coordinates.

The previous fresh-instance build 3940e883 was deployed successfully at 17:50:34Z to TEST and the licensed client with SHA256 4A8E22CC39DDFC86CFAECE071FA7A1D320FCF928AF88EFD0BDD5150AF675872E. This task has not replaced either installed JAR. Fresh server-stopped/client-closed confirmation is required before applying its update; server.properties remains untouched.

## Completion

Implementation/review batches remaining: **0**. Licensed gameplay QA is separate.

Java 21 build passed (11 seconds); all 54 existing JUnit tests passed with zero failures/errors/skips, including dungeon-forfeit, instance generation/snapshot and native serialization coverage. All 1,997 src JSON files parsed; git diff --check passed. No datagen, destructive clean or dedicated/GameTest server launch was performed. This is automated regression evidence and native source review, not an in-game acceptance claim.

The final pre-checkpoint SFTP refresh at 19:06:31Z inventoried 2,047 files. The changed latest/debug logs contained 18/19 new lines, respectively, with no new warnings, errors or exceptions. No client Java process was running at that check; only the Gradle daemon remained. A development runClient launch is authorized for handoff, with bounded startup evidence stored outside Git. Do not enter worlds automatically.

Keep the unrelated staged deletion of build/libs/cosmicdungeon-1.5.0.jar and three preexisting generated cache edits out of this checkpoint, along with all untracked build/log/cache files. Normal task-branch push and a dependent draft PR follow validation; installed TEST/client JAR replacement remains pending fresh confirmation.

Possible future improvement: a developer destination preview showing the exact feet position.
