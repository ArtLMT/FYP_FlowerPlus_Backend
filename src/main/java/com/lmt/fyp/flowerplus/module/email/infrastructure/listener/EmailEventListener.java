package com.lmt.fyp.flowerplus.module.email.infrastructure.listener;

import com.lmt.fyp.flowerplus.module.email.application.event.OtpRequestedEvent;
import com.lmt.fyp.flowerplus.module.email.application.port.in.SendEmailUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {
    private final SendEmailUseCase sendEmailUseCase;
    private final TemplateEngine templateEngine;

    @Async
    // AFTER_COMMIT so a code is never mailed for a registration that rolled back.
    // fallbackExecution so the event still fires when there is NO transaction at
    // all: a plain resend writes nothing to the database, and without this the
    // listener is skipped silently and the mail simply never arrives.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOtpRequested(OtpRequestedEvent event){
        try {
            sendEmailUseCase.sendOTP(event.email(), event.otp());
        } catch (Exception e) {
            log.error("Failed to send OTP to " + event.email(), e);
        }
    }
}
