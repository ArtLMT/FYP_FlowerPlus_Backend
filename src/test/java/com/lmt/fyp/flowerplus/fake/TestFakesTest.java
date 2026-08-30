package com.lmt.fyp.flowerplus.fake;

import com.lmt.fyp.flowerplus.module.auth.service.OtpStore;
import com.lmt.fyp.flowerplus.module.email.service.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestFakesConfig.class)
class TestFakesTest {

    @Autowired
    private OtpStore otpStore;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private InMemoryOtpStore inMemoryOtpStore;

    @Autowired
    private NoOpEmailSender noOpEmailSender;

    @Test
    void testCanInjectBothFakes() {
        assertThat(otpStore).isNotNull().isInstanceOf(InMemoryOtpStore.class);
        assertThat(emailSender).isNotNull().isInstanceOf(NoOpEmailSender.class);
        assertThat(inMemoryOtpStore).isSameAs(otpStore);
        assertThat(noOpEmailSender).isSameAs(emailSender);
    }
}
