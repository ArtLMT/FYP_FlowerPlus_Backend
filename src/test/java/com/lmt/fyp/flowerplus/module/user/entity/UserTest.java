package com.lmt.fyp.flowerplus.module.user.entity;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
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

    @Test
    @DisplayName("adopting a PENDING account makes it an ACTIVE STAFF with a new password")
    void adoptAsStaff() {
        User user = User.builder()
                .status(UserAccountStatus.PENDING)
                .role(UserRole.CUSTOMER)
                .password("old-hash")
                .build();

        user.adoptAsStaff("new-hash");

        assertThat(user.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.STAFF);
        assertThat(user.getPassword()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("only a PENDING account can be adopted as staff")
    void nonPendingCannotBeAdopted() {
        for (UserAccountStatus status : List.of(
                UserAccountStatus.ACTIVE, UserAccountStatus.SUSPENDED, UserAccountStatus.BANNED)) {
            User user = User.builder().status(status).role(UserRole.CUSTOMER).password("hash").build();

            assertThatThrownBy(() -> user.adoptAsStaff("new")).isInstanceOf(IllegalStateException.class);
            assertThat(user.getStatus()).isEqualTo(status);
            assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
        }
    }

    @Test
    @DisplayName("ban deactivates an ACTIVE account and unban reactivates it")
    void banThenUnban() {
        User user = withStatus(UserAccountStatus.ACTIVE);

        user.ban();
        assertThat(user.getStatus()).isEqualTo(UserAccountStatus.BANNED);

        user.unban();
        assertThat(user.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("only an ACTIVE account can be banned")
    void onlyActiveCanBeBanned() {
        for (UserAccountStatus status : List.of(
                UserAccountStatus.PENDING, UserAccountStatus.SUSPENDED, UserAccountStatus.BANNED)) {
            User user = withStatus(status);

            assertThatThrownBy(user::ban).isInstanceOf(IllegalStateException.class);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("unbanning anything but a BANNED account is refused")
    void onlyBannedCanBeUnbanned() {
        for (UserAccountStatus status : List.of(
                UserAccountStatus.ACTIVE, UserAccountStatus.PENDING, UserAccountStatus.SUSPENDED)) {
            User user = withStatus(status);

            assertThatThrownBy(user::unban).isInstanceOf(IllegalStateException.class);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }
}
