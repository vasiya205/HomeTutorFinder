# Deployment

## Option A - Docker Compose (one command, any machine or cloud VM)
Requirements: Docker Desktop (Windows/Mac) or Docker Engine (Linux).
```bash
# in the project root (uses JWT_SECRET, MAIL_*, RAZORPAY_* from .env)
docker compose up -d --build
```
| URL | What |
|---|---|
| http://localhost:3000 | Website (nginx serving the React build, proxying `/api` to the gateway) |
| http://localhost:8080 | API gateway |
| http://localhost:8761 | Eureka dashboard |
| `localhost:5433` | PostgreSQL in the container (user `postgres`, password `postgres`). Add it in pgAdmin to browse the data |

Stop it with `docker compose down`, or add `-v` to also delete the database volume.

**Deploy to a cloud VM** (AWS EC2, Azure VM, GCP, DigitalOcean, Oracle Free Tier):
install Docker, `git clone` the repository, create `.env`, set `PUBLIC_FRONTEND_URL=http://<server-ip>:3000`
(used in email links), run `docker compose up -d --build`, and open port 3000 in the firewall or security group.

## Option B - Render (managed cloud, Blueprint)
1. Push the project to GitHub (`.env` is git-ignored).
2. Render → **New → Blueprint** → select the repository. `render.yaml` creates a PostgreSQL database,
   5 Docker web services and the static frontend. All services share the one database, and each has its own tables and Flyway history table.
3. After the first deploy, open each service → **Environment** and fill in the values marked *sync: false*,
   using the public URLs Render shows (e.g. `https://htf-tutor.onrender.com`):

   | Service | Variable | Value |
   |---|---|---|
   | htf-gateway | `AUTH_SERVICE_URL`, `TUTOR_SERVICE_URL`, `BOOKING_SERVICE_URL`, `NOTIFICATION_SERVICE_URL` | the 4 service URLs |
   | htf-booking | `TUTOR_SERVICE_URL`, `NOTIFICATION_SERVICE_URL` | tutor / notification URLs |
   | htf-auth | `NOTIFICATION_SERVICE_URL` | notification URL |
   | htf-frontend | `VITE_API_BASE_URL` | gateway URL, then **Manual Deploy** to rebuild |
   | env group `htf-shared` | `FRONTEND_URL` | the frontend URL (for links in emails) |
   | htf-notification | `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail + app password (optional) |
   | htf-booking | `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | test keys (optional) |
4. Free instances sleep after 15 minutes without traffic. Open the site and the gateway health URL about 1 minute before a demo to wake them up.

## Option C - Frontend on Vercel / Netlify
`frontend/` is a standard Vite app. Build command `npm run build`, output `dist`, and environment
variable `VITE_API_BASE_URL=https://<your-gateway-url>`. `vercel.json` and `public/_redirects`
already handle page refreshes on deep links. Add the site URL to the gateway's `CORS_ALLOWED_ORIGINS`.

## Production checklist
- [ ] Strong random `JWT_SECRET` (same value for all services)
- [ ] `SEED_DEMO_DATA=false` once you go live with real users
- [ ] Gmail app password set, then confirm with **Admin → Send test email**
- [ ] `FRONTEND_URL` points to the public site, so email buttons work
- [ ] `CORS_ALLOWED_ORIGINS` contains the public site origin
