# Update 1.5.0 Notes (Initial Implementation)

## Attunement Fragment economy account foundation

This is the first 1.5.0 implementation pass and introduces backend account plumbing only.
No vendors, purchase GUI, or gameplay sinks/sources are included in this update.

### Currency model

Legal-tender denominations (non-vanilla currency):

- TRACE
- MARK
- SEAL
- CROWN
- ANCHOR

Exchange ratio is decimal by tier:

- 10 Trace = 1 Mark
- 10 Mark = 1 Seal
- 10 Seal = 1 Crown
- 10 Crown = 1 Anchor

All balances are stored internally as total `Trace` using `long`.

### Persistence and account data

- Added persistent player economy data via overworld `SavedData`.
- Balance data persists across server restarts.
- Per-player capacity override support is included.
- Default account capacity is currently `10,000 Trace`.

### Commands

Added `/currency` root command:

- `/currency balance`
- `/currency balance <player>`
- `/currency add <player> <denomination> <amount>`
- `/currency remove <player> <denomination> <amount>`
- `/currency set <player> <denomination> <amount>`
- `/currency clear <player>`
- `/currency capacity <player> <traceAmount>`
- `/currency value`
- `/currency value inventory`

Permission model:

- Any player can run self-balance.
- Other-player balance checks and all admin mutations require developer/console authority.

### Scope boundary

This update intentionally does **not** include:

- vendors
- economy GUI/menu screens
- world sinks/sources beyond pickup auto-storage

These are planned for future 1.5.x follow-up work.


## 1.5.0 Follow-up: Attunement fragment item tender + auto-storage

- Added five legal-tender item forms: `attunement_trace`, `attunement_mark`, `attunement_seal`, `attunement_crown`, and `attunement_anchor`.
- These items are drop-compatible as world entities but auto-deposit into player currency balance on successful pickup.
- Pickup uses all-or-nothing capacity checks: if full capacity blocks the full stack value, no partial deposit occurs and the entity remains in-world.
- Capacity-denied pickup sends a short, rate-limited actionbar denial message.
- No crafting recipes were added for these tender items.


## 1.5.0 Follow-up: Vendor pricing foundation (no vendors yet)

- Added a new pricing package: `net.goui.cosmicdungeon.economy.pricing`.
- Added foundational types: `VendorValueCategory`, `VendorPrice`, `GearSetDefinition`, and `VendorPricingService`.
- Added starter isolated hardcoded set definitions (structured for later JSON migration under `data/cosmicdungeon/vendor_prices/*.json`).
- Included starter pricing for the D2 T1 Judicator chainmail set:
  - `visor_of_the_resolute`
  - `cuirass_of_purpose`
  - `chausses_of_the_pledge`
  - `sabatons_of_the_unheard_oath`
- Starter values: 10 Trace per individual piece, 100 Trace for complete set detection.
- Added `/currency value` to report the held item sell value/debug source.
- Added `/currency value inventory` to report detected complete sets and notable inventory values.
- Scope boundary remains unchanged: no vendor NPCs, no vendor GUI, no item removal in pricing evaluation.


## 1.5.0 Follow-up: Faction foundation (JHW baseline only)

- Added faction foundation package: `net.goui.cosmicdungeon.faction`.
- Added `FactionTier` ladder: HOSTILE, SUSPICIOUS, INDIFFERENT, CORDIAL, FAVORABLE, WARMLY, ALLY.
- Added faction definition registry with `cosmicdungeon:jhw` baseline:
  - scale: 0-500
  - default starting value: 80 (SUSPICIOUS)
  - persisted per-player in overworld `SavedData`
- Added service API for read/set/adjust/tier checks with min/max clamping.
- Added `/faction` command root with get/set/add/list subcommands.
- Permission model keeps AccessPolicy as central authority:
  - self-read is public
  - reading or mutating other players requires developer/console authority

Scope boundary: this pass does not add vendors, NPC interactions, achievement triggers, atonement dungeons, or hostile behavior hooks.


## 1.5.0 Follow-up: Long-term progression foundation (D1/D2 unlock data only)

- Added persistent long-term progression package: `net.goui.cosmicdungeon.progression`.
- Added `PlayerProgressionData` SavedData keyed by player UUID with:
  - D1 Torch Flower best count (0-6)
  - D1 completion flag for >=3 Torch Flowers
  - Lesser Blooms total
  - Cavern Residue total
  - Village access unlock flag
  - D1 NPC unlock tier (0-4)
  - D2 NPC unlock tier (0-4, reserved for future hooks)
  - generic string progression flags set for future expansion unlocks
- Added `ProgressionService` API for reads/writes and derived calculations.
- D1 NPC unlock tiers now derive from Lesser Blooms:
  - 0-4 => tier 0
  - 5-9 => tier 1
  - 10-14 => tier 2
  - 15-19 => tier 3
  - 20+ => tier 4
- Village access unlocks once D1 is completed with at least 3 Torch Flowers.
- Added `/progression` command root for debug/admin mutation workflows.
- Permission model keeps AccessPolicy as central authority:
  - self-read allowed
  - reading others or mutating progression requires developer/console

Scope boundary: this pass does not wire dungeon completion events, collection events, NPC vendor logic, or any gameplay trigger hooks yet.


