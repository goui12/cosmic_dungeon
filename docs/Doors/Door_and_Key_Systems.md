# Door & Key Systems (Developer) — 1.4.8

## System behavior

Door locking is runtime state on vanilla doors, backed by lock/passage tracking data.

- Door lock IDs map keys to specific doors.
- Door passage counts and pass limits are tracked.
- Redstone blocking logic can prevent bypass behavior on locked doors.

## Developer interactions

- Look-target workflow is required for most door commands (within 5 blocks).
- Key duplication only works for bound keys held in main hand.
- Door info/key info tools provide copy-ready lock metadata for build teams.

## Command subset

- `/door lock`
- `/door info`
- `/door count <n>`
- `/door pass_limit <n>`
- `/door reset_count`
- `/door key info`
- `/door key duplicate`

## Expected restrictions

- Non-door targets are rejected.
- Only vanilla doors are lockable by lock command.
- Unbound keys cannot be inspected/duplicated for lock workflows.
