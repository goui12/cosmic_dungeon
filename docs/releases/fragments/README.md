# Release Fragments

- Normal feature and bug-fix pull requests must create one unique Markdown fragment in this directory.
- Normal feature pull requests must not directly edit `docs/releases/Update_1.5.1.md`.
- Only a dedicated release-assembly task may combine fragments into the main release notes.
- Workflow-setup, temporary investigation, and documentation-only tasks may omit a fragment when they do not represent a player-facing, developer-facing, operational, compatibility, or release-relevant change.

## File Naming

Use `<task-or-pr>-<short-name>.md`. Before a PR number exists, use a unique task or branch slug.

Examples:

- `vendor-capacity-check.md`
- `help-menu-scroll-fix.md`
- `184-repair-cancellation.md`

Names must be lowercase, concise, filesystem-safe, and unique.

## Required Fragment Content

Each fragment should contain only applicable sections from:

- Summary.
- Player-facing behavior.
- Developer or operator behavior.
- Exact systems affected.
- Saved-data, registry, NBT, preset, or migration effects.
- Compatibility with existing 1.5.0 worlds.
- Server/client and security implications.
- Automated validation performed.
- Remaining manual Minecraft QA.
- Backup, update, and rollback instructions when applicable.
- Documentation pages that were added or updated.

Do not invent testing, migration, or compatibility claims.

## Fragment Boundaries

- Keep each fragment scoped to one pull request.
- Do not copy the entire PR description.
- Do not duplicate unrelated release history.
- Do not edit another active task’s fragment.
- Do not use a shared fragment between parallel tasks.
- If one PR contains several tightly coupled changes, keep them in one fragment with clear subsections.
- Future-version changes must identify their intended release and must not be assembled into 1.5.1 accidentally.

## Release Assembly

A dedicated release-assembly task must:

- Read every applicable fragment.
- Merge its information into the correct release-note sections.
- Preserve compatibility, migration, testing, and manual-QA limitations.
- Remove duplicate wording.
- Delete only fragments that were successfully incorporated.
- Leave unrelated or future-release fragments untouched.
- Never claim more validation than the fragments provide.
