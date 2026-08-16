package com.lmt.fyp.flowerplus.module.email.application.service;

import com.lmt.fyp.flowerplus.module.email.application.port.in.SendEmailUseCase;
import com.lmt.fyp.flowerplus.module.email.application.port.out.EmailSenderPort;
import com.lmt.fyp.flowerplus.module.email.domain.model.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService implements SendEmailUseCase{
    private final EmailSenderPort emailSenderPort;

    @Override
    public void sendOTP(String email, String otp) {
        EmailMessage message = EmailMessage.builder()
                .receiver(email)
                .subject("Your FlowerPlus verification code")
                .body("Your verification code is " + otp + ". It expires in 5 minutes.")
                .isHtml(false)
                .build();

        emailSenderPort.sendEmail(message);
    }
}