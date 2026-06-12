package com.lmt.fyp.flowerplus.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeleteEntity extends TimestampEntity {

    @Column(nullable = false)
    private boolean isDeleted = false;

    private Instant deletedAt;
}
