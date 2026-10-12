# Metrics history retention and reconstruction

Default policy is **retain all**. There is no purge schedule, new retention cap, archive job, compression policy or database migration. The server-local directory is `<world>/Dungeon Instance Metrics`; world UUID and deterministic world/run instance IDs remain stable.

## Stored records and views

- **identity.xml**: the stable world identity.
- `<instance-UUID>.xml`: canonical active/checkpoint/final run, outcome, members and statistics. Completed, abandoned and startup-aborted records remain. Active records are never removed by reconstruction.
- **totals.xml** and `players/<owner-UUID>.xml`: lifetime server/owner projections plus last10 run references. The ten-reference view does not delete older canonical runs.
- `resources/<instance-UUID>/<transaction-UUID>.xml`: every immutable v1 resource segment, including own/shared events, exact owners/items/quantities, sequence and donor cursor/ranks. All segments remain.
- `resource-state/<instance-UUID>.xml`: exact cumulative afterimage and latest64 events. The current run snapshot also embeds this recent tail; older supported v1 embedded records remain readable. These bounded views do not truncate the segment archive.
- **pending.xml**: existing exact-afterimage finalization journal. Retain it and all associated authoritative transaction evidence until normal recovery settles it. Unsupported/conflicting schemas fail visibly.

Never totalize the segments again on top of canonical run statistics. Own resource use remains in segment/state detail; the canonical used statistic is shared use, with contributions separate.

## Offline copies and existing reconstruction

A backup/restore is a lossless offline copy of the entire metrics directory and related authoritative save/transaction data, preserving identity, active runs, originals and journals. This card adds no new backup or archival command. Work with stopped/offline copied inputs; preserve their originals.

The existing RunMetricsStore command `rebuild <archive-copy> <separate-empty-directory>` reconstructs finalized history/projections. It is not a complete world backup and does not relocate/delete active runs, unrelated files or originals. Existing supported journal recovery may replay exact afterimages on the copied input; unsupported/unresolved evidence blocks opening/reconstruction. A failed output is incomplete and must not be used as a verified restore.

Declared segmented history now requires a complete sequence, exact world/run/transaction/owner identities, cursor progression and equality with the complete saved afterimage. Missing directory/state/middle segments, truncated XML, duplicate sequences or conflicting state fail visibly before that run enters output totals. Original bytes are retained. A temporary disk index holds one run's segments, processing one bounded segment and bounded cumulative state at a time; cleanup removes only those temporary verification copies. No history-sized runtime cache or new dependency is added.

Supported pre-segment v1 aggregate-only/embedded-row records are preserved, including an empty resources directory. They do not acquire invented zero totals or segment detail. If both segment and state evidence vanished completely, v1 cannot distinguish that loss from pre-segment history; describe available coverage honestly and retain the canonical records. Partial legacy canonical totals/tails remain independent of a valid segmented afterimage; reconstruction copies each and never guesses a migration boundary.

## Measured growth

The isolated Java fixtures measure logical XML file bytes, not filesystem allocation, SavedData, the world, compressed backups, latency or TPS. All use six members and real v1 writers/readers. Completed/aborted samples use8 crafts/48events; the heavy sample uses120 crafts/720events,64rotating item types and128mob keys/member. Only64recent rows are displayed, while all120heavy segments remain. Exact measured values from the coordinated build follow.

| Scenario | Crafts / events | Canonical XML bytes | State bytes | All segments bytes | Lifetime totals bytes | Directory bytes | Segment bytes/event |
|---|---:|---:|---:|---:|---:|---:|---:|
| completed | 8 / 48 | 14584 | 13820 | 14828 | 3912 | 47260 | 308.917 |
| aborted | 8 / 48 | 14584 | 13820 | 14828 | 3912 | 47260 | 308.917 |
| resource-heavy | 120 / 720 | 88037 | 58544 | 224022 | 113493 | 484212 | 311.142 |

Directory total includes 116 bytes of world identity and one-run lifetime snapshots for server/six owners. Counts include every persisted segment; all three rebuilt copies match their originals exactly. Ten new focused tests passed with the complete767-test Java21 build,0failures/errors,2004sourceJSON and scoped link/diff validation.

Treat measured bytes/run/event as scenario-specific estimates. Retain-all segment storage grows with every event; cumulative state/totals grow with distinct keys and owners. Extrapolation assumes the same shapes, name/key lengths and workload. Shared lifetime totals are not another per-run segment archive, so do not multiply their entire snapshot per historical run. Backups/copies and filesystem metadata add separate costs. No measurement authorizes automatic deletion.

[Batch25 source/validation report](ai/tasks/BATCH_1_8_25_20261012.md).
