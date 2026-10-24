package com.trustvault.repository;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<AppUser> findByRole(Role role);

    List<AppUser> findAllByOrderByCreatedAtDesc();
}
