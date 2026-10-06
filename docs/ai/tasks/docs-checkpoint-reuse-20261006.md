# Narrative documentation checkpoint reuse

Cameron approved Approach A on 2026-10-06, following an explicit AGENTS conflict review.
Task branch: chore/docs-checkpoint-reuse-20261006. Base: 161ad72cc26ab3270f0a3c47fb534a66f3871618.
Exclusive development hotspot: Integration Gate workflow/verifier. No concurrent writer.
Files: AGENTS.md; .github/workflows/build.yml; scripts/docs_checkpoint.py;
scripts/tests/test_docs_checkpoint.py; this task report.

## Behavior and quality boundary
Only allowlisted narrative planning/completion Markdown can reuse a successful ancestor full
integration run with identical non-narrative Git objects/modes. Full proof is bound to repository,
branch, workflow, run/attempt and source head; API job steps independently verify clean build
and native GameTests actually succeeded. Proof is an immutable run artifact, not a user claim.
Current checkout fingerprint includes PR base changes. All source, scripts, tests, workflows,
Gradle settings and docs/config-examples remain protected inputs. Missing/invalid evidence fails
closed to full validation. Releases still run the separate complete release workflow.
The verifier checks document encoding, relative file links and diff whitespace. It retains
machine-readable decision/proof artifacts. No runtime/saved-data/network/registry changes;
no migration or datagen is applicable. No mod version change or newly distributed JAR is needed.

## Validation and measured baseline
Passed locally: 17 verifier regressions plus 17 publisher checks; Java21 build and diff checks.
Pending: full integration CI and actual narrative-only follow-up reuse.
Batch11 final four-file narrative checkpoint CI37496418873 took144seconds, including53seconds
clean build and28seconds nativeGameTests. Credit savings cannot be measured from this evidence.
First rollout deliberately performs full tests because the workflow/verifier themselves changed.
A following narrative-only checkpoint must prove actual reuse before reporting the optimization
as verified end to end.

## Manual QA / boundaries
No new gameplay/UI to test. Existing licensed multiplayer QA remains separate.
Authentication, release publisher, stopped-server/closed-client checks and artifact hashes unchanged.
Possible future improvement: consolidate routine evidence retrieval around these compact receipts.
Batch12 allied-wolf protection remains next; it will receive full gameplay validation.

## Full validation checkpoint
CI37502567598 passed the clean build and all36 native GameTests on3bc3856c.
All349 JUnit and2 loading tests remain intact;34 Python checks pass.
This narrative-only completion update exercises the approved evidence-reuse path.
GitHub review of the cumulative PR found a prior Batch11 outsider-recovery result omission;
it will be corrected on Batch11's existing branch before the next gameplay release.

Full workflow including bytecode-free verifier passed CI37503267794. This final narrative update
exercises real proof reuse; no gameplay or build inputs changed.
