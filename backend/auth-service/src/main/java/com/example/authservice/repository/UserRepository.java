package com.example.authservice.repository;

import com.example.authservice.domain.Role;
import com.example.authservice.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndEnabledTrue(Role role);

    List<UserEntity> findAllByOrderByCreatedAtDesc();
}
