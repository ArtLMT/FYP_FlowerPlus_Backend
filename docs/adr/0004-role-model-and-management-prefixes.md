---
type: adr
id: 4
status: accepted
date: 2026-09-15
module: security
aliases:
  - Role model and management prefixes
tags:
  - flowerplus/adr
---

# ADR 0004 — Role hierarchy and two management URL prefixes

## Context

Most management work in the SRS is done by **Staff and Admin together** — products, materials,
vouchers, order status — while a few actions are the owner's alone (BR-AUTH-05/11, BR-PEN-13, and
the whole Finance side). Every business module from Catalog on ships its own management endpoints,
so the way those endpoints are authorized had to be settled once, before the first of them.

Three roles exist (`CUSTOMER`, `STAFF`, `ADMIN`). Two questions had to be answered:

1. Is Admin a superset of Staff, or a separate set of permissions?
2. How is a management endpoint marked, so every module does it the same way and a customer can
   never reach one?

## Decision

**Admin passes every Staff check, through a role hierarchy.** One static `RoleHierarchy` bean in
`SecurityConfig` declares `ROLE_ADMIN > ROLE_STAFF`. A staff-and-admin endpoint therefore checks
`hasRole('STAFF')` only; nothing is implied for `CUSTOMER`.

**Management endpoints live under two URL prefixes, each guarded twice:**

| Prefix | Who | URL rule in the filter chain |
|---|---|---|
| `/api/manage/**` | Staff and Admin | `hasRole('STAFF')` |
| `/api/admin/**` | Admin only | `hasRole('ADMIN')` |

The URL rule is a **second layer**. Each management endpoint still carries `@PreAuthorize`, so moving
one out of its prefix cannot silently open it, and the role is checked **before any lookup** — a
non-existent or non-owned target is a 404, never a 403 that would confirm it exists.

## Consequences

**Gained**

- One authorization shape for every module's Staff/Admin surface. A reviewer reads the prefix and
  knows who may call it.
- Admin never needs to be enumerated alongside Staff on each rule; the hierarchy covers it in one
  place. Finance, the one Admin-only module with no Staff side, sits under `/api/admin/**`.
- Two independent gates (URL rule + `@PreAuthorize`). `ManagementPrefixTest` proves both, and a
  Staff-403 test fails loudly if method security is ever switched off.

**Given up**

- No `STAFF > CUSTOMER` implication. Whether Staff should inherit customer abilities is deliberately
  left open (Permissions #5); until it is decided, a Staff account is not automatically a customer.
- Two annotations per endpoint rather than one. The redundancy is the point — neither layer is
  trusted alone.

## Alternatives considered

- **Resource paths only, authorized purely by `@PreAuthorize`.** Rejected: with no URL-prefix rule,
  a new management endpoint that forgets its annotation is open by default (`anyRequest`
  `.authenticated()` lets any signed-in customer through). The prefix makes the default deny.
- **Separate, unrelated permissions for Admin and Staff** (no hierarchy). Rejected: every
  staff-and-admin rule would then have to name both roles, and the SRS treats Admin as the owner who
  can do everything Staff can (BR-AUTH-05/11).
- **A single `/api/manage/**` prefix, distinguishing Admin-only actions by annotation alone.**
  Rejected: the Admin-only surface (Finance, staff management, user lookups) is large enough that a
  visible `/api/admin/**` boundary is clearer than scattered `hasRole('ADMIN')` annotations under a
  shared prefix.

---

Indexed in [[Decisions]]. Built in accounts P1; first endpoints under `/api/admin/**` are the user
and address lookups (P2) and the Staff-account surface (P3–P5).
