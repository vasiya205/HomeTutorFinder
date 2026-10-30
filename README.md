# 🎓 Home Tutor Finder

A full-stack platform that connects students with home and online tutors. Students search, book,
pay and review tutors. Tutors manage requests, and admins oversee the platform. Every important
step sends an HTML email.

**React 19 + Vite** frontend · **Spring Boot 3 microservices** (Eureka, API Gateway, 4 domain services) · **PostgreSQL** · **JWT** · **Razorpay** · **SMTP email** · **Docker**

---

## ✅ Rubric coverage

| Rubric (15 marks each) | What is implemented | Where to see it |
|---|---|---|
| **1. Attractive frontend** | Responsive design system, landing page, tutor search with filters, profile pages, role dashboards, toasts, skeleton loaders and modals | `frontend/src` · http://localhost:5173 |
| **2. Database integration** | PostgreSQL with one database per microservice, **Flyway** migrations, constraints and indexes. Databases are created automatically and demo data is seeded | `database/README.md` · pgAdmin |
| **3. Sign-up & sign-in with JWT** | Register as a Student or Tutor, BCrypt hashing, HS256 JWT checked by every service, role-based access (STUDENT/TUTOR/ADMIN), forgot-password and reset by email, account disabling | `auth-service`, `common/security` |
| **4. Payment** | Razorpay test checkout with **server-side HMAC signature verification**. The amount is always calculated on the server. A clearly labelled demo checkout is used when no keys are set | `booking-service/.../PaymentService.java` |
| **5. Notification mail** | Dedicated **notification-service**: branded HTML emails over Gmail SMTP for welcome, password reset, booking request, accept/reject, cancellation, completion, payment receipt and payment received. Every email is logged in PostgreSQL and can be viewed in the app | `notification-service` · Inbox page · Admin → Email log |
| **6. Deployment** | Dockerfile per service, `docker-compose.yml` (one command), Render Blueprint (`render.yaml`), GitHub Actions CI, VS Code one-click tasks | `docs/DEPLOYMENT.md` |

---

## 🚀 Run it in VS Code (Windows)

### 1. Install once
- **JDK 21**: `winget install EclipseAdoptium.Temurin.21.JDK`
- **Node.js 20+**: https://nodejs.org
- **PostgreSQL + pgAdmin**: https://www.postgresql.org/download/windows/ (remember the password you set for `postgres`)
- Maven is **optional**. If it is not installed, the build script downloads it automatically.

### 2. Configure
Open the project folder in VS Code and edit **`.env`** in the root:
```properties
DB_PASSWORD=your_postgres_password
```
That is all you need. Email and Razorpay keys are optional (see below).

### 3. Start everything
**Terminal → Run Task… → `Start Full App`** (or press `Ctrl+Shift+B`).

This builds the backend and opens a terminal for each service:
`eureka-server :8761 · api-gateway :8080 · auth :8081 · tutor :8082 · booking :8083 · notification :8084 · frontend :5173`

On the first run the build downloads dependencies, which takes about 3–5 minutes. After that, startup takes about 40 seconds.

Open **http://localhost:5173** and click a demo account on the sign-in page:

| Role | Email | Password |
|---|---|---|
| Student | `student@hometutor.demo` | `Demo@123` |
| Tutor | `priya@hometutor.demo` | `Demo@123` |
| Admin | `admin@hometutor.demo` | `Demo@123` |

> Run the task **`Check setup & health`** at any time to see which services are up.
> Without VS Code tasks, run `powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1`.

### 4. (Optional) Real emails with Gmail
1. Turn on 2-Step Verification for your Google account.
2. Create an **App Password**: https://myaccount.google.com/apppasswords
3. Add the following to `.env` and restart notification-service:
   ```properties
   MAIL_USERNAME=yourname@gmail.com
   MAIL_PASSWORD=abcd efgh ijkl mnop
   ```
4. Sign in as admin → **Email log** → **Send test**. You can also register with your own email to receive the welcome email.

Without SMTP, every email is still rendered, saved to PostgreSQL and shown in the **Inbox** page. Its status shows as `Logged`.
Demo `@hometutor.demo` addresses are never sent to a real mail server.

