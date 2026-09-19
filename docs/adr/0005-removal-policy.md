---
type: adr
id: 5
status: accepted
date: 2026-09-19
module: cross-cutting
aliases:
  - Removal policy
tags:
  - flowerplus/adr
---

# ADR 0005 — How rows are removed: status, real delete, or never

## Context

Earlier notes said "no soft delete; removal is a real delete or a status change". That line had no
recorded decision behind it, and it confused two things: a status that hides a row *is* a form of
soft delete, and the SRS never approved real deletes for business data. Its removal rules all say
the opposite — keep the row:

- BR-PROD-11 — products are deactivated, not deleted, once orders reference them
- BR-MAT-08 — materials used by a recipe or transaction are not deleted
- BR-ORD-07 — orders are never deleted
- BR-INV-11 — inventory transactions are never edited or deleted

The code also holds two unused base classes, `SoftDeleteEntity` and `AuditableSoftDeleteEntity`, that
add a generic `is_deleted` flag. No table has the columns for them. Before the first catalog module,
one rule was needed for how any table is removed.

## Decision

**How a row is removed depends on what the row is.**

| Kind of table | Removal | Examples |
|---|---|---|
| Has a lifecycle the rules name | **A status column.** "Removed" is one of its states. | material, product, voucher, account, order |
| No lifecycle, and nothing needs the row after removal | **A real delete.** | address (orders copy it as text), category (removing it unlinks its products), product image |
| History | **Never removed.** Corrections are new rows. | inventory transactions, order status history, payments, penalty transactions |

**No generic `is_deleted` flag** until a table has no lifecycle *and* its removed rows must still be
referenced or restored. No current rule creates such a table. The two soft-delete base classes stay
unused until one does.

## Consequences

**Gained**

- Every removal traces to a rule. Where a rule names states (Active / Deactivated, Draft), those
  states are the only record of removal, so there is one source of truth.
- Rows that other rules still need stay visible. BR-MAT-07 keeps a deactivated material in existing
  recipes; a generic soft-delete filter would hide it from every query and break that rule.
- Tables with nothing depending on them stay small and need no filter.

**Given up**

- A real delete cannot be undone. Accepted only where nothing refers to the row afterwards.
- No uniform "deleted by / deleted at" across tables. For status-based tables, auditing
  (`updated_by`, `updatedAt`) records who changed the status and when.

## Alternatives considered

- **A generic `is_deleted` flag everywhere** (`SoftDeleteEntity`). Rejected: tables with a named
  lifecycle would then have two ways to say "gone" that can disagree (a product Deactivated but not
  deleted, or Active but deleted), and the filter hides rows rules still need (BR-MAT-07).
- **Real delete everywhere, blocked when referenced.** Rejected: it contradicts BR-PROD-11, BR-MAT-08
  and BR-ORD-07, which keep the row even where a delete would be technically possible.
- **Status on every table.** Rejected: an address or a product image has no states the business
  talks about; a status there is a flag under another name.

---

Indexed in [[Decisions]]. First applied to Material (BR-MAT-08, 2026-09-19) and to categories
([[Product rules]] #14).
