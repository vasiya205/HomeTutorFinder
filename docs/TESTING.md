# Testing

## Automated
```powershell
# VS Code: Terminal -> Run Task -> "Run backend tests"
powershell -ExecutionPolicy Bypass -File scripts\test-backend.ps1
```
| Test | Covers |
|---|---|
| `BookingRulesTest` | Price calculation, time validation (future, 30 min-4 h), full status workflow (who can accept, pay-before-complete, paid lessons cannot be cancelled, strangers blocked) |
| `RazorpaySignatureVerifierTest` | HMAC-SHA256 signature verification (valid / tampered order / wrong secret) |
| `EmailTemplateRendererTest` | Every email type renders, HTML escaping of user input (XSS), plain-text alternative |

GitHub Actions (`.github/workflows/ci.yml`) runs the backend tests, the frontend build and the Docker image builds on every push.

## Manual end-to-end checklist
| # | Step | Expected |
|---|---|---|
| 1 | Register a new student with your real email | Signed in, welcome email in Inbox (and Gmail if SMTP is set) |
| 2 | Register with the same email again | "An account with this email already exists" |
| 3 | Sign in with a wrong password | "Invalid email or password." |
| 4 | Call `GET /api/bookings/my` without a token (Postman) | 401 JSON |
| 5 | Sign in as a student, then call `GET /api/auth/admin/users` with that token | 403 JSON |
| 6 | Book a lesson in the past / for 10 minutes | Validation message |
| 7 | Book Priya at a time she is already booked | "The tutor is already booked at that time" |
| 8 | Tutor accepts → student pays → tutor completes → student reviews | Status changes, 2 emails per step, rating appears on the tutor card |
| 9 | Admin disables a user → that user calls `/api/auth/me` | 401, sign-in shows "account has been disabled" |
| 10 | Forgot password → open the link from the Inbox → set a new password | Can sign in with the new password, link cannot be reused |
| 11 | pgAdmin: `SELECT * FROM bookings` | Payment id, `PAID`, `paid_at` stored |
