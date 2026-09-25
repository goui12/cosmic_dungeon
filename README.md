# Cosmic Dungeon TEST builds

This artifact-only branch is maintained by scripts/publish-test-build.ps1 on the development branches.
latest-built is the most recent successful testing build. current-test is the revision verified on the TEST server and Cameron's installed client.
The portable updater reads current-test, pinning manifest and JAR to one Git commit. Both slots include a source commit and SHA-256. Builds can retain the same 1.5.1 version and still update correctly.
Do not merge this artifact branch into main. Runtime binaries are intentionally tracked here by Cameron's explicit instruction. Older revisions remain in Git history. Download only; no SFTP or GitHub credentials are required by testers.
