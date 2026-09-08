package com.lmt.fyp.flowerplus.module.auth.repository;

import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Same lookup, but eager-loads the owner. The refresh path reads the user's
     * status and identity after the rotation transaction has closed, so the
     * association must already be initialised — a plain findByToken would throw
     * LazyInitializationException there.
     */
    @Query("select rt from RefreshToken rt join fetch rt.user where rt.token = :token")
    Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

    void deleteByUser(User user);

    void deleteByExpiryDateBefore(Instant expiryDate);

    /** Sweeps rotated / logged-out tombstones. Paired with the expiry sweep. */
    @Modifying
    @Query("delete from RefreshToken rt where rt.isRevoked = true")
    void deleteRevoked();
}
