package com.lmt.fyp.flowerplus.module.user.web.dto;

import com.lmt.fyp.flowerplus.module.user.entity.Address;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String receiverName,
        String phone,
        String address,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getReceiverName(),
                address.getPhone(),
                address.getAddress(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
