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
PENDING ──activate()──▶ ACTIVE
                          SUSPENDED   (no writer yet)
                          BANNED      (no writer yet)
```

Nothing in production code sets SUSPENDED or BANNED yet; there is no admin endpoint for them.

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
They run even while `@SpringBootTest` contexts cannot start on this machine.

Indexed in [[Modules]].
