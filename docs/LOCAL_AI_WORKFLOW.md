# Local AI development and TEST-server I/O

## Entry points

- Active repository: read `CosmicDungeon_AI/BatchRunner/config.json`.
- Human-auditable cache: sibling folder `CosmicDungeon_AI`, starting at `CURRENT_STATE.md` and `DECISIONS.md`.
- Machine-specific, non-secret settings: `CosmicDungeon_AI/config.json`. Do not commit this file.
- Encrypted credential: `%LOCALAPPDATA%\CosmicDungeon\secrets\test-sftp.credential.xml`; never copy it into the cache, repository, logs or chat.
- Read the root [AGENTS.md](../AGENTS.md) before changing anything. Contradictory design choices and increased client resource requirements need Cameron's approval.

## Current player guidance

For changes to character or gameplay behavior, update [PLAYER_GUIDE.md](PLAYER_GUIDE.md), [Help_Menu.md](Help_Menu.md), affected [class guides](Classes/Help_Menu_Classes.md), and the in-game HelpMenuContent/ClassResourceHelp sources together. Check actual shipped services and the approved local canon; preserve NPC lore, reachable page IDs, and deferred-class boundaries. The H guide is read-only: inline links navigate only, item examples never transfer items, and hotkey hints read native Controls. Keep instructions here focused on maintenance; the linked guide owns the manual.

## Frequent Gradle commands (Java 21)

| Command | Purpose |
| --- | --- |
| `.\gradlew.bat build` | Compile/package and run configured build checks; not multiplayer QA. |
| `.\gradlew.bat runServerData` | Generate data into `src/generated/resources_server`. |
| `.\gradlew.bat runClientData` | Generate assets into `src/generated/resources_client`. |
| `.\gradlew.bat runClient` | Launch only when Cameron explicitly requests it. |
| `.\gradlew.bat clean` | Delete build outputs; currently also deletes a tracked 1.5.0 jar. Preserve it and resolve policy first. |

