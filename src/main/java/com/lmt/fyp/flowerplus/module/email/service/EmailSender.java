package com.lmt.fyp.flowerplus.module.email.service;

/**
 * The seam onto the outside world for delivering mail.
 *
 * <p>This interface earns its keep: real delivery needs an SMTP server, so tests
 * substitute a fake here. The production implementation is
 * {@code infrastructure.SpringMailSender}.
 */
public interface EmailSender {

    void sendEmail(EmailMessage emailMessage);
}
