# Architecture

## Services

| Service | Port | Responsibility | Database |
|---|---|---|---|
| eureka-server | 8761 | Service registry (dashboard at http://localhost:8761) | - |
| api-gateway | 8080 | Single entry point, routing, CORS (Spring Cloud Gateway) | - |
| auth-service | 8081 | Sign-up, sign-in, JWT, password reset, admin user management | `hometutor_auth` |
| tutor-service | 8082 | Tutor profiles, search, subjects | `hometutor_tutor` |
| booking-service | 8083 | Bookings workflow, payments (Razorpay), reviews, admin stats | `hometutor_booking` |
| notification-service | 8084 | Renders and sends HTML emails, stores the email log | `hometutor_notification` |
| common (library) | - | JWT, error handling, DB auto-create, notification client, demo data | - |

## Request flow: booking a lesson

```
Browser ──POST /api/bookings (Bearer JWT)──► Gateway ──► booking-service
   booking-service: verify JWT → role STUDENT → validate time (future, 30 min-4 h, no overlap)
   booking-service ──GET /api/tutors/internal/user/{id} (service JWT)──► tutor-service  (rate, name, email)
   booking-service: price = hourly rate × duration (calculated on the server) → INSERT bookings (PENDING)
   booking-service ──POST /api/notifications/email (service JWT, async)──► notification-service
   notification-service: render HTML → INSERT email_notifications → send via SMTP (@Async) → SENT / LOGGED / FAILED
```

## Booking state machine

```
PENDING ──tutor accepts──► ACCEPTED ──student pays──► (PAID) ──tutor completes──► COMPLETED ──student reviews
   │                          │
   ├─tutor rejects─► REJECTED  └─student/tutor cancels (only while unpaid)─► CANCELLED
   └─student cancels─► CANCELLED              (admin can cancel any PENDING/ACCEPTED booking)
```
The rules live in `BookingService.validateTransition` and are unit tested in `BookingRulesTest`.

## Payment flow (Razorpay)
1. `POST /api/bookings/{id}/payments/order` creates a Razorpay order for the amount stored on the server.
2. The browser opens Razorpay Checkout with `keyId` and `orderId`.
3. Razorpay returns `payment_id` and `signature`. `POST .../payments/verify` checks
   `HMAC_SHA256(order_id + "|" + payment_id, key_secret) == signature`.
4. Only then is the booking marked `PAID` and the receipt emails sent.

Without test keys, `PAYMENT_MODE=AUTO` falls back to **DEMO** mode. It uses the same order/verify flow
with a simulated checkout, and the UI labels it clearly.

## Security
- **JWT (HS256)**: claims `sub` (user id), `email`, `fullName`, `role`, `iss`, `exp` (10 h). The signing key is SHA-256(JWT_SECRET).
- Every service validates the token itself (`common/security/JwtAuthenticationFilter`), so the gateway stays a thin router.
- auth-service also checks on every request that the account is still enabled.
- Service-to-service calls use 5-minute tokens with role `SERVICE`. Internal endpoints (`/api/tutors/internal/**`, `POST /api/notifications/email`) require that role.
- Errors are returned as consistent JSON: `{"status":400,"message":"...","fieldErrors":{...}}` (`common/web/GlobalExceptionHandler`).

## Data ownership
Each service owns its own database (*database per service*). Names and emails are copied into
bookings when they are created, so lists and emails do not need cross-service calls. Ids are
references, not foreign keys.
