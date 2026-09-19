# Local AI development and TEST-server I/O

## Entry points

- Repository: `C:\Users\Cameron\Documents\mod_development\cosmic_dungeon`.
- Human-auditable cache: sibling folder `CosmicDungeon_AI`, starting at `CURRENT_STATE.md` and `DECISIONS.md`.
- Machine-specific, non-secret settings: `CosmicDungeon_AI/config.json`. Do not commit this file.
- Encrypted credential: `%LOCALAPPDATA%\CosmicDungeon\secrets\test-sftp.credential.xml`; never copy it into the cache, repository, logs or chat.
- Read the root [AGENTS.md](../AGENTS.md) before changing anything. Contradictory design choices and increased client resource requirements need Cameron's approval.

## Frequent Gradle commands (Java 21)

| Command | Purpose |
| --- | --- |
| `.\gradlew.bat build` | Compile/package and run configured build checks; not multiplayer QA. |
| `.\gradlew.bat runServerData` | Generate data into `src/generated/resources_server`. |
| `.\gradlew.bat runClientData` | Generate assets into `src/generated/resources_client`. |
| `.\gradlew.bat runClient` | Dev-client run; not the default testing route. |
| `.\gradlew.bat clean` | Delete build outputs; currently also deletes a tracked 1.5.0 jar. Preserve it and resolve policy first. |

Optional logged wrapper: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-local.ps1 -Task build`.
A successful build issues a local receipt with the Git commit, build-input fingerprint, jar SHA256 and log path. Deployment rejects missing/stale receipts.
The same wrapper accepts the other four tasks. Dev-client execution is gated; clean refuses to destroy a tracked build artifact. No Gradle task or heap setting was changed by onboarding.

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

## Coordinated deployment and recovery

1. Cameron confirmed `Cosmic Dungeon ADMINISTRATIVE ACCESS ONLY` on 2026-09-15; ClientPathConfirmed is now true. This does not authorize an unrequested client launch or deployment.
2. Save the credential locally; verify the test identity with Probe. The helper checks the pinned host/account/root, the observed internal port (`ExpectedServerPort=25565`), and `online-mode=true`. Public `GamePort=12250` and SFTP port 22 are separate. Do not modify the working server.properties; changed values require a decision, not an automatic repair. These checks do not independently verify Akliz port forwarding.
3. Build with build-local.ps1 and inspect the result. Datagen must have been run first when relevant.
4. Review `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\deploy-mod.ps1` (dry run). An explicit -Jar selects a specific artifact; otherwise the exact mod_version artifact is used, never the newest arbitrary jar.
5. Cameron stops the TEST server through the Akliz web panel and closes the client. Only after his actual stopped/closed confirmation run `...\deploy-mod.ps1 -Apply -ServerStopped -ClientClosed`.
6. The script stages and SHA256-verifies both copies before moving old CosmicDungeon jars into timestamped backups. It checks both installed hashes and preserves unrelated mods/configs/worlds.
7. It does NOT restart the server. Cameron starts it through the Akliz web panel after successful deployment verification. Panel/console automation is not configured. Client/server mod directories are intentionally not blindly mirrored.
8. On caught activation failure it attempts rollback. This is not a distributed atomic transaction: power loss or loss of SFTP during swapping can require manual recovery. `deploy-pending.json` blocks further deployments until reviewed.
9. Keep server stopped during recovery. Inspect the local `backups/<id>/manifest.json` and remote `.cosmic-ai-backups/<id>`. Restore only the listed CosmicDungeon jar(s), preserve failed-new artifacts, compare both targets, and clear a pending journal only after verified recovery.

## Cache freshness and limits

Keep `CURRENT_STATE.md` compact; update it after milestones. `DECISIONS.md` separates pending, approved and rejected choices. Source snapshots must include file ID/URL, source revision/modified time, fetched UTC time, content hash and explicit coverage; initialize sources/index.json without claiming unavailable content is cached.
Use live Git status/commit and relevant file hashes to invalidate stale code notes. Before code alignment, refresh metadata for relevant Google Docs and fetch changed, missing or partial sources. A historic title-match/access result is not a full semantic read.
The cache soft budget is 512 MiB; log snapshots are capped at 16 MiB each. Review size/retention instead of silently deleting rollback backups. No automatic ongoing monitoring is enabled.
