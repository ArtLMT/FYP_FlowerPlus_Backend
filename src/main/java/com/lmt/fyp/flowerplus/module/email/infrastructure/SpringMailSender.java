package com.lmt.fyp.flowerplus.module.email.infrastructure;

import com.lmt.fyp.flowerplus.module.email.exception.EmailSendException;
import com.lmt.fyp.flowerplus.module.email.service.EmailMessage;
import com.lmt.fyp.flowerplus.module.email.service.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * SMTP adapter: renders the Thymeleaf template and hands the message to the
 * mail server. The only class here that knows what SMTP is.
 */
@Component
public class SpringMailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String from;

    public SpringMailSender(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            @Value("${application.mail.from}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
    }

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
            throw new EmailSendException("Failed to send email to " + emailMessage.getReceiver(), e);
        }
    }
}
