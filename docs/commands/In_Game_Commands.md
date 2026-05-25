# In-Game Commands (Developer Reference) — 1.4.9

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
- `/region look all`
- `/region info <name>`
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
