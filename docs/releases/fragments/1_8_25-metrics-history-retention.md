# Batch25 — metrics history retention

Retain all run XML and immutable resource segments. Harden existing offline copied-history reconstruction with bounded disk-indexed sequence/identity/afterimage checks and visible incomplete/conflicting-data failure, preserving originals and supported v1 legacy records. Ten isolated copy/restore/replay/growth tests and a documented storage policy; no migration, purge, new archive command or live recording change. [Policy](../../METRICS_HISTORY.md).
