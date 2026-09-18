package com.lmt.fyp.flowerplus.module.user.repository;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByRole(UserRole role);

    /** Role-scoped lookup: a customer's or the Admin's id must resolve to empty,
     *  so the Staff admin endpoints answer 404 for a non-Staff target. */
    Optional<User> findByIdAndRole(UUID id, UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByRoleAndStatus(UserRole role, UserAccountStatus status, Pageable pageable);
}
