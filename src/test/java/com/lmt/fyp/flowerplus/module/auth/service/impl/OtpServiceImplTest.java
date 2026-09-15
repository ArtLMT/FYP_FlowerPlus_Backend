package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.config.OtpProperties;
import com.lmt.fyp.flowerplus.exception.ApiException;
import com.lmt.fyp.flowerplus.fake.InMemoryOtpStore;
import com.lmt.fyp.flowerplus.module.auth.event.OtpRequestedEvent;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpDailyLimitReachedException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpThrottledException;
import com.lmt.fyp.flowerplus.module.auth.service.OtpHasher;
import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test over the in-memory store — no Spring, no Redis. */
class OtpServiceImplTest {

    private static final String EMAIL = "someone@example.com";

    private final InMemoryOtpStore store = new InMemoryOtpStore();
    private final List<Object> published = new ArrayList<>();

    private OtpServiceImpl service(Duration resendInterval, int resetDailyLimit) {
        OtpProperties properties = new OtpProperties(
                Duration.ofMinutes(5), 5, resendInterval, resetDailyLimit, "test-only-secret");
        return new OtpServiceImpl(store, new OtpHasher(properties), published::add, properties);
    }

    @Test
    @DisplayName("a second reset request inside the resend interval is refused, with the wait left")
    void resendIntervalApplies() {
        OtpServiceImpl service = service(Duration.ofSeconds(60), 5);

        service.throttle(OtpPurpose.PASSWORD_RESET, EMAIL);

        assertThatThrownBy(() -> service.throttle(OtpPurpose.PASSWORD_RESET, EMAIL))
                .isExactlyInstanceOf(OtpThrottledException.class)
                .satisfies(e -> assertThat(((OtpThrottledException) e).getRetryAfter())
                        .isPositive()
                        .isLessThanOrEqualTo(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("past the daily reset limit the refusal is its own kind, with the rest of the day to wait")
    void resetCodesAreCappedPerDay() {
        OtpServiceImpl service = service(Duration.ZERO, 3);

        for (int i = 0; i < 3; i++) {
            service.throttle(OtpPurpose.PASSWORD_RESET, EMAIL);
        }

        assertThatThrownBy(() -> service.throttle(OtpPurpose.PASSWORD_RESET, EMAIL))
                .isInstanceOfSatisfying(OtpDailyLimitReachedException.class,
                        e -> assertThat(e.getRetryAfter())
                                .isGreaterThan(Duration.ofHours(23))
                                .isLessThanOrEqualTo(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("registration codes have no daily cap")
    void registrationIsNotCapped() {
        OtpServiceImpl service = service(Duration.ZERO, 1);

        for (int i = 0; i < 3; i++) {
            service.throttle(OtpPurpose.REGISTRATION, EMAIL);
        }
    }

    @Test
    @DisplayName("an issued code is published with its purpose and stored under it")
    void issuedCodeCarriesItsPurpose() {
        service(Duration.ofSeconds(60), 5).issueOTP(OtpPurpose.PASSWORD_RESET, EMAIL);

        assertThat(published).singleElement()
                .isInstanceOfSatisfying(OtpRequestedEvent.class,
                        event -> assertThat(event.purpose()).isEqualTo(OtpPurpose.PASSWORD_RESET));
        assertThat(store.findHash(OtpPurpose.PASSWORD_RESET, EMAIL)).isPresent();
        assertThat(store.findHash(OtpPurpose.REGISTRATION, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("a reset code does not verify a registration")
    void resetCodeCannotVerifyRegistration() {
        OtpServiceImpl service = service(Duration.ofSeconds(60), 5);
        service.issueOTP(OtpPurpose.PASSWORD_RESET, EMAIL);
        String code = ((OtpRequestedEvent) published.get(0)).otp();

        assertThatThrownBy(() -> service.verify(OtpPurpose.REGISTRATION, EMAIL, code))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_INVALID));
    }
}
