---
type: module
module: auth
phase: "00"
status: shipped; password reset added 2026-09-11, untested
aliases:
  - Auth module
tags:
  - flowerplus/module
---

# Module: Auth

Registration, login, Google login, refresh-token rotation and password reset. Requirements:
[[Auth rules]].

## Password reset

| Endpoint | Body | Response |
|---|---|---|
| `POST /api/auth/forgot-password` | `{email}` | always `204` |
| `POST /api/auth/reset-password` | `{email, code, newPassword}` | `204`; `400 OTP_INVALID` for a wrong or expired code |

- Only ACTIVE and SUSPENDED accounts get a code. PENDING, BANNED and unknown emails get nothing,
  and the same `204`.
- The code is the same 6-digit code as registration (5 minutes, 5 attempts), namespaced by purpose,
  so a reset code can never verify a registration and vice versa.
- The email has the same layout as the verification email, with reset wording.
- A successful reset deletes all the account's refresh tokens and clears this browser's cookies.
  Access tokens already issued stay valid until they expire (`JWT_EXPIRATION`).
- New passwords follow the one password policy: 8–72 characters (`PasswordPolicy`).

## Traps

- **The send limits apply to every request, account or not.** Emails that get no code still go
  through `OtpService.throttle`, so a quick second request gets the same `429` either way.
  Skipping that for unknown emails would make the `429` itself reveal registered addresses.
- **At most 5 reset codes per email per 24 hours** (`OTP_RESET_DAILY_LIMIT`), answered with
  `429 OTP_DAILY_LIMIT_REACHED` rather than the resend interval's `OTP_THROTTLED`. One code survives
  5 guesses, but a fresh code every 60 seconds would otherwise allow about 7,200 guesses a day
  against one account.
- **The account is re-checked after the code verifies** — it may have been banned since the code
  was sent. The answer is the same `400` as a wrong code.

## Tests

- `OtpServiceImplTest` — plain unit test, runs anywhere: resend interval, daily cap, purpose
  separation.
- `PasswordResetTest` — integration: the full flow, sessions ended, no code for ineligible accounts,
  the same `429` for unknown emails, wrong code, password policy. **Not yet run.**

Indexed in [[Modules]].
