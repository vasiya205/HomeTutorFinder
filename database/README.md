# Database (PostgreSQL + pgAdmin)

You do **not** need to create anything by hand. When each service starts it:

1. Creates its own database if it is missing (`DB_AUTO_CREATE=true`).
2. Creates or updates its tables with **Flyway** migrations (`backend/<service>/src/main/resources/db/migration`).
3. Adds demo data to empty tables (`SEED_DEMO_DATA=true`).

| Service | Database | Tables |
|---|---|---|
| auth-service | `hometutor_auth` | `users`, `password_reset_tokens` |
| tutor-service | `hometutor_tutor` | `tutor_profiles` |
| booking-service | `hometutor_booking` | `bookings`, `reviews` |
| notification-service | `hometutor_notification` | `email_notifications` |

Each microservice owns its own database (the *database-per-service* pattern), so there are no
foreign keys between databases. Ids such as `bookings.tutor_user_id` refer to records owned by
another service, and the services talk to each other over REST.

## Viewing the data in pgAdmin

1. Open pgAdmin and connect to your local server (`localhost`, port `5432`, user `postgres`).
2. Start the app once, then right-click **Databases** and choose **Refresh**.
3. Open `hometutor_booking` > **Schemas** > **public** > **Tables**, then right-click `bookings` > **View/Edit Data** > **All Rows**.
4. Handy queries for a demo are in [`useful-queries.sql`](useful-queries.sql). Open the **Query Tool** on the matching database to run them.

## Demo accounts (password `Demo@123`)

| Role | Email |
|---|---|
| Admin | `admin@hometutor.demo` |
| Student | `student@hometutor.demo`, `karthik@hometutor.demo` |
| Tutor | `priya@`, `arjun@`, `ananya@`, `rahul@`, `fatima@`, `vikram@hometutor.demo` |

## Start again with a clean database

Stop all services, then run [`reset-databases.sql`](reset-databases.sql) in pgAdmin's Query Tool,
connected to the `postgres` database. The next start recreates everything, demo data included.

## Manual setup (only if your PostgreSQL user cannot create databases)

Set `DB_AUTO_CREATE=false` in `.env` and create the four databases above in pgAdmin. Flyway still
creates the tables.
