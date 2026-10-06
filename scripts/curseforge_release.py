"""Validated CurseForge publishing. Standard library only; credentials stay in env."""
import argparse
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import tomllib
import urllib.error
import urllib.request
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
API = "https://minecraft.curseforge.com/api"
REPO = "goui12/cosmic_dungeon"
VERSION = re.compile(r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-(alpha|beta)(?:\.([1-9]\d*))?)?\Z")
SERVICE = "META-INF/services/net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider"


def channel(version):
    match = VERSION.fullmatch(version)
    if not match or match[4] == "alpha" and match[5] is None:
        raise ValueError("Use major.minor.patch[-alpha.N|-beta|-beta.N].")
    return match[4] or "release"


def properties(root=ROOT):
    return dict(line.split("=", 1) for line in
                (root / "gradle.properties").read_text(encoding="utf-8-sig").splitlines()
                if "=" in line and not line.lstrip().startswith("#"))


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def digest(path):
    with path.open("rb") as handle:
        return hashlib.file_digest(handle, "sha256").hexdigest()


def validate(tag, root=ROOT):
    props = properties(root)
    version = props["mod_version"].strip()
    kind = channel(version)
    if tag != "v" + version:
        raise ValueError("Release tag must exactly match gradle.properties mod_version.")
    files = {"main": root / f"build/libs/cosmicdungeon-{version}.jar",
             "loading": root / f"build/libs/cosmicdungeon-{version}-loading-screen.jar"}
    with zipfile.ZipFile(files["main"]) as jar:
        metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
        mods = [mod for mod in metadata["mods"] if mod["modId"] == "cosmicdungeon"]
        if len(mods) != 1 or mods[0]["version"] != version:
            raise ValueError("Main jar's embedded version/mod ID does not match the release.")
        if SERVICE in jar.namelist():
            raise ValueError("Client early service must remain outside the shared runtime jar.")
    with zipfile.ZipFile(files["loading"]) as jar:
        if jar.read(SERVICE).decode().strip() != "net.goui.cosmicdungeon.loading.CosmicLoadingWindow":
            raise ValueError("Loading helper service metadata is incorrect.")
        manifest = jar.read("META-INF/MANIFEST.MF").decode()
        if "FMLModType: LIBRARY" not in manifest:
            raise ValueError("Loading helper must remain an early-service library.")
        if f"Implementation-Version: {version}" not in manifest.splitlines():
            raise ValueError("Loading helper's embedded version does not match the release.")
        for asset in ["theme-cosmicdungeon.json", "cd_minecraft.png", "cd_loading_background.png",
                      "cd_progress_bar_bg.png", "cd_progress_bar_fg.png"]:
            if not jar.read("cosmic-loading/" + asset):
                raise ValueError("Loading helper contains an empty theme asset.")
    changelog_path = root / f"docs/releases/{version}.md"
    changelog = changelog_path.read_text(encoding="utf-8")
    if not changelog.strip():
        raise ValueError("Release changelog is empty.")
    project = int(props["curseforgeProjectId"])
    if project != 1326805:
        raise ValueError("Unexpected main CurseForge project ID.")
    return {"version": version, "release_type": kind, "project_id": project,
            "minecraft": props["minecraft_version"], "changelog": changelog,
            "artifacts": {role: {"path": str(path), "filename": path.name,
                                  "sha256": digest(path)} for role, path in files.items()}}


