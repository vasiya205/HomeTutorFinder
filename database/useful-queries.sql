-- ===== hometutor_auth =====
-- All accounts (passwords are BCrypt hashes, never plain text)
SELECT id, full_name, email, role, enabled, LEFT(password_hash, 20) || '...' AS bcrypt_hash, created_at
FROM users ORDER BY id;

-- Make an existing account an administrator
-- UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';

-- ===== hometutor_tutor =====
SELECT id, user_id, full_name, subject, location, experience_years, hourly_rate, teaching_mode
FROM tutor_profiles ORDER BY id;

-- ===== hometutor_booking =====
-- Bookings with payment details
SELECT id, student_name, tutor_name, subject, start_date_time, total_amount, status,
       payment_status, payment_mode, payment_id, paid_at
FROM bookings ORDER BY created_at DESC;

-- Revenue and lesson counts per tutor
SELECT tutor_name, COUNT(*) AS lessons, SUM(total_amount) FILTER (WHERE payment_status = 'PAID') AS revenue
FROM bookings GROUP BY tutor_name ORDER BY revenue DESC NULLS LAST;

-- Average rating per tutor
SELECT tutor_user_id, ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS reviews
FROM reviews GROUP BY tutor_user_id ORDER BY avg_rating DESC;

-- ===== hometutor_notification =====
-- Every email the platform produced and whether it was delivered
SELECT id, type, recipient_email, subject, status, error_message, created_at, sent_at
FROM email_notifications ORDER BY created_at DESC;
