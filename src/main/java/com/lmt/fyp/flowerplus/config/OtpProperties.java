package com.lmt.fyp.flowerplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param ttl            how long a code stays valid
 * @param maxAttempts    guesses allowed against one code before it is destroyed
 * @param resendInterval minimum gap between sends to the same address
 */
@ConfigurationProperties(prefix = "application.security.otp")
public record OtpProperties(Duration ttl, int maxAttempts, Duration resendInterval) {
}
