---
type: module
module: user
phase: "00"
status: shipped
tables:
  - user_account
  - user_profile
aliases:
  - User module
tags:
  - flowerplus/module
---

# Module: User

Accounts, profiles and delivery addresses ([[address]]). Auth drives the account lifecycle through
`UserService`; it never touches the repositories. Requirements: [[Auth rules]], [[Penalty rules]].

## Lifecycle

```
PENDING ──activate()─────▶ ACTIVE ──ban()────▶ BANNED
PENDING ──adoptAsStaff()─▶ ACTIVE ◀──unban()── BANNED
                           SUSPENDED   (no writer yet)
```

`ban()` / `unban()` are the Staff deactivation writers (accounts P4, Admin only). SUSPENDED still
has no writer — it arrives with the penalty rules (07). All four transitions are entity methods with
guards; `status` has no setter.

## Roles and management prefixes

Three roles: `CUSTOMER`, `STAFF`, `ADMIN`. **ADMIN passes every STAFF check** — one `RoleHierarchy`
bean in `SecurityConfig` (`ROLE_ADMIN > ROLE_STAFF`). CUSTOMER is implied by nothing.

| Prefix | Who | URL rule |
|---|---|---|
| `/api/manage/**` | Staff and Admin | `hasRole('STAFF')` |
| `/api/admin/**` | Admin only | `hasRole('ADMIN')` |

- The URL rule is a second layer. A management endpoint still carries `@PreAuthorize`, so moving it
  out of its prefix can't silently open it.
- Anyone else gets `403 ACCESS_DENIED`; no token is `401 UNAUTHENTICATED` first.
- Endpoints under `/api/admin/**`: the lookups `GET /api/admin/users/{id}` and
  `GET /api/admin/addresses/{id}` (P2), plus the Staff-account surface below (P3–P5).
  `/api/manage/**` is still empty — it fills from Catalog on.
- Tests: `RoleHierarchyTest` (plain) and `ManagementPrefixTest` — 404 there means "passed security,
  no endpoint yet".

Decision record: [[0004-role-model-and-management-prefixes]].

## Staff accounts

The Admin's Staff surface (BR-AUTH-05), all under `/api/admin/staff`, all Admin only, all targeting
STAFF accounts. `StaffAccountService` owns it; `AdminStaffController` is the web edge.

| Method | Path | Success |
|---|---|---|
| `POST` | `/api/admin/staff` | `201` + `Location: /api/admin/users/{id}`, `UserResponse`; `409 EMAIL_ALREADY_EXISTS` if the email is taken by an ACTIVE/SUSPENDED/BANNED account |
| `PUT` | `/api/admin/staff/{id}/deactivate` | `200` `UserResponse` (status BANNED) |
| `PUT` | `/api/admin/staff/{id}/reactivate` | `200` `UserResponse` (status ACTIVE) |
| `GET` | `/api/admin/staff` | `200` `PageResponse<UserResponse>` — `page`/`size` (default 20, max 100), newest first, optional `?status=ACTIVE\|BANNED` |

- **Create (P3).** The account is ACTIVE with a random password the Admin never sees; the person
  sets their own through the emailed code (see [[auth]] — a *welcome* mail, verified by the ordinary
  reset endpoint). A **PENDING** account on that email is *adopted* as STAFF (`User.adoptAsStaff`),
  its never-proven password discarded — a squatter can't block a hire. The name the Admin enters
  becomes the profile name.
- **Deactivate / reactivate (P4).** Deactivation is BANNED, reusing every existing blocked-account
  check: the next request is `401`, login is `403 ACCOUNT_BLOCKED`, refresh and forgot-password
  refuse. Reversible. Repeating either call is a `200` no-op, never a `500`. A non-STAFF id (a
  customer, the Admin) is `404 USER_NOT_FOUND` via `findByIdAndRole`.
- **List (P5).** Staff have no cap, so the list really pages. `?status` narrows it; the default lists
  every status.
- Tests: `StaffAccountTest` (create + deactivate) and `AdminStaffListTest` (list, paging, filter).

## Where each rule lives

| Rule | Owner |
|---|---|
| Who may authenticate | `UserAccountStatus.canAuthenticate()`: not BANNED, not PENDING |
| Who may place an order | `UserAccountStatus.canPlaceOrder()`: ACTIVE only. No caller until Order |
| Only PENDING can become ACTIVE | `User.activate()` throws otherwise |

`status` has no setter, so `activate()` is the only way to move it after construction.
`SecurityUser.isAuthBlocked` now delegates to the enum rather than holding its own copy of the rule.

## Traps

- **The OAuth2 BANNED check stays explicit.** `CustomOAuth2UserService` tests BANNED directly
  instead of `canAuthenticate()`, because that path *adopts* a PENDING account rather than
  rejecting it. This is deliberate (pre-hijack fix); do not "simplify" it.
- **`isAccountNonLocked` / `isEnabled` must stay in sync with `canAuthenticate()`.** They split the
  same set so login can say "blocked" vs "verify your email".

## Behaviour change (2026-09-11)

`verifyEmail` now rejects a correct code for an account that is no longer PENDING, with the same
400 `OTP_INVALID` as an expired code. It used to activate the account and issue tokens anyway, so:

- a code that outlived activation (its AFTER_COMMIT invalidate failed) worked as a login, and
- a PENDING account banned while its code was live would have been un-banned by verifying.

## Auditing

`User` extends `AuditableEntity`, so `user_account` records who created each account
(`created_by`) and who changed it last (`updated_by`) — both added in V6, both `REFERENCES
user_account(id) ON DELETE SET NULL`.

- **Filled automatically** by `SecurityAuditorAware`: the id of the signed-in `SecurityUser`.
  `SecurityUser` carries the id, so this costs no query.
- **`created_by` is `null`** when nobody was signed in: self-registration, Google login, the admin
  created at startup. A Staff account created by an Admin (P3) records the Admin as `created_by`, and
  a deactivation (P4) records them as `updated_by` — the audit trail for BR-AUTH-05 actions.
- **A write with nobody signed in leaves `updated_by` as it was.** Email verification, password
  reset and Google login change the row without touching the column. Verified by
  `UserAuditingTest`. So `updated_by` means "the last signed-in user who changed this account",
  not "the last change".
- The Google login principal is not a `SecurityUser`, so it never counts as an author.

## Admin account

There is exactly one Admin. `AdminAccountInitializer` runs on every startup:

- an admin already exists → does nothing;
- no admin, and `ADMIN_EMAIL` / `ADMIN_PASSWORD` are missing or break the password policy →
  **refuses to start**;
- no admin, but `ADMIN_EMAIL` already belongs to an account → **refuses to start**; an existing
  account is never made admin;
- otherwise → creates an ACTIVE, LOCAL admin with a profile.

The values live in the gitignored `.env`, never in a migration, so the password hash never enters
git. The tests set dummy values because they start the app too.

Not handled, on purpose: the owner losing access to the admin's mailbox, or handing the business
over. Skipped for now.

## Tests

`UserAccountStatusTest`, `UserTest`, `AdminAccountInitializerTest`: plain unit tests, no Spring.
`UserTest` covers the lifecycle transitions — `activate`, `adoptAsStaff`, `ban`/`unban` and the
states each one refuses.

Auditing: `SecurityAuditorAwareTest` (plain — who counts as the author) and `UserAuditingTest`
(Spring — the two columns in the database).

Indexed in [[Modules]].
