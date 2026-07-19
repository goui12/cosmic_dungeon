# AI Integration Planner Prompt

## Inputs
- **Backlog:** `<complete backlog>`
- **Special constraints:** `<constraints>`

Analyze the current repository, `AGENTS.md`, `docs/ai/ARCHITECTURE_INDEX.md`, and the latest `main` commit as the planning baseline. Plan manual serial integration (not GitHub merge queue) in safe execution waves. Same-wave tasks must be independent: never share a file, exclusive hotspot, registry/network/menu/SavedData/migration/transaction surface, dependency, or competing behavior. Isolate persistence, registry, networking, transactions, dungeon reset, teleportation, Access Policy, and class enforcement unless independence is proven. Prefer narrow tasks, keeping tightly coupled behavior together. Normal features use unique release fragments, never `docs/releases/Update_1.5.1.md`.

## Required task record
For every task provide: title; branch name; intended behavior; dependencies; exact files/directories; exclusive hotspots owned; possible central integration files; forbidden files/systems; saved-data/migration/registry/network/client-server implications; LOW/MEDIUM/HIGH risk; required build/GameTest/JSON/datagen/manual QA; release-fragment requirement; parallel/non-parallel reason against every other task; and a complete paste-ready Codex prompt.

## Required output
Finish with wave summary, recommended launch order, recommended merge order for every wave, cross-wave dependencies, and high-risk decisions requiring user approval.
