package com.lmt.fyp.flowerplus.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountStatusTest {

    @Test
    @DisplayName("BANNED and PENDING cannot authenticate; SUSPENDED still can")
    void canAuthenticate() {
        assertThat(UserAccountStatus.ACTIVE.canAuthenticate()).isTrue();
        assertThat(UserAccountStatus.SUSPENDED.canAuthenticate()).isTrue();
        assertThat(UserAccountStatus.PENDING.canAuthenticate()).isFalse();
        assertThat(UserAccountStatus.BANNED.canAuthenticate()).isFalse();
    }

    @Test
    @DisplayName("only ACTIVE can place an order")
    void canPlaceOrder() {
        assertThat(UserAccountStatus.ACTIVE.canPlaceOrder()).isTrue();
        assertThat(UserAccountStatus.SUSPENDED.canPlaceOrder()).isFalse();
        assertThat(UserAccountStatus.PENDING.canPlaceOrder()).isFalse();
        assertThat(UserAccountStatus.BANNED.canPlaceOrder()).isFalse();
    }
}
