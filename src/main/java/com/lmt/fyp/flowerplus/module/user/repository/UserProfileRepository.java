package com.lmt.fyp.flowerplus.module.user.repository;

import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUser(User user);

    /** Batch-load profiles for a page of users, so a list never fires one query per row. */
    List<UserProfile> findByUserIn(Collection<User> users);
}
