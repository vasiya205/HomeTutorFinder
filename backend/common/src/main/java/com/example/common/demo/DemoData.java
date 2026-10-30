package com.example.common.demo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo accounts shared by all services so the seeded data lines up across the separate
 * databases (auth user id 2 is the same person as tutor profile user_id 2, and so on).
 * Every demo account uses the password {@link #PASSWORD}. Seeding only happens on an empty database.
 */
public final class DemoData {
    public static final String PASSWORD = "Demo@123";
    public static final String EMAIL_DOMAIN = "@hometutor.demo";

    public record Account(long id, String fullName, String email, String role) {}

    public record TutorProfile(long userId, String fullName, String email, String subject, String qualification,
                               String location, int experienceYears, BigDecimal hourlyRate, String teachingMode,
                               String bio) {}

    public static final Account ADMIN = new Account(1, "Platform Admin", "admin" + EMAIL_DOMAIN, "ADMIN");
    public static final Account STUDENT = new Account(8, "Sneha Patel", "student" + EMAIL_DOMAIN, "STUDENT");
    public static final Account STUDENT_TWO = new Account(9, "Karthik Rao", "karthik" + EMAIL_DOMAIN, "STUDENT");

    public static final List<TutorProfile> TUTORS = List.of(
            new TutorProfile(2, "Priya Sharma", "priya" + EMAIL_DOMAIN, "Mathematics",
                    "M.Sc. Mathematics, IIT Madras", "Hyderabad", 8, new BigDecimal("800.00"), "BOTH",
                    "I help Class 9-12 and JEE students build strong fundamentals in algebra, calculus and coordinate "
                            + "geometry. Every session ends with a short practice set and a progress note for parents."),
            new TutorProfile(3, "Arjun Reddy", "arjun" + EMAIL_DOMAIN, "Physics",
                    "B.Tech + M.Tech, NIT Warangal", "Bengaluru", 6, new BigDecimal("900.00"), "ONLINE",
                    "Physics made visual: I use simulations and real-life examples to explain mechanics, "
                            + "electricity and modern physics for CBSE, ICSE and JEE aspirants."),
            new TutorProfile(4, "Ananya Iyer", "ananya" + EMAIL_DOMAIN, "English",
                    "M.A. English Literature, CELTA certified", "Chennai", 10, new BigDecimal("650.00"), "BOTH",
                    "Spoken English, grammar, essay writing and IELTS preparation. I focus on confidence and fluency "
                            + "with plenty of conversation practice."),
            new TutorProfile(5, "Rahul Verma", "rahul" + EMAIL_DOMAIN, "Programming",
                    "B.Tech CSE, 5 years as a software engineer", "Hyderabad", 5, new BigDecimal("1000.00"), "ONLINE",
                    "Python, Java and web development taught project-first. Ideal for school students starting to code "
                            + "and college students preparing for placements."),
            new TutorProfile(6, "Fatima Khan", "fatima" + EMAIL_DOMAIN, "Chemistry",
                    "M.Sc. Chemistry, University of Mumbai", "Mumbai", 7, new BigDecimal("750.00"), "HOME",
                    "Organic, inorganic and physical chemistry for Class 11-12 and NEET. Clear notes, reaction maps and "
                            + "weekly tests."),
            new TutorProfile(7, "Vikram Singh", "vikram" + EMAIL_DOMAIN, "Biology",
                    "MBBS (pursuing PG), AIIMS Delhi", "Delhi", 4, new BigDecimal("850.00"), "BOTH",
                    "NEET-focused biology with diagrams, mnemonics and previous-year question practice. "
                            + "Human physiology and genetics are my favourite chapters.")
    );

    private DemoData() {}

    public static List<Account> accounts() {
        List<Account> tutorAccounts = TUTORS.stream()
                .map(tutor -> new Account(tutor.userId(), tutor.fullName(), tutor.email(), "TUTOR"))
                .toList();
        return java.util.stream.Stream.of(List.of(ADMIN), tutorAccounts, List.of(STUDENT, STUDENT_TWO))
                .flatMap(List::stream)
                .toList();
    }

    public static boolean isDemoEmail(String email) {
        return email != null && email.toLowerCase().endsWith(EMAIL_DOMAIN);
    }
}
