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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class SpringMailAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
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

            Context context = new Context();
            context.setVariables(emailMessage.getVariables());
            String html = templateEngine.process(emailMessage.getTemplateName(), context);

            helper.setText(html, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + emailMessage.getReceiver(), e);
        }
    }
}