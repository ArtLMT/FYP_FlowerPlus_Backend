package com.lmt.fyp.flowerplus.module.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    private static Address address(boolean isDefault) {
        return Address.builder()
                .receiverName("Alice")
                .phone("0912345678")
                .address("1 Flower St")
                .isDefault(isDefault)
                .build();
    }

    @Test
    @DisplayName("editing the default address keeps it the default")
    void editKeepsDefault() {
        Address address = address(true);

        address.edit("Bob", "0987654321", "2 Rose Ave");

        assertThat(address.isDefault()).isTrue();
        assertThat(address.getReceiverName()).isEqualTo("Bob");
        assertThat(address.getPhone()).isEqualTo("0987654321");
        assertThat(address.getAddress()).isEqualTo("2 Rose Ave");
    }

    @Test
    @DisplayName("markDefault makes the address the default")
    void markDefault() {
        Address address = address(false);

        address.markDefault();

        assertThat(address.isDefault()).isTrue();
    }
}
