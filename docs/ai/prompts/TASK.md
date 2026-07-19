# Implementation Task Prompt

## Task Inputs
- **Title:** `<title>`
- **Behavior:** `<intended behavior>`
- **Base / branch:** `<base>` / `<task branch>`
- **Dependencies:** `<dependencies>`
- **Files:** `<intended files>`; **hotspots owned:** `<hotspots>`
- **Forbidden:** `<files/systems>`
- **Acceptance criteria:** `<criteria>`
- **Documentation/tests/manual QA:** `<requirements>`

Read `AGENTS.md` and `docs/ai/ARCHITECTURE_INDEX.md`; confirm the base branch and inspect adjacent code before editing. State touch set and all implications. Keep one task on one branch and one PR; reuse existing architecture; avoid unrelated cleanup; stop before entering another task’s hotspot. Preserve server authority and persisted-data compatibility; add migrations/tests for storage changes; run applicable NeoForge datagen; create one unique release fragment for normal feature work. Run every validation required by `AGENTS.md`, commit, push, create/update one PR, never merge, and provide its full completion report.