## 1.5.0 Follow-up: Achievement + advancement foundation

- Added new package: `net.goui.cosmicdungeon.achievement` with:
  - `CosmicAchievementIds` for centralized advancement `ResourceLocation` ids.
  - `CosmicAdvancementUtil` safe server-side grant helper (`triggered` default criterion).
  - `AchievementCounterData` persisted per-player counters/bitmasks for incremental trigger implementation.
- Added developer/console-only `/achievement` command root:
  - `/achievement grant <player> <achievementId>`
  - `/achievement counters <player>`
  - `/achievement counters reset <player>`
- Added advancement datagen provider (`ModAdvancementProvider`) and registered NeoForge `AdvancementProvider` in `DataGenerators` server datagen path.
- Achievement advancements are now generated from datagen instead of manually maintained JSON files in `src/main/resources`.

Scope boundary: this pass provides framework + tooling only; gameplay/event triggers for these achievements are intentionally not wired yet.


## 1.5.0 Follow-up: D1 Plant Flags achievement foundation

- Added package `net.goui.cosmicdungeon.achievement.plantflags` with persisted state machine foundation for D1 Plant Flags.
- Added `PlantFlagData` SavedData including active run/session id, planted player UUID set, disconnect cooldown timestamp, completion flag, and configurable region bounds.
- Added `PlantFlagService` with APIs to record banner planting, clear per-run state, compute online eligible players, evaluate cooldown/eligibility, and complete once ready.
- Added event hooks for banner placement and disconnect handling:
  - Banner placements only count when the placed block is a banner and inside configured Plant Flags region.
  - Disconnects/link-dead events trigger a 5-minute cooldown lockout before completion can occur.
- Added `/plantflags` command root:
  - `/plantflags status` (public)
  - `/plantflags reset` (developer/console)
  - `/plantflags setregion pos1` and `/plantflags setregion pos2` (developer/console)
  - `/plantflags complete-debug` (developer/console)
- Completion behavior currently grants `cosmicdungeon:achievements/plant_flags` and broadcasts:
  - `"The planted banners stir. JHW answers."`
- Scope boundary preserved: no new JHW entity was introduced in this pass. A TODO marker is included where physical JHW summon/NPC interaction flow will later replace the placeholder broadcast.


## 1.5.0 Follow-up: Binding Idol achievement tracking hooks + debug commands

- Implemented `net.goui.cosmicdungeon.achievement.BindingIdolAchievements` as the central tracker for Binding Idol achievement counters and threshold grants.
- Added API hooks:
  - `recordReturnedThroughBindingIdol(ServerPlayer revivedPlayer)`
  - `recordProvidedBindingIdol(ServerPlayer provider, ServerPlayer receiver)`
- Both hooks update `AchievementCounterData` and grant threshold achievements for:
  - returns from death through a binding idol: 1/5/10/50/100
  - provides binding idol to a dungeoneer: 1/5/10/50/100
- Added developer/console debug commands under `/achievement`:
  - `/achievement idol return <player>`
  - `/achievement idol provide <provider> <receiver>`
- Scope note: no standalone Binding Idol gameplay system was introduced in this pass; only achievement API hooks and debug wiring were added for safe incremental integration.


## 1.5.0 Follow-up: Vital Exchange I-IV foundation

- Added `net.goui.cosmicdungeon.achievement.VitalExchangeAchievements` as the central API for Vital Exchange grants:
  - `recordVitalExchange(ServerPlayer provider, ServerPlayer receiver, ItemStack providedStack)`
- Trigger eligibility rules implemented:
  - provided stack quantity must be greater than 0
  - receiver must currently be `Deadeye` (via `ClassNbtUtil` + `ClassKeys`)
  - both provider and receiver receive the corresponding achievement grant
- Item-to-achievement mapping implemented:
  - `scintilla_vitalis` -> `vital_exchange_1`
  - `lux_vitalis` -> `vital_exchange_2`
  - `mending_sting` -> `vital_exchange_3`
  - `verdant_jolt` -> `vital_exchange_4`
- Current item registration status in `ModItems`:
  - `scintilla_vitalis` and `lux_vitalis` are already registered
  - `mending_sting` and `verdant_jolt` are not yet registered; this pass uses stable `ResourceLocation` TODO constants to avoid inventing item behavior
- Added developer/console debug command wiring under `/achievement`:
  - `/achievement vitalexchange <provider> <receiver> <item>`

Scope boundary: this pass does not add a full player trade HUD/system; it only adds achievement service + debug trigger wiring for safe incremental integration.

## D1 Environmental Achievement Trackers

Added isolated D1 tracker services under `net.goui.cosmicdungeon.achievement.d1`:
- `TiredNotBrokenTracker`
- `SixfoldVigilTracker`
- `CycleOfRecordedSoundTracker`
- `SynchronousPealTracker`

Added `/achievement d1regions` tooling:
- `/achievement d1regions status`
- `/achievement d1regions set <regionName> pos1`
- `/achievement d1regions set <regionName> pos2`

Added debug grant command:
- `/achievement d1 debug <achievementName> <player>`

Default region keys expected by the trackers:
- `d1_woodland_manor`
- `d1_wither_room`
- `d1_camp_5`
