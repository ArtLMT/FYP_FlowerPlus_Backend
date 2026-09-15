---
type: adr
id: 2
status: accepted
date: 2026-09-10
module: address
aliases:
  - Address lives in the user module
tags:
  - flowerplus/adr
---

# ADR 0002 — Address lives in the user module, with no inverse collection

## Context

The build order grouped `address` with the catalog tables as one "Catalog & Address" module, on the
grounds that both are straightforward CRUD. That is a statement about effort, not about domain.

Two questions had to be answered before writing the entity:

1. Which module owns `address`?
2. How is the one-to-many between `user_account` and `address` mapped?

## Decision

**`Address` lives in `module/user`.** Three pieces of evidence from the schema and the security
model, none of which point at the catalog:

- `V1` declares `user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE` — the row's
  lifecycle is the account's.
- Every query against it is scoped to the authenticated user. There is no listing, searching or
  admin browsing of addresses.
- Nothing outside the user's own session reads it.

**The relation is mapped on the child only**, as `@ManyToOne(fetch = LAZY, optional = false)` with
`@JoinColumn(name = "user_id")`. `User` gets **no** `@OneToMany` inverse collection. This mirrors
`UserProfile`, which already owns its FK the same way.

## Consequences

**Gained**

- The `@ManyToOne` is an *intra*-module association. Had `Address` been filed under catalog, that
  module would compile-import `module.user.entity.User`, which is exactly the cross-module entity
  coupling AGENTS.md exists to prevent.
- Address lists stay pageable. A `@OneToMany` collection cannot be paged or filtered without loading
  all of it, so a user with forty addresses would load forty rows to show ten.
- `User` stays thin. It is loaded on every authenticated request, so every field hung off it is a
  field the auth path has to reason about.
- `LAZY` on the association costs nothing: the owner is already known from the security principal on
  every path that loads an address, so an eager fetch would be a second query for a value the caller
  already holds.

**Given up**

- No `user.getAddresses()` convenience. Callers go through `AddressService.listFor(owner, pageable)`,
  which is the intended path anyway.
- The user module now has two aggregates in it (account+profile, and addresses) rather than one.
  Acceptable while addresses stay user-scoped; if an admin-facing address surface ever appears, this
  decision should be revisited.

## Alternatives considered

- **A plain `UUID userId` column with no JPA association.** Would decouple the modules completely and
  make placing `Address` in the catalog module harmless. Rejected because the decoupling is not
  needed once the entity lives in the user module, and an untyped FK loses the ability to navigate to
  the owner at all.
- **A bidirectional mapping** with `@OneToMany(mappedBy = "user")` on `User`. Rejected for the paging
  and request-path cost above.

---

Indexed in [[Decisions]]. Module: [[address]].