Optional logged wrapper: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-local.ps1 -Task build`.
A successful build issues a local receipt with the Git commit, build-input fingerprint, jar SHA256 and log path. Deployment uses the exact CI publication receipt. Legacy feed publication requires explicit `-PublishLegacyFeed`.
The same wrapper accepts the other four tasks. Cameron revoked automatic development-client launches on 2026-10-04; a new explicit launch request is required. clean still refuses to destroy a tracked build artifact. No Gradle heap setting is changed.

## Credential setup and trust

Run `powershell -NoProfile -ExecutionPolicy Bypass -STA -File .\scripts\set-sftp-credential.ps1` on Cameron's desktop. Use `-Reset` only to replace a saved credential.
Type the password in the masked local form, or explicitly select the existing local `COSMIC_SFTP_PASS` value without displaying it. The legacy environment variable is not deleted automatically.
The exported PSCredential is encrypted by Windows DPAPI and restricted by NTFS permissions. It is reusable by the same Windows user on the same computer, not a portable or everlasting backup, and not protection against compromised same-user/admin code.
The scripts use WinSCP's installed .NET assembly with SecurePassword and a pinned SHA256 host fingerprint. No password-bearing command line, plaintext temporary script, wildcard host-key acceptance, or automatic host-key reset is used.

## I/O commands (from the repository)

`powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\cosmic-io.ps1 -Action Status` records live Git state and setup flags, without reading secret contents.
`... -Action Probe` validates SFTP reads and server identity; `... -Action Probe -Apply` additionally uploads, reads back, hashes and removes a uniquely named harmless probe file.
`... -Action List -RemotePath /minecraft-neoforge/mods` lists the target directory (maximum 100 displayed entries, with a total count).
`... -Action Download -RemotePath /minecraft-neoforge/logs/latest.log` saves a new local snapshot; existing destinations are not overwritten and individual downloads are capped at 64 MiB.
`... -Action Upload -LocalPath C:\path\file -RemotePath /minecraft-neoforge/.cosmic-ai-staging/file -Apply` uploads a new staging file only. Without -Apply it is a dry run; existing remote files are never overwritten by this action.
`... -Action Logs` refreshes bounded client/server latest.log snapshots, skips unchanged remote metadata, and reports warning/error counts in the last 500 lines. These are sample counts, not whole-log totals.
`... -Action Launch -Apply` opens the verified CurseForge shortcut only after client-path confirmation. Cameron performs legitimate Microsoft login/Play as Goui12. It does not automate authentication or guarantee an instance has started.
`powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\capture-client.ps1 -Seconds 30 -IntervalSeconds 5` samples target-client CPU/RAM to CSV and stops automatically. Duration is capped at 300 seconds. It neither launches Minecraft nor attaches a profiler. CPU percentages are normalized to the whole machine, not one core.

## CurseForge and TEST deployment (2026-10-11)

1. Validate/commit/push source and unique tag. CurseForge Actions builds/tests and publishes runtime plus companion.
2. For Alpha, retain the publication receipt and finish: server delivery is not required. Do not download the runtime for deployment, inspect server state, use sFTP or create pending server delivery. The following steps apply only to Beta and official/stable builds.
3. Download exact tagged runtime and curseforge-receipt.json from the GitHub release; no additional artifact/readback hashes.
4. Use `scripts/deploy-mod.ps1 -Jar <runtime> -ReleaseReceipt <receipt>` for dry run. It validates source/version/receipt, pinned TEST identity and current shutdown logs.
5. Add `-Apply` to update stopped TEST with fresh shutdown check, staging byte count and acknowledged transfer/rename journals. No client target is read/changed.
6. Preserve server.properties, unrelated mods/config/worlds and legitimate authentication. No restart/client launch. Save compact receipt and finish; clients use CurseForge.
7. Unknown state or deploy-pending.json blocks. Reconcile old/staged/backup/target paths before retrying uncertain operations. Retain recovery files as needed, not permanent rollback copies.

Latest-session "Stopping the server" without later startup/activity suffices; normal
shutdown saves are accepted. Ask for shutdown only when observed running.

## Cache freshness and limits

Keep `CURRENT_STATE.md` compact; update it after milestones. `DECISIONS.md` separates pending, approved and rejected choices. Source snapshots must include file ID/URL, source revision/modified time, fetched UTC time, content hash and explicit coverage; initialize sources/index.json without claiming unavailable content is cached.
Use live Git status/commit and relevant file hashes to invalidate stale code notes. Cameron's 2026-10-10 instruction makes the local repo backup the canon source for this workflow:
C:/Users/Cameron/Documents/mod_development/cosmic_dungeon/Google Docs and Sheet/.
Read relevant Docs/ exports/native JSON and Sheets/Dungeon Crawl Master Sheet.xlsx, using Audit/DOCUMENT_INDEX.md, the saved manifest and Audit/STATUS.md for provenance and coverage. The backup is not duplicated in every worktree. Preserve recorded revisions/fetch times/hashes and distinguish cached source review from current online freshness; a historic title/access result is not a semantic read.
No Google Drive/OpenAI connector, live metadata refresh, Sync-Google or OAuth/login is required. Earlier refresh/setup instructions remain historical/optional; do not follow them automatically. Report specific missing or ambiguous local content and ask only when it affects an unresolved decision. Latest explicit user requirements take precedence; online refresh requires a separate request.
The cache soft budget is 512 MiB; log snapshots are capped at 16 MiB each. Prune obsolete deployment JAR copies as needed without additional confirmation; Cameron does not require historical rollbacks. Preserve authoritative source snapshots, compact receipts and any files needed by an unresolved deployment journal. No automatic ongoing monitoring is enabled.


## Handoff and support

Finish task/source notes before final build/commit where possible.
After Alpha CurseForge publication, or Beta/official CurseForge plus acknowledged sFTP, only compact external completion is needed. Record skipped Alpha delivery truthfully as not required; do not claim server installation.
Report automated results and short pending licensed GUI/multiplayer QA separately.
Legacy test-builds/updater is optional, not a completion gate.
See [CurseForge releases](CURSEFORGE_RELEASES.md) for publication/retry/moderation.
No automatic local dedicated/GameTest/development client/CurseForge/TEST launch.
If Remote Desktop fails, report incomplete step and provide:
`npx.cmd -y @wonderwhy-er/desktop-commander@latest remote`.
