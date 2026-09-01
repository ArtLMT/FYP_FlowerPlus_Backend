package com.lmt.fyp.flowerplus.module.email.service.impl;

import com.lmt.fyp.flowerplus.config.OtpProperties;
import com.lmt.fyp.flowerplus.module.email.service.EmailMessage;
import com.lmt.fyp.flowerplus.module.email.service.EmailSender;
import com.lmt.fyp.flowerplus.module.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailSender emailSender;
    private final OtpProperties otpProperties;

    @Override
    public void sendOTP(String email, String otp) {
        // Derived, not hardcoded: the number the user reads must track the TTL
        // the store actually enforces (application.security.otp.ttl).
        EmailMessage message = EmailMessage.builder()
                .receiver(email)
                .subject("Your FlowerPlus verification code")
                .templateName("email/otp-verification")
                .variables(Map.of("otp", otp, "expiryMinutes", otpProperties.ttl().toMinutes()))
                .build();

        emailSender.sendEmail(message);
    }
}
