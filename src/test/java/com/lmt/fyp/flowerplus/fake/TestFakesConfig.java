package com.lmt.fyp.flowerplus.fake;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestFakesConfig {

    @Bean
    @Primary
    public InMemoryOtpStore inMemoryOtpStore() {
        return new InMemoryOtpStore();
    }

    @Bean
    @Primary
    public NoOpEmailSender noOpEmailSender() {
        return new NoOpEmailSender();
    }
}
