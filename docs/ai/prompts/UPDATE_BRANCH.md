# Update Existing PR Branch Prompt

- **Branch:** `<branch>`; **PR:** `<number>`; **original intent:** `<intent>`.

Read `AGENTS.md` and `docs/ai/ARCHITECTURE_INDEX.md`; confirm branch/intent, fetch `origin/main`, inspect incoming commits, and explain overlap. Merge `origin/main` normally unless explicitly directed otherwise. Never silently discard either intent. Resolve only clearly preservable low-risk conflicts (independent docs, unique fragments, language entries, isolated tests, imports, non-overlapping layout constants).

Stop on registry IDs/order; payloads/codecs/handlers/dispatch; SavedData/NBT/presets/versions/migrations; currency/vendor/trade/inventory/repair transactions; dungeon snapshots/resets/restoration; rifts/RD/teleportation; Access Policy/class enforcement; or competing behavior. Report both intents, exact files/methods, safe options, compatibility effects, and recommendation. After a safe update run applicable build/GameTest/JSON/datagen/diff checks, push, update the existing PR, and never create a second PR or merge it.
