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

    private static final String CODE_TEMPLATE = "email/otp-verification";

    private final EmailSender emailSender;
    private final OtpProperties otpProperties;

    @Override
    public void sendOTP(String email, String otp) {
        sendCode(email, otp,
                "Your FlowerPlus verification code",
                "verification code",
                "Verify your email address",
                "Enter the code below to finish creating your FlowerPlus account.",
                "If you did not request this code, you can safely ignore this email — no account will be created.");
    }

    @Override
    public void sendPasswordResetCode(String email, String otp) {
        sendCode(email, otp,
                "Your FlowerPlus password reset code",
                "password reset code",
                "Reset your password",
                "Enter the code below to choose a new password for your FlowerPlus account.",
                "If you did not ask to reset your password, you can safely ignore this email — your password will not change.");
    }

    /** One layout for every code email; only the wording changes. */
    private void sendCode(String email, String otp, String subject, String codeLabel,
                          String heading, String lead, String ignoreNote) {
        // Derived, not hardcoded: the number the user reads must track the TTL
        // the store actually enforces (application.security.otp.ttl).
        Map<String, Object> variables = Map.of(
                "otp", otp,
                "expiryMinutes", otpProperties.ttl().toMinutes(),
                "codeLabel", codeLabel,
                "heading", heading,
                "lead", lead,
                "ignoreNote", ignoreNote);

        emailSender.sendEmail(EmailMessage.builder()
                .receiver(email)
                .subject(subject)
                .templateName(CODE_TEMPLATE)
                .variables(variables)
                .build());
    }
}
