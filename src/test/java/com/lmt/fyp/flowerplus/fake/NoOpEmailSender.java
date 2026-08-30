package com.lmt.fyp.flowerplus.fake;

import com.lmt.fyp.flowerplus.module.email.service.EmailMessage;
import com.lmt.fyp.flowerplus.module.email.service.EmailSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoOpEmailSender implements EmailSender {

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
