# Release Assembly Prompt

- **Target version:** `<version>`; **fragment selection:** `<range>`; **constraints:** `<constraints>`.

Read `AGENTS.md`, `docs/ai/ARCHITECTURE_INDEX.md`, current release notes, and every `docs/releases/fragments/` file. This is the only normal task permitted to edit `docs/releases/Update_1.5.1.md`. Combine selected fragments into correct sections; preserve technical facts, compatibility/migration instructions, manual-QA limits, and player/developer boundaries. Remove duplicates, retain useful organization/cross-links, and add headings only when needed. Never overstate testing or migration evidence.

Modify no runtime code, resources, Gradle, workflows, or generated files. Delete only successfully incorporated fragments; retain unrelated/future fragments; verify no information was lost. Run documentation/link/search/JSON-if-applicable/diff checks and explain why runtime build, GameTests, and datagen do not apply to documentation-only work. Commit, push, create/update one PR, and never merge.
