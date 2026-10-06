import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("checkpoint", Path(__file__).parents[1] / "docs_checkpoint.py")
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


class CheckpointTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.root_patch = patch.object(gate, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        self.git("init", "-q")
        self.git("config", "user.name", "Checkpoint Test")
        self.git("config", "user.email", "test@example.invalid")
        self.write("src/main/java/Test.java", "baseline\n")
        self.write("docs/ai/tasks/test.md", "# Completion\n")
        self.write("docs/config-examples/input.json", "{}\n")
        self.write(".github/workflows/build.yml", "trusted\n")
        self.commit()
        self.sha = self.git("rev-parse", "HEAD").strip()
        self.run = {"id": 12, "run_attempt": 1, "conclusion": "success", "status": "completed",
                    "path": gate.WORKFLOW, "head_branch": "feature/test", "head_sha": self.sha,
                    "event": "pull_request", "head_repository": {"full_name": gate.REPOSITORY}}
        self.proof = {"schema": 1, "kind": "full", "run_id": 12, "run_attempt": 1,
                      "source_head": self.sha, "repository": gate.REPOSITORY,
                      "workflow": gate.WORKFLOW, "fingerprint": gate.tree_fingerprint()}
        self.jobs = [{"name": "Build + GameTests", "conclusion": "success", "steps": [
            {"name": "Clean build", "conclusion": "success"},
            {"name": "Run GameTests", "conclusion": "success"}]}]

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, stderr=subprocess.PIPE,
                                       text=True)

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def commit(self):
        self.git("add", ".")
        self.git("commit", "-qm", "test")

    def trusted(self):
        return gate.trusted_run(self.run, "feature/test", 13, self.sha,
                                gate.tree_fingerprint(), self.proof, self.jobs)

    def fake_api(self, path, binary=False):
        if "/workflows/" in path:
            return {"workflow_runs": [self.run]}
        if path.endswith("/artifacts"):
            return {"artifacts": [{"id": 31, "name": "checkpoint-proof-12-1", "expired": False}]}
        if path.endswith("/jobs"):
            return {"jobs": self.jobs}
        self.fail("Unexpected API request " + path)

    def decide(self):
        with patch.object(gate, "api", side_effect=self.fake_api), \
             patch.object(gate, "read_proof", return_value=self.proof):
            return gate.decide("feature/test", self.git("rev-parse", "HEAD").strip(), 13)

    def test_allowlist_is_narrow(self):
        for path in ["docs/ai/tasks/test.md", *gate.EXACT_DOCS]:
            self.assertTrue(gate.narrative(path))
        for path in ["AGENTS.md", "README.md", "docs/config-examples/input.json",
                     "docs/ai/tasks/nested/test.md", "docs/ai/tasks/test.py",
                     "docs/ai/tasks/../test.md", "docs/ai/tasks/test.MD"]:
            self.assertFalse(gate.narrative(path), path)
        for mode in ["100755", "120000", "160000"]:
            self.assertFalse(gate.narrative("docs/ai/tasks/test.md", mode))

    def test_narrative_edit_reuses_successful_native_tests(self):
        self.write("docs/ai/tasks/test.md", "# Verified completion\n")
        self.commit()
        result = self.decide()
        self.assertFalse(result["full"])
        self.assertEqual(12, result["baseline_run"])
        self.assertEqual(["docs/ai/tasks/test.md"], result["documents"])

    def test_dirty_narrative_only_is_eligible_locally(self):
        self.write("docs/ai/tasks/test.md", "# Updated completion\n")
        self.assertFalse(self.decide()["full"])

    def test_each_non_narrative_input_requires_full_validation(self):
        baseline = self.proof["fingerprint"]
        for name in ["src/main/java/Test.java", "src/test/java/RegressionTest.java",
                     "docs/config-examples/input.json", ".github/workflows/build.yml",
                     "scripts/docs_checkpoint.py", "scripts/tests/test_docs_checkpoint.py",
                     "gradle.properties", "AGENTS.md"]:
            with self.subTest(name=name):
                self.git("reset", "--hard", self.sha)  # Disposable test repository only.
                self.write(name, "changed\n")
                self.commit()
                self.assertNotEqual(baseline, gate.tree_fingerprint())
                self.assertTrue(self.decide()["full"])

    def test_dirty_runtime_input_requires_full(self):
        self.write("src/main/java/Test.java", "changed\n")
        self.assertTrue(self.decide()["full"])

    def test_untracked_input_requires_full(self):
        self.write("src/new.java", "new\n")
        self.assertTrue(self.decide()["full"])

    def test_forged_or_stale_proof_rejected(self):
        self.assertTrue(self.trusted())
        for key, value in [("schema", 2), ("kind", "reused"), ("run_id", 99),
                           ("run_attempt", 2), ("source_head", "0" * 40),
                           ("repository", "other/repo"), ("workflow", "other.yml"),
                           ("fingerprint", "wrong")]:
            with self.subTest(key=key), patch.dict(self.proof, {key: value}):
                self.assertFalse(self.trusted())

    def test_failed_wrong_branch_fork_or_current_run_rejected(self):
        for key, value in [("id", 13), ("conclusion", "failure"), ("status", "in_progress"),
                           ("path", "other.yml"), ("head_branch", "other"),
                           ("event", "pull_request_target"),
                           ("head_repository", {"full_name": "untrusted/fork"})]:
            with self.subTest(key=key), patch.dict(self.run, {key: value}):
                self.assertFalse(self.trusted())

    def test_nonancestor_rejected(self):
        with patch.object(gate, "ancestor", return_value=False):
            self.assertFalse(self.trusted())

    def test_skipped_or_failed_native_gates_rejected(self):
        for step in self.jobs[0]["steps"]:
            for conclusion in ["skipped", "failure", None]:
                with patch.dict(step, {"conclusion": conclusion}):
                    self.assertFalse(self.trusted())

    def test_missing_proof_requires_full(self):
        with patch.object(gate, "api", return_value={"workflow_runs": []}):
            self.assertTrue(gate.decide("feature/test", self.sha, 13)["full"])

    def test_broken_link_fails_document_validation(self):
        self.write("docs/ai/tasks/test.md", "[missing](no-such-file.md)\n")
        with self.assertRaises(ValueError):
            gate.check_documents(["docs/ai/tasks/test.md"])

    def test_regular_relative_link_validates(self):
        self.write("docs/ai/tasks/test.md", "[source](../../../src/main/java/Test.java)\n")
        gate.check_documents(["docs/ai/tasks/test.md"])

    def test_modes_and_symlinks_are_protected_inputs(self):
        baseline = b"100644 blob aaa\tdocs/ai/tasks/test.md\0"
        for mode in [b"100755", b"120000", b"160000"]:
            changed = mode + b" blob aaa\tdocs/ai/tasks/test.md\0"
            self.assertNotEqual(gate.fingerprint(baseline), gate.fingerprint(changed))

    def test_unavailable_or_malformed_evidence_falls_back_to_full(self):
        for error in [OSError("offline"), ValueError("bad JSON"), KeyError("missing")]:
            with patch("sys.argv", ["docs_checkpoint.py"]), \
                 patch.dict(gate.os.environ, {"GITHUB_RUN_ID": "0", "GITHUB_OUTPUT": "", "CHECKPOINT_HEAD": self.sha}), \
                 patch.object(gate, "decide", side_effect=error):
                gate.main()
            result = json.loads((self.root / "build/checkpoint/decision.json").read_text())
            self.assertTrue(result["full"])

    def test_local_unverified_evidence_has_nonzero_exit(self):
        with patch("sys.argv", ["docs_checkpoint.py", "--local"]), \
             patch.dict(gate.os.environ, {"GITHUB_RUN_ID": "0", "GITHUB_OUTPUT": "", "CHECKPOINT_HEAD": self.sha}), \
             patch.object(gate, "decide", return_value={"full": True, "reason": "no proof"}):
            with self.assertRaises(SystemExit) as error:
                gate.main()
            self.assertEqual(2, error.exception.code)

    def test_oversized_or_expired_artifact_rejected(self):
        for artifact in [{"expired": True}, {"size_in_bytes": 1_000_001}]:
            with self.assertRaises(ValueError):
                gate.read_proof(artifact)


if __name__ == "__main__":
    unittest.main()
