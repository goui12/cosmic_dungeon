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
The common gameplay JAR and client startup helper remain separate. With the companion
configured, each release uploads the helper there and the gameplay JAR to the main
project. Without a companion, or when resuming a legacy main file whose companion
relation was never uploaded, an additional-file archive preserves the helper; this is
**not** app-managed installation. A processing parent can reject an archive attachment;
reconcile the owner console and wait for parent approval before a controlled retry. Ordinary clients can play using NeoForge's default startup
screen without this optional cosmetic component.

The companion **Cosmic Dungeon Loading Screen** now exists under the same owner:
project ID `1727305`, slug `cosmic-dungeon-loading-screen`, with client-only file metadata.
GitHub Actions variables `CURSEFORGE_LOADING_PROJECT_ID` and
`CURSEFORGE_LOADING_PROJECT_SLUG` contain these values. Both projects receive
the exact matching version automatically. Set the repository variable
CURSEFORGE_LOADING_RELATION_APPROVED=true only after the companion project is approved.
The CLI equivalent is --link-loading (or the same process environment variable).
Until then, new uploads omit that optional dependency and record pending_relation.
Both exact JARs remain on their respective CurseForge projects and the GitHub prerelease;
no redundant archive is attached to a processing parent for these explicitly deferred pairs.
The companion may remain unavailable publicly until moderation completes; use the GitHub
helper download or transitional updater meanwhile. This does not claim app migration is complete.
Approved future releases can declare the optional visual dependency so dedicated servers
do not require the helper. Changing the flag never rewrites an already accepted file.
Older unlinked receipts without this explicit deferred-pair evidence retain archive recovery.
Install both projects once in the client profile, and enable Beta (Alpha for testers).
After the companion project exists, verify its approval, app recognition, and both updates
in a clean profile before retiring the transitional PS1 updater.

Existing configured clients: close Minecraft and replace the old helper with the matching
`cosmicdungeon-<version>-loading-screen.jar` in `mods`. Keep only one helper version.
The helper bundles the theme JSON and four authored PNGs. Before NeoForge loads the theme,
the selected Cosmic provider installs/refreshes only its own assets under config/fml,
preserving unrelated files and settings. Unchanged files are not rewritten. Asset preparation
failure is logged, and NeoForge's existing theme fallback remains available.

For a new profile, set `earlyLoadingScreenTheme = "cosmicdungeon"` and `earlyWindowProvider = "cosmicdungeon"`
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

## API version-name correction (2026-10-04)

Use the official upload API's gameVersionNames field. The game/versions endpoint has
multiple same-name Minecraft entries belonging to different dependencies. Type ID 1
is not a universal Minecraft mod version namespace; selecting it caused HTTP400/API1009
for 1.21.10 (ID13966). Project-scoped name resolution avoids that ambiguity.
The publisher validates requested names and keeps additional files version-free so
they inherit their parent. API failures report bounded, credential-redacted error
fields and still retain pending receipts until their outcome is reconciled.

The initial v1.5.2-beta.1 CI artifacts remain immutable. Its rejected upload was
reconciled against the owner console before a corrected manual publisher resumed
those same hashes. A tooling-only correction does not retag or rebuild that release.
Official contract: https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-api

Dependency relation projectID values must be JSON integers: the live API rejects quoted
IDs with HTTP400/API1002 despite the documentation example. The first beta main file is
9063474, companion file 9063592, and legacy archive 9063594. Upload acceptance is recorded;
companion project moderation and licensed-client app update testing remain separate gates.

The receipt records the companion ID/slug actually sent with a successful main upload.
A reused older main without that evidence retains its archive fallback even after a
companion is enabled; a retry never claims it added metadata to an already uploaded file.
