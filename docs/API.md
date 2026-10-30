# API reference

All requests go through the gateway: `http://localhost:8080`. Protected endpoints need
`Authorization: Bearer <token>`. Errors look like `{"status":409,"message":"..."}`.
A ready-made Postman collection is in `postman/`.

## Auth - `auth-service`
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | `{fullName, email, password, role: STUDENT\|TUTOR}` → `{token, user}` (201) |
| POST | `/api/auth/login` | public | `{email, password}` → `{token, tokenType, expiresInSeconds, user}` |
| GET | `/api/auth/me` | any signed-in user | Current user |
| POST | `/api/auth/forgot-password` | public | `{email}` → emails a reset link |
| POST | `/api/auth/reset-password` | public | `{token, newPassword}` |
| GET | `/api/auth/admin/users` | ADMIN | All users |
| PATCH | `/api/auth/admin/users/{id}/enabled` | ADMIN | `{enabled: true\|false}` |

## Tutors - `tutor-service`
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/tutors/search?q=&subject=&location=&minRate=&maxRate=&sort=` | public | `sort` = experience \| rate_asc \| rate_desc \| newest |
| GET | `/api/tutors/subjects` | public | Distinct subjects |
| GET | `/api/tutors/{id}` | public | Profile by profile id |
| GET | `/api/tutors/user/{userId}` | public | Profile by user id |
| GET | `/api/tutors/me` | TUTOR | Own profile (204 if not created) |
| PUT | `/api/tutors/me` | TUTOR | Create/update: `{qualification, subject, location, experienceYears, hourlyRate, teachingMode: HOME\|ONLINE\|BOTH, bio}` |
| GET | `/api/tutors/internal/user/{userId}` | SERVICE | Includes email (used by booking-service) |

## Bookings, payments, reviews - `booking-service`
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/bookings` | STUDENT | `{tutorUserId, subject, startDateTime, endDateTime, notes}` (local ISO date-times) |
| GET | `/api/bookings/my` | STUDENT | Own bookings |
| GET | `/api/bookings/incoming` | TUTOR | Requests for the tutor |
| PATCH | `/api/bookings/{id}/status` | participant | `{status: ACCEPTED\|REJECTED\|COMPLETED\|CANCELLED}` |
| POST | `/api/bookings/{id}/payments/order` | STUDENT | → `{mode, keyId, orderId, amountInPaise, ...}` |
| POST | `/api/bookings/{id}/payments/verify` | STUDENT | `{orderId, paymentId, signature}` → `{paymentStatus, paymentId, booking}` |
| POST | `/api/reviews` | STUDENT | `{bookingId, rating 1-5, comment}` (completed lessons only, one per booking) |
| GET | `/api/reviews/tutor/{tutorUserId}` | public | Reviews for a tutor |
| GET | `/api/reviews/summary` | public | `[{tutorUserId, averageRating, reviewCount}]` |
| GET | `/api/admin/bookings` | ADMIN | All bookings |
| GET | `/api/admin/bookings/stats` | ADMIN | Counts + revenue |
| PATCH | `/api/admin/bookings/{id}/cancel` | ADMIN | Cancel an active booking |

## Notifications - `notification-service`
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/notifications/email` | SERVICE | Queue an email `{type, recipientEmail, recipientName, data}` |
| GET | `/api/notifications/my` | signed-in | Emails sent to me (with HTML) |
| GET | `/api/notifications/{id}` | owner / ADMIN | One email |
| GET | `/api/notifications/admin` | ADMIN | Email log (latest 200) |
| GET | `/api/notifications/admin/status` | ADMIN | SMTP configured? counts |
| POST | `/api/notifications/admin/test` | ADMIN | `{to}` sends a test email |

Health: `GET /actuator/health` on every service.
