package com.example.authservice.seed;

import com.example.authservice.repository.UserRepository;
import com.example.common.demo.DemoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Inserts the demo admin, tutors and students (password Demo@123) into an empty users table. */
@Component
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoDataSeeder(UserRepository userRepository, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                          @Value("${app.seed-demo-data:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        String passwordHash = passwordEncoder.encode(DemoData.PASSWORD);
        for (DemoData.Account account : DemoData.accounts()) {
            // Fixed ids keep the demo data consistent with tutor-service and booking-service.
            jdbcTemplate.update("""
                    INSERT INTO users (id, full_name, email, password_hash, role, enabled, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, TRUE, NOW(), NOW())
                    """, account.id(), account.fullName(), account.email(), passwordHash, account.role());
        }
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users))");
        log.info("Seeded {} demo accounts. Sign in with e.g. {} / {}", DemoData.accounts().size(),
                DemoData.STUDENT.email(), DemoData.PASSWORD);
    }
}
