---
type: adr
id: 6
status: accepted
date: 2026-09-24
module: cross-cutting
aliases:
  - Optimistic locking for management edits
tags:
  - flowerplus/adr
---

# ADR 0006 — Optimistic locking for management edits

## Context

Two Staff can read the same mutable record and submit different changes. Without a concurrency
check, the later write silently overwrites the earlier one. Auditing records who wrote last, but
does not prevent the loss.

Material exposes this now; Product, Address, Staff accounts and other management records will have
the same problem. Inventory reservations and voucher redemption limits are different: they contend
for scarce quantities, not a human-edited record, and remain their own Order/Inventory locking
decision.

## Decision

**Mutable aggregate roots that are edited by people use optimistic locking.**

- A new migration adds an explicit non-null numeric `version` column, initially `0`, to each chosen
  aggregate root. Do not use PostgreSQL's internal `xmin`.
- A read response includes `version`. Every update, state transition, or deletion command supplies
  the version the caller read in its JSON body.
- A stale version fails with `409 CONCURRENT_MODIFICATION`. The response carries no special payload;
  the client reloads the resource, shows the newer state, and lets the user decide whether to apply
  their intended change again.
- The server never automatically retries a human edit. That would recreate a silent overwrite.
- The version belongs on the aggregate root. Child rows such as recipe entries and images are
  protected through their parent; join tables and append-only history are not versioned.

This applies to Material, Product, Address, and Staff/User management when those records are
mutable. Each module Design names its aggregate root and migration. It does **not** decide stock
reservation, batch quantity, voucher redemption, or other scarce-resource concurrency; their
operations require a separately chosen guarded update or database lock.

## Consequences

**Gained**

- A Staff member never silently overwrites another Staff member's completed edit.
- The same visible API rule and `ErrorCode` apply to management modules.
- The version number is deterministic; timestamps are not used as a concurrency guard.

**Given up**

- The frontend must retain the version returned by a read and handle a conflict by refreshing.
- A second editor sometimes repeats their edit after reviewing the new state.
- Each affected table needs a versioned migration and tests for the stale-write path.

## Alternatives considered

- **Last write wins.** Rejected: it loses a completed Staff edit without telling either person.
- **Pessimistic locks for all edits.** Rejected: ordinary edits may take minutes while a person fills
  in a form; holding a database lock for that time wastes connections and can block unrelated work.
- **`updated_at` as the version.** Rejected: timestamp precision and clock handling make it a weaker,
  less explicit compare-and-update value.
- **Automatic retry after a conflict.** Rejected: retrying a human's old intent overwrites the newer
  value the conflict was meant to protect.

---

Indexed in [[Decisions]]. First applied to Material backlog H14 (2026-09-24).
