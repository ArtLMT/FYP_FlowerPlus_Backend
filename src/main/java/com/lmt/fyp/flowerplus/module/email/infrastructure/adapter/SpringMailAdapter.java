package com.lmt.fyp.flowerplus.module.email.infrastructure.adapter;

import com.lmt.fyp.flowerplus.module.email.application.port.out.EmailSenderPort;
import com.lmt.fyp.flowerplus.module.email.domain.model.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringMailAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    @Value("${application.mail.from}")
    private String from;

    @Override
    public void sendEmail(EmailMessage emailMessage) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(from);
            helper.setTo(emailMessage.getReceiver());
            helper.setSubject(emailMessage.getSubject());
            helper.setText(emailMessage.getBody(), emailMessage.isHtml());

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + emailMessage.getReceiver(), e);
        }
    }
}