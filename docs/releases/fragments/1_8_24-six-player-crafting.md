# Six-player crafting concurrency
Measure one/six actual subscriptions in a six-member consenting party using final ingredient payloads, cached/changed stock, serialized crafts, exact accounting/fairness and rejection/isolation checks. Optimize only demonstrated redundant work while retaining fresh transaction reads and normal server authority.

Reuse one immutable potion-stock projection per existing owner snapshot to remove measured per-viewer projection/cloning; preserve fresh execution reads and dirty/expiry/lifecycle rules.
