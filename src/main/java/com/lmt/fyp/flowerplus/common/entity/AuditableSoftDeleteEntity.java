package com.lmt.fyp.flowerplus.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class AuditableSoftDeleteEntity extends AuditableEntity {

    @Column(nullable = false)
    private boolean isDeleted = false;

    private Instant deletedAt;

    private UUID deletedBy;
}
