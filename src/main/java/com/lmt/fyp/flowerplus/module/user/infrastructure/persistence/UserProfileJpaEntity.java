package com.lmt.fyp.flowerplus.module.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * PERSISTENCE ENTITY — OUTSIDE the wall (infrastructure).
 *
 * JPA mapping to the user_profile table. The FK relationship points at
 * {@link UserJpaEntity} (the persistence type), never at the domain model.
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    private String avatar;
}
