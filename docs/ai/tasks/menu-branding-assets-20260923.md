# Menu branding asset preparation - 2026-09-23

Status: asset import and automated packaging validation complete; runtime integration pending.
Branch: feature/menu-branding-assets-20260923.
Scope: the 12 completed desktop assets, exact 42-pixel side crops, 1024-square exports,
provenance, and validation. No runtime branding implementation or deployment.
Ownership: new assets/cosmicdungeon title/background/loading/text/music assets plus this
task's client documentation and unique release fragment. No central integration hotspots.

No saved-data, registry, network, security, or migration changes.
No Google canon decision is involved; Cameron's supplied art/text/audio governs this task.
Preserve pre-existing staged build-JAR removal, generated caches, logs, and build output.
No publishing request was made; keep this checkpoint local. A future commit/push request
must follow AGENTS.md and verify current remote ancestry before publishing.

Validation: exact crop pixel comparisons, source/copy/ZIP hashes, PNG dimensions, complete
Ogg page CRCs, source JSON validation, Java 21 build, packaged-resource hash checks, and
git diff --check. Local game/GameTest execution is not authorized by the current request.
Datagen is not applicable. Rendered panorama seams and audible playback remain pending.
Documentation: ../../client/Menu_Branding_Assets.md.

## Validation results

- Java 21 offline build passed. The first shell wrapper treated an SLF4J stderr warning
  as a terminating error; rerunning with normal native stderr handling passed.
- clean was not run because the local workflow protects tracked build artifacts.
  Existing build JARs were backed up before validation; the pre-existing staged removal
  of the historical 1.5.0 JAR was preserved exactly.
- 1994 source JSON files parsed successfully.
- All 12 imported resources matched their built JAR entries byte-for-byte.
- All imported PNG CRC/decompression checks passed; all 5,312,886 cropped pixels matched.
- Soundtrack: 221 seconds, stereo Vorbis, 44.1 kHz; all 2,427 Ogg page CRCs passed.
- Offline cubemap preview shows mismatched joins, especially floor/ceiling. No claim of
  seamlessness; capture position/projection/pole orientation remain to be diagnosed.
- No game, GameTest, deployment, datagen, common-code changes, or migration.
- Unrelated tracked/cache edits and pre-existing staged changes were verified unchanged.

Next improvement: wire the approved assets through the existing client architecture after
resolving panorama acceptance, while keeping the NeoForge loading credit visible.
