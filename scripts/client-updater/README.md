# Cosmic Dungeon TEST client updater

Copy Update-CosmicDungeon.ps1 and Update-CosmicDungeon.bat with the matching CurseForge profile's mods folder. Make a shortcut to the BAT in its new location. No Cameron-specific path, administrator rights, Git installation, token, SFTP credentials or Windows startup task is needed.

Close Minecraft, double-click the BAT, then open the copied profile in CurseForge and press Play when the updater reports success. The BAT keeps its window visible. It does not authenticate or auto-launch a game, and it does not run a background watcher. A new build with the same 1.5.1 filename is detected by its SHA-256.

The public test-builds branch has two slots:

- latest-built: every successful scripts/build-local.ps1 -Task build publishes its JAR and build receipt fields in a normal commit/push.
- current-test: only an exact completed deployment, independently verified against installed client and TEST-server hashes, can publish this slot. Testers download this revision.

The updater pins the manifest and JAR to one artifact-branch commit. This prevents a concurrent publish from mixing revisions. The manifest sourceCommit identifies the source checkpoint; sourceFingerprint identifies the build inputs when available. Pre-commit candidate builds may include uncommitted source edits and are not advertised to testers. Ordinary git source branches do not acquire these binaries.

Only the main cosmicdungeon-x.y.z.jar is managed. The loading-screen module, other mods, saves and settings are preserved. If the baseline Minecraft/NeoForge versions or other dependencies change, distribute an updated profile separately. The copied profile must initially use Minecraft 1.21.10 and NeoForge 21.10.64.

Downloads have size, SHA-256 and mod-metadata checks. Old main-mod JARs are moved to a timestamped backup under the instance's CosmicDungeonUpdater/backups directory. Partial swaps roll back; an interrupted process leaves a pending.json journal and refuses another update until recovery. Never restore backups while Minecraft is running. Unknown Java processes conservatively block a replacement; no process is killed.

## Developer publishing

Use scripts/build-local.ps1 -Task build for automatic latest-built publication. Successful scripts/deploy-mod.ps1 -Apply also publishes current-test after the installation transaction completes. Publishing failure does not roll back a completed deployment; correct the error and retry:

    powershell.exe -NoProfile -File scripts/publish-test-build.ps1 -Mode Deployed -ReceiptPath <complete-manifest.json> -Apply

The publisher is dry-run by default. It uses the existing verified local Git authentication and pinned SFTP helper, in an isolated temporary artifact checkout. It never force-pushes or changes main. Cameron explicitly authorized binary commits to test-builds on September 25, 2026. Git history retains old binary revisions and will grow with testing builds.

Validation command: powershell.exe -NoProfile -File scripts/test-client-updater.ps1. All fixtures use temporary directories and mocked downloads. For a live read-only check from a copied mods folder: powershell.exe -NoProfile -File Update-CosmicDungeon.ps1 -CheckOnly. Exit 0 means current; exit 10 means an update is available; exit 1 means an error.
