# In-Game Commands (Developer Reference) — 1.5.0

## Command roots registered by the mod

- `/world`
- `/creative`
- `/survival`
- `/metalmancer`
- `/more`
- `/door` (lock/info/count/pass_limit/reset_count/key)
- `/heal`
- `/fly`
- `/flyspeed`
- `/fullbright`
- `/day`
- `/night`
- `/riftdestination`
- `/shake`
- `/spawner`
- includes `/spawner keybind <1-5> <preset_name>` for preset hotkey assignment
- `/rift`
- `/region`
- `/rank`
- `/developer`
- `/dungeoneer`
- `/classselectordestination`
- `/currency`
- `/faction`
- `/progression`
- `/achievement`
- `/plantflags`
- `/vendor`
- `/trade`

## High-use command groups

### Spawner
`/spawner help|set|name|boss|cap|equip|enchant|drop|drops|showlabels|delay|preset|info|reset`

### Region (actual command tree)

- `/region wand`
- `/region new <name>`
- `/region create <name>`
- `/region new <name> copy <source>`
- `/region create <name> copy <source>`
- `/region look <name>`
- `/region look all` (nearby-region outline toggle; outlines include an x-ray overlay so hidden edges remain visible through walls while still respecting normal nearby/render-distance behavior)
- `/region info <name>`
- `/region here` (shows `/region info`-style output for all regions at your current position)
- `/region parent <region> <newParent>` (`none`/`null` clears parent)
- `/region delete <name>`
- `/region list`
- `/region flags <name>`
- `/region flags <name> <flag> <allow|deny|clear>`
- `/region flags <name> inherit <flags|exceptions> <on|off>`
- `/region flags <name> exceptions <place|break>`
- `/region flags <name> exceptions <place|break> <torch|ladder|water> <allow|deny|clear>` (`water` only for `place`)
- `/region flag list`
- `/region flag <flag> <allow|deny>` (applies to effective region at your location)

**Default behavior note:** newly created regions now default `interact=allow`.

### Door/Key
`/door lock|info|count|pass_limit|reset_count|key info|key duplicate`

### Rift
`/rift list|delete ...` and `/riftdestination ...` for destination mapping workflows.

### Class selector
`/classselectordestination ...` for per-slot, fallback, and max-player routing controls.

### Rank/authority
`/rank ...` for rank/password workflow across developer and dungeoneer authority boundaries.

## Notes for operators

- Many workflows require a player source (console-only invocations can fail for player-targeted UIs).
- Several commands are look-target dependent within short range.
- Failed syntax for `/spawner` falls back to help output with unknown syntax warning.


### Trade (1.5.0 player-to-player foundation)
`/trade <player>`
`/trade accept <player>`
`/trade deny <player>`
`/trade cancel`

Authority: normal players can send, accept, deny, and cancel their own trades. No developer/admin rank is required.

Notes:
- `/trade <player>` sends a pending invite to an online target and remains the typed fallback without a distance requirement.
- The **Trade Request** Controls keybind (default `CAPS LOCK`, category **Cosmic Dungeon**) sends the same invite path when the client is looking at another player within 3 blocks; the server revalidates online target, same dimension, close range, approximate look direction, active trade state, and cooldown before inviting.
- Incoming invites include clickable `[Accept Trade]` and `[Deny Trade]` chat buttons that run the command fallbacks.
- Pending invites expire after 30 seconds and requesters have a 3-second cooldown between successful invite sends.
- Players cannot start or accept another trade while already in an active trade.
- If the hotkey has no valid look target, the client shows: `Look at a player within 3 blocks to request a trade.`
- Trade cancellation, menu close, and logout/disconnect return offered items safely. Other-player offer slots are read-only.

### Currency (Attunement Fragment economy foundation)
`/currency balance [player]`
`/currency add <player> <trace|mark|seal|crown|anchor> <amount>`
`/currency remove <player> <trace|mark|seal|crown|anchor> <amount>`
`/currency set <player> <trace|mark|seal|crown|anchor> <amount>`
`/currency clear <player>`
`/currency capacity <player> <traceAmount>`
`/currency value`
`/currency value inventory`

Authority: self-balance and value-check commands are open to players; other-player/admin mutations require developer or console authority through AccessPolicy.

Pickup behavior: Attunement currency item entities auto-store into balance on pickup, with all-or-nothing capacity checks (no partial deposit).

### Faction (1.5.0 foundation)
`/faction get <player> <faction>`
`/faction set <player> <faction> <value>`
`/faction add <player> <faction> <delta>`
`/faction list <player>`

Authority: players can read their own faction values. Reading or mutating another player's faction values requires developer or console authority through AccessPolicy.



### Progression (1.5.0 long-term progression foundation)
`/progression get <player>`
`/progression d1 complete <player> <torchFlowers>`
`/progression lesser add <player> <amount>`
`/progression lesser set <player> <amount>`
`/progression cavern add <player> <amount>`
`/progression cavern set <player> <amount>`
`/progression village <player> <true|false>`

Authority: players can read their own progression. Reading another player's progression and all mutations require developer or console authority through AccessPolicy.


### Achievement (1.5.0 advancement foundation)
`/achievement grant <player> <achievementId>`
`/achievement counters <player>`
`/achievement counters reset <player>`
`/achievement idol return <player>`
`/achievement idol provide <provider> <receiver>`
`/achievement vitalexchange <provider> <receiver> <item>`

Authority: developer or console only through `AccessPolicy.requireDeveloperOrConsole`.

Notes:
- Uses `CosmicAdvancementUtil` with default criterion `triggered`.
- Intended as debug/admin tooling while gameplay triggers are implemented incrementally.
- Binding Idol debug hooks: use `idol return` and `idol provide` to increment idol counters and validate threshold advancement grants.


### Plant Flags (1.5.0 D1 foundation)
`/plantflags status`
`/plantflags reset`
`/plantflags setregion pos1`
`/plantflags setregion pos2`
`/plantflags complete-debug`

Authority: `status` is public. `reset`, `setregion`, and `complete-debug` require developer or console authority via `AccessPolicy.requireDeveloperOrConsole`.

Notes:
- Tracks per-run/session planting state and a 5-minute disconnect cooldown before completion can resolve.
- Completion grants `cosmicdungeon:achievements/plant_flags` to online eligible players and broadcasts placeholder JHW summon text.


### Vendor (1.5.0 progression + faction access)
`/vendor list`
`/vendor reload`
`/vendor profile <profileId>`
`/vendor assign <profileId>`
`/vendor clear`
`/vendor info`
`/vendor spawn <profileId>`
`/vendor access <profileId>`

Authority: developer or console only via `AccessPolicy.requireDeveloperOrConsole`.

Scope boundary:
- loads datapack vendor profiles from `data/cosmicdungeon/vendor_profiles/*.json`
- supports villager vendor assignment + vendor menu
- `/vendor access` explains whether the executing player can access a vendor profile and why
