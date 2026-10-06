# Batch 10: Tamsin replay and exact 1.6.0-beta

Status: implemented; local validation passed, clean CI/review/merge/release pending.

## Behavior and architecture

Client-local preview reuses the existing agreement/map renderer. The real server menu
remains untouched, so readiness, class selection, accepted onboarding, tax and group
membership cannot be changed by replay. It works in group/class selector presentation,
including developer selector sessions. No cancels only the preview.
Refresh/resize and party snapshots preserve the preview; real tax/onboarding changes
supersede it. The i control matches Refresh at20x18.

## Validation and boundaries

Eight replay-controller tests cover first visit, full replay, No, developer selector,
refresh/resize, repeated/obsolete input, authoritative tax transition and latest return.
Publisher tests cover bare-beta exact artifact versions, stable promotion and next alpha.
CI clean build/native GameTests, full JSON, diff checks and review required before merge/tag.
No network protocol, save, registry, spawner, preset, chest or migration changes.
No datagen applies. Java21 required; no local clean of the historical tracked1.5.0 jar.
Google read-only source refresh failed due to expired authorization; audit
BATCH10_TAMSIN_SOURCE_REFRESH_20261006 preserves source IDs/cached hashes. Explicit user
instructions govern; no fresh Google semantic alignment claim.

## Manual QA

1. Open Tamsin as a returning leader/member; click i, Yes, Continue. Same group/class/readiness.
2. Click No during replay; reopen replay and Refresh/resize during map. Correct return/view.
3. First-time agreement remains mandatory; ordinary Yes/No/tax and class choice unchanged.
4. Use developer selector preview without gaining live onboarding/entry permissions.
5. Verify Positive Potions9/10 resurrection, exact position/protection/cooldown in co-op.
6. Verify both CurseForge client components in a licensed profile.

## Remaining work

After10, authorized11 follows; after both,15 batches12-26 remain.
Future improvement: localizing the shared conversation strings independently of this release.

## Exact files

- src/main/java/net/goui/cosmicdungeon/client/screen/TamsinReplay.java
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassSelectorScreen.java
- src/test/java/net/goui/cosmicdungeon/client/screen/TamsinReplayTest.java
- scripts/curseforge_release.py
- scripts/tests/test_curseforge_release.py
- gradle.properties
- AGENTS.md
- docs/CURSEFORGE_RELEASES.md
- .github/workflows/curseforge-release.yml
- docs/releases/1.6.0-beta.md
- docs/releases/fragments/batch10-tamsin-replay-beta-20261006.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/tamsin-replay-beta-20261006.md

## Local validation

Java21 build successful:344 native JUnit tests (zero failures/errors/skips), plus loading helper tests.
17 publisher safeguards passed;2,001 source JSON files parsed;git diff --check passed.
No local game/GameTest-server launch, datagen or migration. CI native GameTests and release gates remain pending.
