# Cosmic Dungeon In-Game Commands

This document lists every command registered by the mod at server command registration time.

## Access legend
- **Any player**: no elevated permission required (`hasPermission(0)` or equivalent).
- **OP**: Minecraft permission level 2 required.
- **Developer/Console**: gated by `AccessPolicy.requireDeveloperOrConsole`.
- Some commands still do player-only checks at runtime, even when registration allows console.

## Registered commands

### World and dungeon lifecycle
- `/world <target>` *(Developer/Console; player execution path teleports the player)*
- `/world save <target>` *(Developer/Console)*
- `/world reset <target> [snapshot]` *(Developer/Console)*
- `/world saves <target>` *(Developer/Console)*
- `/world runs [target]` *(Developer/Console)*

### Game mode / utility
- `/creative [target]` *(OP)*
- `/survival [target]` *(OP)*
- `/heal [target]` *(OP)*
- `/fly` *(OP, toggles flight)*
- `/flyspeed <speed>` *(OP; speed range `0.0..10.0`)*
- `/fullbright` *(OP, toggles night vision)*
- `/day` *(OP)*
- `/night` *(OP)*
- `/more [amount]` *(OP; amount range `1..2304`, default `64`)*
- `/shake` *(Any player entity; sends a client shake payload)*

### Class / rank / auth
- `/metalmancer` *(Any player)*
- `/metalmancer ore <amount>` *(Any player; amount `>= 0`)*
- `/rank`
- `/rank help`
- `/rank password <oldPassword> <newPassword>`
- `/rank <target> <rank> <password>`
- `/developer <password>`
- `/dungeoneer` *(Developer/Console)*

### Rift commands
- `/rift destination new <name>`
- `/rift destination delete <name>`
- `/rift destination info <name>`
- `/rd new <name>`
- `/rd delete <name>`
- `/rd info <name>`
- `/rift list [dimension]` *(OP)*
- `/rift delete <pos>` *(OP)*
- `/rift delete <name>` *(OP)*
- `/rift delete <dimension> <pos>` *(OP)*
- `/rift delete <dimension> <name>` *(OP)*

### Region commands *(Developer role or console)*
- `/region wand`
- `/region new <name>`
- `/region create <name>`
- `/region look all`
- `/region look <name>`
- `/region info <name>`
- `/region parent <region> <newParent>`
- `/region delete <name>`
- `/region list`

#### Region flags (named region)
- `/region flags <name>`
- `/region flags <name> <flag> <allow|deny|clear>`
- `/region flags <name> inherit <flags|exceptions> <on|off>`
- `/region flags <name> exceptions <place|break>`
- `/region flags <name> exceptions <place|break> <ex> <allow|deny|clear>`

#### Region flags (region at your current position)
- `/region flag list`
- `/region flag <flag> <allow|deny>`

### Class selector destination commands *(Developer/Console)*
- `/classselector ui dest <pos>`
- `/classselector ui destslot <pos> <slot>`
- `/classselector ui players <pos>`
- `/classselector dest set <pos> <destination>`
- `/classselector dest clear <pos>`
- `/classselector destslot set <pos> <slot> <destination>`
- `/classselector destslot clear <pos> <slot>`
- `/classselector players set <pos> <count>`

### Spawner commands
- `/spawner label show|hide|<enabled>` *(OP or Developer/Console)*
- `/spawner boss on|off|<enabled>` *(OP or Developer/Console)*
- `/spawner set <mob>` *(Any player)*
- `/spawner delay <ticks>` *(Any player)*
- `/spawner delayrange <minTicks> <maxTicks>` *(Any player)*
- `/spawner range <blocks>` *(Any player)*
- `/spawner count <count>` *(Any player)*
- `/spawner players <blocks>` *(Any player)*
- `/spawner cap <count>` *(Any player)*
- `/spawner stats` *(Any player)*

### Door commands
- `/door count` *(Any player; raycasts target door)*
- `/door pass_limit <limit>` *(Any player; `0` clears limit)*
- `/door reset_count` *(Any player)*
- `/door lock`
- `/door info`
- `/door key info`
- `/door key duplicate`

---

## Source of truth
The command list above is derived from `onRegisterCommands(...)` in `CosmicDungeonMod` and the `register(...)` methods of each referenced command class.
