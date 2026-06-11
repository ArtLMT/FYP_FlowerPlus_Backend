package com.lmt.fyp.flowerplus.repository;

import com.lmt.fyp.flowerplus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by UserDetailsService to load a user by their email address. */
    Optional<User> findByEmail(String email);
}