### 5. (Optional) Real Razorpay test checkout
Create free test keys at https://dashboard.razorpay.com (Test Mode → API Keys) and add them to `.env`:
```properties
RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxx
```
Restart booking-service. Test card: `4111 1111 1111 1111`, any future expiry, any CVV. UPI test ID: `success@razorpay`.

---

## 🧭 Demo flow for judges (5 minutes)
1. **Home** → search "Mathematics" → open **Priya Sharma**, then look at the ratings, reviews and recommended books.
2. Sign in as **Student** → request a lesson → a toast confirms it and emails go to the tutor and the student.
3. **Dashboard → Needs action** → **Pay** the accepted Programming lesson → checkout → receipt email.
4. **Inbox** → see the rendered emails (welcome, request, accepted, receipt).
5. Sign out → **Tutor (Priya)** → accept Karthik's pending request → the student is emailed.
6. Sign out → **Admin** → overview stats, disable/enable a user, view the **Email log**, then **Send test** email.
7. Open **pgAdmin** → `hometutor_booking.bookings` → the payment id and `PAID` status are stored.

---

## 🏗️ Architecture

```
React (Vite, :5173) ──/api──► API Gateway (:8080) ──► auth-service (:8081)          ─► hometutor_auth
                                   │                ├► tutor-service (:8082)         ─► hometutor_tutor
                                   │                ├► booking-service (:8083) ──────► hometutor_booking
                                   │                │      │  └─► Razorpay API
                                   │                └► notification-service (:8084) ─► hometutor_notification
                                   │                       └─► Gmail SMTP
                            Eureka registry (:8761)
```
- **common** module: JWT service and filter, JSON error handling, DB auto-creation, notification client, demo data.
- Services trust the same `JWT_SECRET`. Service-to-service calls use short-lived **service tokens** with role `SERVICE`.
- Emails are sent **asynchronously**, so a slow mail server never blocks a booking or payment.

More detail: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · API reference: [docs/API.md](docs/API.md) · Deployment: [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) · Tests: [docs/TESTING.md](docs/TESTING.md)

## 📁 Project structure
```
backend/            Maven multi-module Spring Boot project
  common/           shared security, errors, DB bootstrap, notification client
  eureka-server/    api-gateway/   auth-service/   tutor-service/   booking-service/   notification-service/
frontend/           React + Vite app (pages, components, design system in src/styles/index.css)
database/           pgAdmin guide, useful queries, reset script
docs/               architecture, API, deployment, testing
postman/            Postman collection for every endpoint
scripts/            build/run helpers used by the VS Code tasks
.vscode/            one-click tasks + debug configurations
docker-compose.yml  render.yaml  .github/workflows/ci.yml
```

## 🔐 Security highlights
- Passwords are hashed with BCrypt. Sign-in failures return the same "Invalid email or password" message whether or not the email exists.
- JWTs are stateless and include `role`. `@PreAuthorize` checks run on every protected endpoint.
- Public sign-up cannot create admins. The last admin cannot be disabled.
- Password reset tokens are random, single-use and expire in 30 minutes. Only their SHA-256 hash is stored.
- Payments are verified with a Razorpay HMAC-SHA256 signature on the server, and only `rzp_test_` keys are accepted.
- Secrets live only in `.env`, which is git-ignored.

## 🛠 Troubleshooting
| Problem | Fix |
|---|---|
| `password authentication failed for user "postgres"` | Set the right `DB_PASSWORD` in `.env` |
| `Connection refused localhost:5432` | Start PostgreSQL (Windows Services → `postgresql-x64-…` → Start) |
| `Port 8080 already in use` | Close the other program, or stop old terminals (Terminal → Kill All Terminals) |
| "Cannot reach the server" in the browser | Backend is still starting. Wait for `Started ...Application` in every terminal |
| `java` not found / wrong version | Install JDK 21 and reopen VS Code |
| `npm.ps1 cannot be loaded` | The tasks use `npm.cmd`. In your own terminal, run `npm.cmd run dev` |
