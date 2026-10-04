# Stage 1: CurseForge 1.5.2 Beta publishing

Source baseline: 8e56c88e355feed515c1b55f7d035df1d0b7691b, PR 214 development stack.
Branch: release/curseforge-1.5.2-beta-20261004. Target: main with the completed source set.

Owns publishing scripts, Gradle/version packaging, CI, release notes, AGENTS workflow and
the rollout tracker. No game entry point, registries, packets, gameplay schemas, live world
or authored chest content is changed. No save migration or datagen is required.

Authorization: Cameron requests public 1.5.2 Beta, automatic alpha/beta/release uploads,
incremental mod_version and merges after completed batch sets. Credentials saved locally
and in GitHub were confirmed 2026-10-04. Do not begin later gameplay batches.

Validation: publishing regression tests; Java 21 build/native unit tests; source JSON;
actual embedded jar versions and helper service metadata; diff checks; clean CI build and
GameTests. Never auto-launch a development client. Existing local unrelated config edits,
generated caches and staged historical jar deletion must remain excluded from this commit.

Distribution boundary: CurseForge Additional Files are archival only. The loading helper
requires a separate companion project and one-time theme/profile setup for app-managed
updates. Preserve the legacy feed until that migration is verified. Public moderation and
clean licensed-client visual checks remain distinct from API acceptance and build success.

See ../CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md and ../../CURSEFORGE_RELEASES.md.
