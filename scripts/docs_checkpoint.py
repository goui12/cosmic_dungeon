"""Reuse full integration evidence only for narrow narrative documentation checkpoints.

Every tracked non-allowlisted Git path, object ID and mode contributes to the
fingerprint, including this script, tests, workflows and docs/config-examples.
A missing proof or any uncertainty requests a full build. Release CI is separate.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import urllib.parse
import zipfile

REPOSITORY = "goui12/cosmic_dungeon"
WORKFLOW = ".github/workflows/build.yml"
PROOF = "checkpoint-proof.json"
SCHEMA = 1
ROOT = Path(__file__).resolve().parents[1]
EXACT_DOCS = {"docs/ai/D1_REMAINING.md", "docs/ai/PARTY_SKILLS_BATCHES_20261006.md"}


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, stderr=subprocess.PIPE)


def narrative(path, mode="100644"):
    return mode == "100644" and (path in EXACT_DOCS or
        re.fullmatch(r"docs/ai/tasks/[a-zA-Z0-9_-]+\.md", path) is not None)


def fingerprint(records):
    kept = []
    for record in records.split(b"\0"):
        if not record:
            continue
        info, path = record.split(b"\t", 1)
        mode, kind, oid = info.decode("ascii").split()
        name = path.decode("utf-8", errors="strict")
        if not (kind == "blob" and narrative(name, mode)):
            kept.append(record)
    return hashlib.sha256(b"\0".join(sorted(kept))).hexdigest()


def tree_fingerprint():
    return fingerprint(git("ls-tree", "-rz", "HEAD"))


def dirty_paths():
    return [p.decode("utf-8") for p in
            git("diff", "--name-only", "-z", "HEAD").split(b"\0") if p]


def check_documents(paths):
    """Validate only narrative documents; relative file links must resolve."""
    for name in paths:
        if not narrative(name):
            continue
        path = ROOT / name
        if not path.exists():
            continue  # A deleted narrative document has no body to validate.
        if path.is_symlink():
            raise ValueError("Narrative checkpoint contains a symbolic link")
        body = path.read_text(encoding="utf-8")
        if "\0" in body:
            raise ValueError("Narrative checkpoint contains binary data")
        for target in re.findall(r"\[[^\]\n]*\]\(([^)\s]+)\)", body):
            if target.startswith(("#", "http:", "https:", "mailto:")):
                continue
            link = urllib.parse.unquote(target.split("#", 1)[0])
            if link and not (path.parent / link).exists():
                raise ValueError(f"Broken local link in {name}: {target}")
    git("diff", "--check")
    git("diff", "--cached", "--check")


def api(path, binary=False):
    data = subprocess.check_output(
        ["gh", "api", path], cwd=ROOT, stderr=subprocess.PIPE, timeout=30)
    return data if binary else json.loads(data)


def ancestor(sha, head):
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        return False
    return subprocess.run(["git", "merge-base", "--is-ancestor", sha, head],
                          cwd=ROOT, capture_output=True).returncode == 0


def trusted_run(run, branch, current_id, head, current_fingerprint, proof, jobs):
    if (run.get("id") == current_id or run.get("conclusion") != "success"
            or run.get("status") != "completed"
            or run.get("path") != WORKFLOW
            or run.get("head_branch") != branch
            or (run.get("head_repository") or {}).get("full_name") != REPOSITORY
            or run.get("event") not in ("pull_request", "push", "workflow_dispatch")):
        return False
    sha = run.get("head_sha", "")
    if not ancestor(sha, head):
        return False
    if (proof.get("schema") != SCHEMA or proof.get("kind") != "full"
            or proof.get("run_id") != run.get("id")
            or proof.get("run_attempt") != run.get("run_attempt")
            or proof.get("source_head") != sha
            or proof.get("repository") != REPOSITORY
            or proof.get("workflow") != WORKFLOW
            or proof.get("fingerprint") != current_fingerprint):
        return False
    # Independently confirm that this run actually executed both required gates.
    for job in jobs:
        if job.get("name") == "Build + GameTests" and job.get("conclusion") == "success":
            steps = {s["name"]: s.get("conclusion") for s in job.get("steps", [])}
            return steps.get("Clean build") == steps.get("Run GameTests") == "success"
    return False


def read_proof(artifact):
    if artifact.get("expired") or artifact.get("size_in_bytes", 1_000_001) > 1_000_000:
        raise ValueError("Missing, expired or oversized proof artifact")
    payload = api(f"repos/{REPOSITORY}/actions/artifacts/{artifact['id']}/zip", binary=True)
    if len(payload) > 1_000_000:
        raise ValueError("Oversized proof download")
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        entry = archive.getinfo(PROOF)
        if entry.file_size > 32_768:
            raise ValueError("Oversized proof document")
        return json.loads(archive.read(entry))


def decide(branch, head, current_id):
    changed = dirty_paths()
    if any(not narrative(p) for p in changed):
        return {"full": True, "reason": "Working build inputs changed"}
    # Refuse untracked inputs; existing ignored/generated build outputs are not source.
    untracked = git("ls-files", "--others", "--exclude-standard", "-z").split(b"\0")
    if any(p and not p.startswith(b"build/") for p in untracked):
        return {"full": True, "reason": "Untracked source or document needs full validation"}
    if changed:
        for record in git("diff", "--raw", "HEAD").decode("utf-8").splitlines():
            metadata = record.split("\t", 1)[0].split()
            if metadata[1] not in ("100644", "000000"):
                return {"full": True, "reason": "Narrative working file mode changed"}
    check_documents(changed)
    current = tree_fingerprint()
    query = urllib.parse.urlencode({"branch": branch, "status": "success", "per_page": 30})
    runs = api(f"repos/{REPOSITORY}/actions/workflows/build.yml/runs?{query}")["workflow_runs"]
    for run in runs[:30]:
        if run.get("id") == current_id or not ancestor(run.get("head_sha", ""), head):
            continue
        artifacts = api(f"repos/{REPOSITORY}/actions/runs/{run['id']}/artifacts")["artifacts"]
        expected = f"checkpoint-proof-{run['id']}-{run.get('run_attempt')}"
        matches = [a for a in artifacts if a.get("name") == expected and not a.get("expired")]
        if len(matches) != 1:
            continue
        proof = read_proof(matches[0])
        jobs = api(f"repos/{REPOSITORY}/actions/runs/{run['id']}/attempts/{run['run_attempt']}/jobs")["jobs"]
        if trusted_run(run, branch, current_id, head, current, proof, jobs):
            names = git("diff", "--name-only", run["head_sha"], "HEAD").decode("utf-8").splitlines()
            # Fingerprint covers merge/base changes as well as PR head changes.
            # This extra source-head diff avoids hiding runtime changes behind an unusual merge.
            if any(not narrative(p) for p in names):
                continue
            check_documents(names)
            git("diff", "--check", run["head_sha"], "HEAD")
            return {"full": False, "reason": "Identical build inputs; narrative checkpoint only",
                    "baseline_run": run["id"], "baseline_head": run["head_sha"],
                    "fingerprint": current, "documents": sorted(set(names + changed))}
    return {"full": True, "reason": "No matching successful full integration proof"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--record-full", action="store_true")
    parser.add_argument("--local", action="store_true")
    args = parser.parse_args()
    path = ROOT / "build" / "checkpoint"
    path.mkdir(parents=True, exist_ok=True)
    head = os.environ.get("CHECKPOINT_HEAD") or git("rev-parse", "HEAD").decode().strip()
    branch = os.environ.get("GITHUB_HEAD_REF") or os.environ.get("GITHUB_REF_NAME")
    branch = branch or git("branch", "--show-current").decode().strip()
    run_id = int(os.environ.get("GITHUB_RUN_ID", "0"))
    if args.record_full:
        if not run_id or os.environ.get("GITHUB_REPOSITORY") != REPOSITORY:
            raise SystemExit("Full proofs are issued only by this repository's integration workflow")
        receipt = {"schema": SCHEMA, "kind": "full", "run_id": run_id,
                   "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
                   "repository": REPOSITORY, "workflow": WORKFLOW,
                   "source_head": head, "checkout": git("rev-parse", "HEAD").decode().strip(),
                   "fingerprint": tree_fingerprint()}
        (path / PROOF).write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
        print("Recorded full integration proof")
        return
    try:
        result = decide(branch, head, run_id)
    except (OSError, ValueError, KeyError, TypeError, AttributeError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        # Never turn unavailable GitHub evidence or malformed data into permission to skip.
        result = {"full": True, "reason": f"Evidence unavailable or invalid ({type(error).__name__})"}
    (path / "decision.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as handle:
            handle.write(f"full={str(result['full']).lower()}\n")
    print(json.dumps(result))
    if args.local and result["full"]:
        raise SystemExit(2)  # Explicitly requires a Java21 build, never silent success.


if __name__ == "__main__":
    main()
