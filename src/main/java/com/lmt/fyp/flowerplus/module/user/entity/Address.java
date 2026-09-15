package com.lmt.fyp.flowerplus.module.user.entity;

import com.lmt.fyp.flowerplus.common.entity.TimestampEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends TimestampEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "receiver_name", nullable = false, length = 255)
    private String receiverName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "address", nullable = false, columnDefinition = "text")
    private String address;

    /** At most one per user, enforced by a partial unique index in V5. */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    /** Leaves isDefault alone on purpose: editing must never clear the default. */
    public void edit(String receiverName, String phone, String address) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.address = address;
    }

    /** The caller must clear the previous default first (V5 index); see AddressService. */
    public void markDefault() {
        this.isDefault = true;
    }
}
