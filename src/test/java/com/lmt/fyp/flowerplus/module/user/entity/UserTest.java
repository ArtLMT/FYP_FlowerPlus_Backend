package com.lmt.fyp.flowerplus.module.user.entity;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static User withStatus(UserAccountStatus status) {
        return User.builder().status(status).build();
    }

    @Test
    @DisplayName("a PENDING account activates")
    void pendingActivates() {
        User user = withStatus(UserAccountStatus.PENDING);

        user.activate();

        assertThat(user.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("only PENDING can be activated, so a BANNED account is never un-banned")
    void nonPendingCannotActivate() {
        for (UserAccountStatus status : List.of(
                UserAccountStatus.ACTIVE, UserAccountStatus.SUSPENDED, UserAccountStatus.BANNED)) {
            User user = withStatus(status);

            assertThatThrownBy(user::activate).isInstanceOf(IllegalStateException.class);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }
}
