# CurseForge release workflow

Main project: https://www.curseforge.com/minecraft/mc-mods/cosmic-dungeon (1326805).
Manage projects: https://authors.curseforge.com/.

## Versions and release triggers

`gradle.properties` is authoritative. Use `major.minor.patch-alpha.N`,
`major.minor.patch-beta.N`, or `major.minor.patch`. Every new distributed test update
increments the patch number; a rebuild of unchanged inputs does not consume a version.
Use `python scripts/curseforge_release.py bump --channel alpha` for the next test,
`--channel beta` for a completed set, and `--channel release` only after its beta has
passed the user's gameplay acceptance. The stable promotion removes the beta suffix.
Create `docs/releases/<version>.md` for every distributed version.

Push an annotated `v<mod_version>` tag to publish automatically. The CurseForge Release
workflow checks the tag, version inside the main JAR, loading-helper metadata, JSON,
publishing regression tests, native Java tests, and GameTests before uploading.
Beta/stable tags must point to source already merged into main. Alpha tags may point
to a task branch. The workflow also creates a GitHub release containing both JARs
and an upload receipt. Never overwrite/reuse a tag for changed inputs.

Completed source batch sets must be merged into main through a reviewed, validated PR;
this is standing authorization from Cameron on 2026-10-04. Never merge test-builds
into main. Stable promotion still waits for full beta testing.

## Credentials

GitHub repository secret: `CURSEFORGE_API_TOKEN`. The workflow injects it only into
the publishing step after validation; no secrets are used in pull-request builds.
Locally, `scripts/publish-curseforge.ps1` validates by default and uploads only with
`-Upload`. It accepts the process environment variable or imports the existing
current-user DPAPI credential at `%LOCALAPPDATA%/CosmicDungeon/secrets/curseforge.credential.xml`.
It restores the prior process environment afterward. Never log token values, pass them
on command lines, commit them to Gradle properties, or send them in chat.

Normal uploads use GitHub Actions as the single publisher. A manual fallback must first
download the tag's `curseforge-receipt.json` from its GitHub release into the local
receipt path (`../CosmicDungeon_AI/releases/<tag>/curseforge-receipt.json`). Do not run
the fallback concurrently with Actions. Local fallback checks the exact tag/HEAD.
After manual publication, save that receipt back onto the same GitHub release.

## Loading-screen distribution

NeoForge 10.0.32 discovers early services in top-level JARs before normal mod discovery.
The common gameplay JAR and client startup helper remain separate. Each release uploads
the helper as an additional file under the main file for archival; this is **not**
app-managed installation. Ordinary clients can play using NeoForge's default startup
screen without this optional cosmetic component.

To enable app-managed helper updates, create a Minecraft mod project named
**Cosmic Dungeon Loading Screen**, under the same CurseForge owner. Set its environment
to Client, explain that it is a NeoForge early-window library, and provide its project ID
as the GitHub Actions variable `CURSEFORGE_LOADING_PROJECT_ID`. Both projects then receive
the exact matching version automatically; the main file declares an optional visual
dependency so dedicated-server installations do not require the helper.
Install both projects once in the client profile, and enable Beta (Alpha for testers).
After the companion project exists, verify its approval, app recognition, and both updates
in a clean profile before retiring the transitional PS1 updater.

Existing configured clients: close Minecraft and replace the old helper with the matching
`cosmicdungeon-<version>-loading-screen.jar` in `mods`. Keep only one helper version.
For a new profile, also copy `src/main/loading-theme/theme-cosmicdungeon.json` to
`config/fml/theme-cosmicdungeon.json`; copy these authored assets under
`config/fml/cosmicdungeon/` using the filenames in the left column:

| Destination filename | Source under src/main/resources/assets/cosmicdungeon/ |
| --- | --- |
| cd_minecraft.png | textures/gui/title/cd_minecraft.png |
| cd_loading_background.png | textures/gui/loading/cd_loading_background.png |
| cd_progress_bar_bg.png | textures/gui/loading/cd_progress_bar_bg.png |
| cd_progress_bar_fg.png | textures/gui/loading/cd_progress_bar_fg.png |

Set `earlyLoadingScreenTheme = "cosmicdungeon"` and `earlyWindowProvider = "cosmicdungeon"`
in `config/fml.toml`, preserving unrelated settings. Never put the helper on the server.
This one-time profile setup can also be distributed as a CurseForge modpack after a
separate modpack project is created. The main mod upload does not distribute a world/map.

## Receipts, retries, and moderation

The publisher records source commit, both SHA256 hashes, project IDs and returned file IDs.
Each upload is checkpointed before submission and immediately after CurseForge responds.
Successful roles are skipped on a retry. An unresolved `pending` entry blocks another
upload: inspect the author console and reconcile the file ID rather than blindly posting
again. GitHub retains this checkpoint in a draft release even if a job is interrupted.
Accepted upload is not proof of moderation approval or visibility in the app.

Keep existing stopped-server/closed-client verification and hash-checked TEST deployment.
Never launch a development client automatically. The legacy test-builds feed remains a
transition path until both CurseForge client components have verified app distribution.
