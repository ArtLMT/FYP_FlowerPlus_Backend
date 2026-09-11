---
type: module
module: address
phase: 01a
status: built-untested
tables:
  - address
migration: V5
aliases:
  - Address module
tags:
  - flowerplus/module
---

# Module: Address

Delivery addresses belonging to a user. Part of `module/user`, not a module of its own —
[[0002-address-lives-in-the-user-module]]. The SRS has no address rules; the ones below are
proposed in [[Address rules]].

## Tables owned

| Table | Migration | Notes |
|---|---|---|
| `address` | created in `V1`, indexed in `V5` | `ON DELETE CASCADE` from `user_account` |

`V5` adds what V1 omitted:

- `idx_address_user_id` — every read is `WHERE user_id = ?`; V1 created no indexes at all.
- `ux_address_one_default_per_user` — **partial** unique index (`ON address (user_id) WHERE
  is_default`). Partial because plain `UNIQUE (user_id, is_default)` would also forbid two
  *non*-default addresses.

## Public surface

`AddressService` is a concrete `@Service`, not interface + impl: nothing outside the module calls
it, there is no second implementation and no test fake, so an interface would be symmetry only.

```java
Page<Address> listFor(User owner, Pageable pageable);
Address       getOwned(User owner, UUID addressId);
Address       addAddress(User owner, String receiverName, String phone, String address, boolean makeDefault);
Address       updateAddress(User owner, UUID addressId, String receiverName, String phone, String address, boolean makeDefault);
Address       setDefault(User owner, UUID addressId);
void          deleteAddress(User owner, UUID addressId);
```

## Endpoints

| Method | Path | Success |
|---|---|---|
| `GET` | `/api/addresses` | `200` `PageResponse<AddressResponse>`, default first |
| `GET` | `/api/addresses/{id}` | `200` |
| `POST` | `/api/addresses` | `201` + `Location` |
| `PUT` | `/api/addresses/{id}` | `200` |
| `PUT` | `/api/addresses/{id}/default` | `200` |
| `DELETE` | `/api/addresses/{id}` | `204` |

`SecurityConfig` was not touched — `anyRequest().authenticated()` already covers all six.

## Business rules

1. **Ownership is a query, not an annotation.** Everything goes through `findByIdAndUserId`.
   Somebody else's id raises `AddressNotFoundException` → **404**, not 403. A 403 confirms the row
   exists. Same reasoning as the profile-read fix in `89affe4`.
2. **The first address a user creates becomes their default**, asked for or not — so a user with
   addresses always has one selected at checkout.
3. **Editing cannot clear the default.** This is enforced by the entity's shape, not by logic:
   `Address` has no setters, only `edit(...)` (which never touches `isDefault`) and `markDefault()`.
   There is no method that clears a single address's default; only the bulk `clearDefaultFor`.
4. **Deleting the default promotes the oldest remaining address.** Deletion is a real `DELETE`:
   `orders` snapshots `delivery_address` / `recipient_name` / `recipient_phone` as text with no FK
   here, so no order history can be orphaned.

## Traps

The partial unique index is checked **per row** and cannot be deferred (partial indexes cannot be
`DEFERRABLE`). Two consequences the code depends on:

- `setDefault` clears the old default in a **separate, earlier statement**. The tempting one-liner
  `UPDATE ... SET is_default = (id = :target)` can transiently hold two `true` rows depending on
  visit order, and fails.
- `deleteAddress` must `flush()` before promoting a replacement. Hibernate orders updates ahead of
  deletes at flush time, so otherwise the promotion hits the index while the old row still exists.

## Events

Published: — Consumed: — Checkout will read the default address through `AddressService`.

## Tests

`AddressCrudTest` — 14 tests, one per rule, plus validation, 401 and malformed-UUID 400.

> [!warning] Never executed
> Blocked by a machine-level JDK failure: `Selector.open()` throws `Unable to establish loopback
> connection` (`sun.nio.ch.UnixDomainSockets.connect0` → `Invalid argument: connect`), which kills
> every `@SpringBootTest` context including the four pre-existing auth suites. Not caused by this
> module. Every rule above is unverified.

The two cross-user 404 tests are the ones to protect: if either becomes a 403, the API has started
leaking the existence of other users' rows.

Indexed in [[Modules]].
