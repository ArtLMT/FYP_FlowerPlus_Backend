package com.lmt.fyp.flowerplus.module.user.repository;

import com.lmt.fyp.flowerplus.module.user.entity.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    Page<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId, Pageable pageable);

    /** Scoped by owner deliberately: somebody else's id must 404, not 403. */
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserId(UUID userId);

    long countByUserId(UUID userId);

    Optional<Address> findFirstByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Both flags matter: a bulk update bypasses the persistence context. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
    void clearDefaultFor(@Param("userId") UUID userId);
}
