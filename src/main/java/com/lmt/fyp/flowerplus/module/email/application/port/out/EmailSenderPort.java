package com.lmt.fyp.flowerplus.module.email.application.port.out;

import com.lmt.fyp.flowerplus.module.email.domain.model.EmailMessage;

public interface EmailSenderPort {
    void sendEmail(EmailMessage emailMessage);
}