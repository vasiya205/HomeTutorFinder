-- Run in pgAdmin Query Tool connected to the "postgres" database, with all services STOPPED.
-- Deletes all Home Tutor Finder data; the next start recreates the databases and demo data.
DROP DATABASE IF EXISTS hometutor_auth WITH (FORCE);
DROP DATABASE IF EXISTS hometutor_tutor WITH (FORCE);
DROP DATABASE IF EXISTS hometutor_booking WITH (FORCE);
DROP DATABASE IF EXISTS hometutor_notification WITH (FORCE);
