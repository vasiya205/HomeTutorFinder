package com.example.tutorservice.seed;

import com.example.common.demo.DemoData;
import com.example.tutorservice.domain.TeachingMode;
import com.example.tutorservice.domain.TutorProfileEntity;
import com.example.tutorservice.repository.TutorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adds the six demo tutor profiles to an empty tutor_profiles table. */
@Component
public class TutorDemoSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TutorDemoSeeder.class);

    private final TutorProfileRepository repository;
    private final boolean enabled;

    public TutorDemoSeeder(TutorProfileRepository repository, @Value("${app.seed-demo-data:true}") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || repository.count() > 0) {
            return;
        }
        for (DemoData.TutorProfile demo : DemoData.TUTORS) {
            TutorProfileEntity profile = new TutorProfileEntity();
            profile.setUserId(demo.userId());
            profile.setFullName(demo.fullName());
            profile.setEmail(demo.email());
            profile.setSubject(demo.subject());
            profile.setQualification(demo.qualification());
            profile.setLocation(demo.location());
            profile.setExperienceYears(demo.experienceYears());
            profile.setHourlyRate(demo.hourlyRate());
            profile.setTeachingMode(TeachingMode.valueOf(demo.teachingMode()));
            profile.setBio(demo.bio());
            repository.save(profile);
        }
        log.info("Seeded {} demo tutor profiles", DemoData.TUTORS.size());
    }
}
