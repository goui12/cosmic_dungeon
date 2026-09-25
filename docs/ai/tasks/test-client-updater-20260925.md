# Portable TEST client updater - 2026-09-25

Cameron authorized replacing the existing PS1/BAT in his confirmed CurseForge mods folder so his dad can copy it and update the TEST client. His follow-up explicitly requires committing/pushing testing builds to a dedicated test-builds branch. That instruction permits runtime binaries on this artifact-only branch; retain the existing exclusion on ordinary source branches.

Source task branch: feature/test-client-updater-20260925, based on a9995057. Single writer owns build/deployment publication integration, the portable updater, workflow contract and task/release notes. No gameplay, persistence, network protocol, generated resource, server configuration or installed JAR changes are intended. No migration/datagen. The region-look correction stays queued for the next mod-code batch while Cameron tests.

## Design

PowerShell 5.1 classes isolate feed transport, client-process checks, update transaction, publishing, and regression fixtures. BAT is only a visible launcher. Relative script paths work after copying the mods folder. Authenticate neither GitHub nor Minecraft on the tester's computer. Preserve unrelated mods and the loading-screen module.

Public artifact branch slots: latest-built receives each completed wrapper build; current-test receives only completed deployments with matching live TEST/client hashes. The updater reads current-test at a single pinned commit. Same-version updates use SHA-256; invalid downloads preserve the old installation. Backups and a durable pending journal protect swaps. Server stop/client close gates for deployment are retained. Publication happens after the deployment transaction so GitHub failure cannot roll back a successful installation.

Only the already deployed a9995057 build is to be advertised initially. Installing scripts does not require stopping gameplay. Do not launch another game or replace live JARs during this tooling task. Existing staged historical JAR deletion and generated-cache edits remain outside the checkpoint.

## Validation

Planned: native Windows PowerShell parser and offline transaction tests covering same-version replacement, current-version no-op, duplicate old JARs, unrelated mod preservation, malformed/path-traversal/wrong-baseline manifests, bad hashes/ZIPs/download failures, running client, partial-swap rollback and interrupted journal. Verify public download in an isolated mods folder and check actual installed folder read-only. Java21 build, JSON/diff checks, normal source branch push and verified artifact branch push. Dedicated/GameTest and licensed gameplay are not exercised by an updater test.

## Completion evidence

Implementation/review batches remaining for this updater: **0**. The separately queued region-look mod-code batch has not started.

All 30 offline updater/publisher assertions passed on native Windows PowerShell 5.1, including rollback after the first old JAR moves and the second is locked. Five relevant PS1 files passed parsing. A real anonymous GitHub download into an isolated mods fixture replaced a same-version JAR, matched the deployed E723E98427887DF8FFDCC742670F8574C233001F41870D6D6DD286C4637EF5BF hash, and then produced a no-download current-version result. Both copied scripts are installed in Cameron's confirmed CurseForge mods folder; a read-only invocation there reports current. The previous updater pair is backed up outside the repo.

Java 21 build passed; the unchanged Java regression suite remained up-to-date with 54 previously passing JUnit cases. All 1,997 source JSON files parsed; diff checks passed. No datagen, destructive clean, new game launch, dedicated/GameTest server launch, world/configuration change or live JAR replacement was performed. Source/installed-script hashes, publication receipts and task evidence are in CosmicDungeon_AI/backups/test-client-updater-20260925 and CosmicDungeon_AI/publishing.

current-test publication verified the existing installed client and TEST-server JAR against the completed a9995057 deployment before pushing artifact commit 8a01ec12. The first build-hook publication pushed latest-built as b48f84e4. Subsequent wrapper builds append fresh artifact commits; consult the final publication receipts for the current HEAD. Normal source task commit/push follows final documentation validation. No main merge.

Remaining manual QA: Dad copies the matching profile/mod files to his PC, shortcuts the BAT, closes Minecraft, updates, and opens that profile in CurseForge. His exact machine/path has not been exercised. Existing server-authoritative gameplay/access/class/rift/inventory/persistence systems are unchanged; the updater does not grant permissions or bypass authentication. Missing dependency/profile upgrades remain a separate explicit distribution task.

## Exact source files

- AGENTS.md
- scripts/build-local.ps1
- scripts/deploy-mod.safe.ps1
- scripts/publish-test-build.ps1
- scripts/test-client-updater.ps1
- scripts/client-updater/Update-CosmicDungeon.ps1
- scripts/client-updater/Update-CosmicDungeon.bat
- scripts/client-updater/README.md
- docs/ai/tasks/test-client-updater-20260925.md
- docs/releases/fragments/test-client-updater-20260925.md

Google canon documents are not requirements for this developer distribution tool; the current explicit user instructions control its behavior. Existing Google authorization is unavailable.

Potential future improvement: include a separately approved loading-screen update when its distribution needs to change.
