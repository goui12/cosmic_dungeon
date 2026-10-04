# Region look in runtime instances - 2026-09-25

Cameron queued this fix for the next mod-code batch in Plan Custom Launcher Modifications.
Protection already maps physical instances to authored template dimensions; visualization
must apply that same existing server mapping while addressing packets to the actual world.

Branch: fix/region-look-instances-20260925, chained from fix/trade-currency-layout-20260925.
Scope: RegionLookServer.java, RegionLookServerTest.java, relevant region guide, this task,
and a unique release fragment. No central network registration, payload codecs, protection,
routing, registry, saved data, world files, template bounds or renderer changes.
No exclusive integration hotspot is modified; only visualization service/presentation docs.

Named look maps a matching template dimension to the viewer's physical world. All-look
filters through that same translation, then sends the physical dimension as before.
Other templates, other physical instances and old generations retain their own identities
and fail the current-world filter. Explicit physical and ordinary world outlines are preserved.
Existing chunk-radius refreshes, permissions, x-ray rendering and toggle semantics are retained.
No new tick loop, world scan, client dependency or migration. Datagen is not applicable.

Validation: Java21 build and all 60 JUnit cases passed, including three new dimension
regressions. All 1,997 source JSON files parsed; diff checks passed. No datagen applicable.
Licensed TEST QA: inside a fresh dungeon instance run /region look <known region> and
/region look all; verify authored bounds, toggles and movement refresh. Repeat in template/
overworld and a separate run; regions from a different template/run must not bleed through.
Existing protection still needs independent gameplay acceptance; outlines do not prove access rules.
No GameTest/dedicated launch; TEST activation still waits for stopped/closed confirmation.
Potential future improvement: validate named-outline behavior after traveling between worlds.
