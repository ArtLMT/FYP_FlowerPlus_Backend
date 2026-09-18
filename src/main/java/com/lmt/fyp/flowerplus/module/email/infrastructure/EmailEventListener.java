package com.lmt.fyp.flowerplus.module.email.infrastructure;

import com.lmt.fyp.flowerplus.module.auth.event.OtpRequestedEvent;
import com.lmt.fyp.flowerplus.module.auth.event.StaffInvitationRequestedEvent;
import com.lmt.fyp.flowerplus.module.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {
    private final EmailService emailService;

    @Async
    // AFTER_COMMIT so a code is never mailed for a registration that rolled back.
    // fallbackExecution so the event still fires when there is NO transaction at
    // all: a plain resend writes nothing to the database, and without this the
    // listener is skipped silently and the mail simply never arrives.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOtpRequested(OtpRequestedEvent event){
        try {
            switch (event.purpose()) {
                case REGISTRATION -> emailService.sendOTP(event.email(), event.otp());
                case PASSWORD_RESET -> emailService.sendPasswordResetCode(event.email(), event.otp());
            }
        } catch (Exception e) {
            log.error("Failed to send OTP to " + event.email(), e);
        }
    }

    @Async
    // Same delivery contract as onOtpRequested: AFTER_COMMIT with fallbackExecution,
    // because auth issues the invitation code outside any transaction (from the
    // async staff-created listener).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStaffInvitationRequested(StaffInvitationRequestedEvent event) {
        try {
            emailService.sendStaffInvitation(event.email(), event.otp());
        } catch (Exception e) {
            log.error("Failed to send staff invitation to " + event.email(), e);
        }
    }
}
