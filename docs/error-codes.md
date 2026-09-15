---
type: reference
module: api
aliases:
  - Error codes
tags:
  - flowerplus/api
---

# Error codes

The contract between the API and the frontend for everything that goes wrong. Decision and
reasoning: [[0001-no-response-envelope]] (amended 2026-09-13).

## How to read a response

1. **2xx** — the body is the resource itself, or empty for `204`.
2. **Anything else** — the body is an `ErrorResponse`. Switch on `errorCode`, never on `message`.

Every error has the same frame. `details` appears only for the codes listed under it.

```json
{
  "success": false,
  "status": 429,
  "error": "Too Many Requests",
  "errorCode": "OTP_THROTTLED",
  "message": "A code was sent recently. Please wait before requesting another.",
  "path": "/api/auth/resend-otp",
  "timestamp": "2026-09-13T08:00:00Z",
  "details": { "retryAfterSeconds": 42 }
}
```

| Field | Meaning |
|---|---|
| `errorCode` | What happened. One of the codes below. The only field to branch on. |
| `message` | What happened this time, in English, for developers and logs. **Do not show it to users** — the frontend owns all user-facing text, keyed by `errorCode`. |
| `details` | Present only for the codes below; absent otherwise. |
| `status` | The HTTP status again, for convenience. |
| `error` | The HTTP reason phrase for `status` (`"Unauthorized"`). Same information as `status`. |
| `path`, `timestamp` | Diagnostics. `timestamp` is an ISO-8601 UTC string. |
| `success` | Always `false`. Kept for compatibility. |

### `details` by code

| Code | `details` |
|---|---|
| `OTP_THROTTLED`, `OTP_DAILY_LIMIT_REACHED` | `{ "retryAfterSeconds": number }` — seconds until the same request can succeed |
| `VALIDATION_FAILED` from a request body | `{ "fields": [ { "field", "rule", "message" } ] }` |
| every other code | no `details` |

```json
"details": { "fields": [
  { "field": "newPassword", "rule": "Size",     "message": "Password must be at least 8 characters long" },
  { "field": "email",       "rule": "NotBlank", "message": "Email is required" }
] }
```

A field failing two rules appears twice. `message` in a field entry is English, like the top-level one.

### Validation rules in use

| Rule | Meaning | Where |
|---|---|---|
| `NotBlank` | required, not only whitespace | most fields |
| `Email` | not an email address | `email` |
| `Size` | too short or too long (the message says which) | `password`, `newPassword`, address fields |
| `Pattern` | wrong format | address `phone` |

`VALIDATION_FAILED` always means *the user's input* was wrong, and always has `fields`. A request
the client code built wrongly gets a different code, because there is no field to show a message on:

| Request | Answer |
|---|---|
| JSON body missing or unparseable | `400 MALFORMED_REQUEST` |
| Query parameter of the wrong type | `400 MALFORMED_REQUEST` (no endpoint has one yet) |
| Path id that cannot be parsed, such as `/api/addresses/not-a-uuid` | `404 NOT_FOUND` — it names nothing |
| No endpoint at the path | `404 NOT_FOUND` |
| Endpoint exists, not for this method | `405 METHOD_NOT_ALLOWED`, with the `Allow` header |
| Body is not `application/json` | `415 UNSUPPORTED_MEDIA_TYPE` |

On a protected path, a request without a token is `401 UNAUTHENTICATED` before any of these apply.

## Codes

| Code | Status | When | Frontend |
|---|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Login with a wrong email or password | "Wrong email or password" |
| `UNAUTHENTICATED` | 401 | A protected request with no token, an expired or malformed token, or for an account no longer allowed to sign in. Also the generic Google login failure. | Call `/api/auth/refresh` once; if that fails, go to login |
| `ACCOUNT_BLOCKED` | 403 | Login to a banned account. Also a blocked Google login. | "This account is blocked" |
| `ACCOUNT_NOT_VERIFIED` | 403 | Login before the email is verified | Go to the verify-email screen |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh with an expired refresh token | Go to login |
| `REFRESH_TOKEN_INVALID` | 401 | Refresh with a missing, unknown, reused or revoked token, or for an account no longer allowed | Go to login |
| `OTP_INVALID` | 400 | A code that is wrong, expired or never issued — deliberately one answer | "Code incorrect or expired", offer a new code |
| `OTP_ATTEMPTS_EXCEEDED` | 429 | Too many wrong guesses; the code is destroyed | Ask for a new code |
| `OTP_THROTTLED` | 429 | A new code requested within the resend interval (60 s by default) | Count down `details.retryAfterSeconds` |
| `OTP_DAILY_LIMIT_REACHED` | 429 | Password reset only: the daily number of codes for this email is used up (5 by default) | "Try again later"; `details.retryAfterSeconds` is the rest of the window |
| `USER_NOT_FOUND` | 404 | A user id that does not exist — `GET /api/users/{id}` today, admin user lookups later. Never from a public auth endpoint. | Depends on screen |
| `EMAIL_ALREADY_EXISTS` | 409 | Register with an email that already has an account | Offer login or password reset |
| `ADDRESS_NOT_FOUND` | 404 | An address that does not exist **or belongs to someone else** | Treat as gone |
| `ADDRESS_LIMIT_REACHED` | 409 | Adding an address when the customer already has 20 | "You can save up to 20 addresses" |
| `VALIDATION_FAILED` | 400 | Request failed validation — see above | Show per field from `details.fields[].rule` |
| `MALFORMED_REQUEST` | 400 | Unparseable JSON or a wrongly typed query parameter — a client bug | Generic error; report the bug |
| `NOT_FOUND` | 404 | No endpoint at the path, or a path id that cannot be parsed | Not-found screen |
| `METHOD_NOT_ALLOWED` | 405 | The path exists, but not for this HTTP method | Generic error; report the bug |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | The body is not `application/json` | Generic error; report the bug |
| `ACCESS_DENIED` | 403 | Signed in, but not allowed | "You don't have access" |
| `INTERNAL_ERROR` | 500 | Anything unexpected | Generic error |

### Answers that are the same on purpose

Some situations share one answer so the API cannot be used to find out which emails have accounts.
Do not ask the backend to split them:

- `POST /api/auth/forgot-password` is `204` for every email, registered or not.
- `POST /api/auth/verify-email` answers `OTP_INVALID` for an email with no account, as for a wrong code.
- `OTP_THROTTLED` and `OTP_DAILY_LIMIT_REACHED` apply to unregistered emails exactly as to registered ones.
- `EMAIL_ALREADY_EXISTS` is the same for active, suspended and banned accounts.

## Google login redirect

Google login is a browser redirect, not a JSON call. On failure the browser returns to
`OAUTH2_REDIRECT_URI` with `?error=` set to an `ErrorCode` name:

| `error` | When |
|---|---|
| `ACCOUNT_BLOCKED` | The matching account is banned |
| `UNAUTHENTICATED` | Any other failure (cancelled consent, unverified Google email, provider error) |

Indexed in [[Modules]].
