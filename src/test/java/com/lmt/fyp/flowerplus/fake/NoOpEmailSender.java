package com.lmt.fyp.flowerplus.fake;

import com.lmt.fyp.flowerplus.module.email.application.port.out.EmailSenderPort;
import com.lmt.fyp.flowerplus.module.email.domain.model.EmailMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoOpEmailSender implements EmailSenderPort {

    private final List<EmailMessage> sentMessages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendEmail(EmailMessage emailMessage) {
        sentMessages.add(emailMessage);
    }

    public List<EmailMessage> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
    }
}