def request(path, token, data=None, content_type=None):
    headers = {"X-Api-Token": token, "User-Agent": "CosmicDungeon-Publisher/1.0"}
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(API + path, data=data, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        # Expose only the bounded API error fields, with the credential redacted.
        # Never log request headers, arbitrary response bodies or HTML error pages.
        detail = ""
        try:
            failure = json.loads(error.read(8192))
            message = failure.get("errorMessage", "")
            code = failure.get("errorCode")
            if isinstance(message, str):
                message = " ".join(message.replace(token, "[REDACTED]").split())[:500]
                detail = f" (API {code}): {message}" if isinstance(code, int) else f": {message}"
        except (ValueError, AttributeError, OSError, http.client.HTTPException):
            pass
        finally:
            error.close()
        raise RuntimeError(f"CurseForge returned HTTP {error.code}{detail}; inspect the receipt before retrying.") from None
    except (urllib.error.URLError, TimeoutError):
        raise RuntimeError("CurseForge connection failed; inspect the receipt before retrying.") from None


def game_version_names(entries, minecraft, client_only=False):
    # Name lookup is resolved in the project's dependency namespace by CurseForge.
    # The API contains multiple identical Minecraft labels; type ID 1 is NOT a
    # universal Minecraft namespace (e.g. 1.21.10's mod version uses type 77784).
    names = [minecraft, "NeoForge", "Java 21", "Client"] + ([] if client_only else ["Server"])
    available = {entry["name"] for entry in entries}
    missing = [name for name in names if name not in available]
    if missing:
        raise ValueError(f"Unknown CurseForge version names: {', '.join(missing)}.")
    return names


def multipart(metadata, artifact):
    boundary = "CosmicDungeon" + uuid.uuid4().hex
    payload = (f'--{boundary}\r\nContent-Disposition: form-data; name="metadata"\r\n'
               'Content-Type: application/json\r\n\r\n').encode()
    payload += json.dumps(metadata).encode() + b"\r\n"
    payload += (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; '
                f'filename="{artifact["filename"]}"\r\nContent-Type: application/java-archive\r\n\r\n').encode()
    payload += Path(artifact["path"]).read_bytes()
    payload += f"\r\n--{boundary}--\r\n".encode()
    return payload, "multipart/form-data; boundary=" + boundary


def save_receipt(path, receipt, github_tag=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent, delete=False) as handle:
        json.dump(receipt, handle, indent=2)
        temporary = handle.name
    os.replace(temporary, path)
    if github_tag:
        subprocess.run(["gh", "release", "upload", github_tag, str(path), "--clobber", "--repo", REPO],
                       cwd=ROOT, check=True, stdout=subprocess.DEVNULL)


def publish(plan, receipt_path, loading_project, github_tag=None, loading_slug=None, link_loading=True):
    token = os.environ.get("CURSEFORGE_API_TOKEN", "").strip()
    if not token:
        raise ValueError("CURSEFORGE_API_TOKEN is missing. Use the local credential wrapper or Actions secret.")
    if loading_project and (loading_project <= 0 or loading_project == plan["project_id"]):
        raise ValueError("Loading-screen companion needs its own positive CurseForge project ID.")
    if loading_project and not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", loading_slug or ""):
        raise ValueError("Set CURSEFORGE_LOADING_PROJECT_SLUG to the companion project's URL slug.")
    commit = git("rev-parse", "HEAD")
    hashes = {role: item["sha256"] for role, item in plan["artifacts"].items()}
    receipt = {"version": plan["version"], "commit": commit, "sha256": hashes,
               "main_project": plan["project_id"], "files": {}}
    if receipt_path.exists():
        receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
        if any(receipt.get(key) != value for key, value in
               {"version": plan["version"], "commit": commit, "sha256": hashes,
                "main_project": plan["project_id"]}.items()):
            raise ValueError("Existing receipt belongs to different source/artifacts. Never overwrite a published version.")
        if receipt.get("pending"):
            raise ValueError("An upload has an unresolved outcome. Check CurseForge and reconcile its file ID before retrying.")
    entries = request("/game/versions", token)
    common = {"changelog": plan["changelog"], "changelogType": "markdown",
              "releaseType": plan["release_type"], "isMarkedForManualRelease": False}

    def upload(role, artifact_role, project_id, metadata):
        if role in receipt["files"]:
            if receipt["files"][role]["project_id"] != project_id:
                raise ValueError("Receipt project ID changed.")
            return receipt["files"][role]["file_id"]
        artifact = plan["artifacts"][artifact_role]
        data, content_type = multipart(metadata, artifact)
        receipt["pending"] = {"role": role, "project_id": project_id, "filename": artifact["filename"]}
        save_receipt(receipt_path, receipt, github_tag)
        result = request(f"/projects/{project_id}/upload-file", token, data, content_type)
        file_id = result.get("id")
        if not isinstance(file_id, int) or file_id <= 0:
            raise RuntimeError("Upload returned no valid file ID; inspect CurseForge before retrying.")
        receipt["files"][role] = {"project_id": project_id, "file_id": file_id}
        if role == "main":
            # Record which relation was actually sent with this accepted main file.
            receipt["main_loading_project"] = loading_project if link_loading else None
            receipt["main_loading_slug"] = loading_slug if loading_project and link_loading else None
            if loading_project and not link_loading:
                receipt["loading_relation_deferred"] = loading_project
                receipt["pending_relation"] = {"project_id": loading_project, "slug": loading_slug,
                                               "reason": "companion relation approval not confirmed"}
        receipt.pop("pending")
        save_receipt(receipt_path, receipt, github_tag)
        print(f"CurseForge accepted {role}: project {project_id}, file {file_id} (moderation may still be pending).")
        return file_id

    if loading_project:
        upload("loading_companion", "loading", loading_project,
               dict(common, displayName=f'Cosmic Dungeon Loading Screen {plan["version"]}',
                    gameVersionNames=game_version_names(entries, plan["minecraft"], True)))
    metadata = dict(common, displayName=f'Cosmic Dungeon {plan["version"]} - NeoForge {plan["minecraft"]}',
                    gameVersionNames=game_version_names(entries, plan["minecraft"]))
    if loading_project and link_loading:
        # Optional visual component: dedicated servers do not need the early-display library.
        metadata["relations"] = {"projects": [{"slug": loading_slug, "projectID": loading_project, "type": "optionalDependency"}]}
    main_id = upload("main", "main", plan["project_id"], metadata)
    # The companion already stores the exact helper. An additional archive is only
    # needed without it; attaching to a still-processing parent can fail with HTTP500.
    main_linked = (loading_project and receipt.get("main_loading_project") == loading_project
                   and receipt.get("main_loading_slug") == loading_slug)
    deferred_pair = (receipt.get("loading_relation_deferred") == loading_project
                     and receipt["files"].get("loading_companion", {}).get("project_id") == loading_project
                     and bool(loading_project))
    if not main_linked and not deferred_pair:
        # Legacy/reconciled mains may predate companion configuration. Keep the
        # discoverable archive unless the accepted main is known to include its link.
        upload("loading_archive", "loading", plan["project_id"],
               dict(common, parentFileID=main_id, displayName=f'Loading screen archive {plan["version"]} (manual install)'))
    receipt["status"] = "submitted"
    # Presence of a companion project alone does not mean the accepted main links it.
    receipt["loading_app_managed"] = bool(main_linked)
    save_receipt(receipt_path, receipt, github_tag)
    return receipt


def bump(kind, root=ROOT):
    path = root / "gradle.properties"
    text = path.read_text(encoding="utf-8-sig")
    old = properties(root)["mod_version"].strip()
    match = VERSION.fullmatch(old)
    if not match or match[4] == "alpha" and match[5] is None:
        raise ValueError("Current mod_version is not a supported version.")
    if kind == "release":
        if match[4] != "beta":
            raise ValueError("Only a tested beta may be promoted to a stable release.")
        new = ".".join(match.group(1, 2, 3))
    else:
        new = f"{match[1]}.{match[2]}.{int(match[3]) + 1}-{kind}.1"
    path.write_text(re.sub(r"(?m)^mod_version=.*$", "mod_version=" + new, text), encoding="utf-8")
    print(f"Version changed: {old} -> {new}. Add docs/releases/{new}.md before publishing.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["validate", "upload", "bump"])
    parser.add_argument("--tag")
    parser.add_argument("--channel", choices=["alpha", "beta", "release"])
    parser.add_argument("--receipt", type=Path, default=ROOT / "build/release/curseforge-receipt.json")
    parser.add_argument("--loading-project", type=int, default=int(os.environ.get("CURSEFORGE_LOADING_PROJECT_ID") or "0"))
    parser.add_argument("--loading-slug", default=os.environ.get("CURSEFORGE_LOADING_PROJECT_SLUG"))
    parser.add_argument("--link-loading", action="store_true",
                        default=os.environ.get("CURSEFORGE_LOADING_RELATION_APPROVED", "").lower() == "true",
                        help="Declare the optional relation only after the companion project is approved.")
    parser.add_argument("--github-release", action="store_true")
    args = parser.parse_args()
    if args.command == "bump":
        if not args.channel:
            parser.error("bump needs --channel")
        bump(args.channel)
        return
    if not args.tag:
        parser.error("validate/upload needs --tag")
    plan = validate(args.tag)
    if args.command == "validate":
        print(json.dumps({key: value for key, value in plan.items() if key != "changelog"}, indent=2))
    else:
        if git("rev-parse", args.tag + "^{commit}") != git("rev-parse", "HEAD"):
            raise ValueError("Release tag does not identify the checked-out commit.")
        publish(plan, args.receipt, args.loading_project, args.tag if args.github_release else None, args.loading_slug, args.link_loading)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError, OSError, KeyError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        print(f"Publishing stopped: {error}", file=sys.stderr)
        sys.exit(1)
