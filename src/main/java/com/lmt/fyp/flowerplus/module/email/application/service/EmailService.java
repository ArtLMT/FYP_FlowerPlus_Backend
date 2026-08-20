package com.lmt.fyp.flowerplus.module.email.application.service;

import com.lmt.fyp.flowerplus.module.email.application.port.in.SendEmailUseCase;
import com.lmt.fyp.flowerplus.module.email.application.port.out.EmailSenderPort;
import com.lmt.fyp.flowerplus.module.email.domain.model.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService implements SendEmailUseCase{
    private final EmailSenderPort emailSenderPort;

    @Override
    public void sendOTP(String email, String otp) {
        EmailMessage message = EmailMessage.builder()
                .receiver(email)
                .subject("Your FlowerPlus verification code")
                .templateName("email/otp-verification")
                .variables(Map.of("otp", otp, "expiryMinutes", 5))
                .build();

        emailSenderPort.sendEmail(message);
    }
}