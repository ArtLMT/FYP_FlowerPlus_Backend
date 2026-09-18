package com.lmt.fyp.flowerplus.module.auth.infrastructure;

import com.lmt.fyp.flowerplus.module.auth.service.OtpService;
import com.lmt.fyp.flowerplus.module.user.event.StaffAccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges a newly created Staff account to its first-password code. Auth owns
 * code issuance, so the user module publishes {@link StaffAccountCreatedEvent}
 * and this listener turns it into an emailed code — the user module never learns
 * how a password is set up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaffInvitationListener {

    private final OtpService otpService;

    // AFTER_COMMIT: the account must be durable before a code that sets its
    // password goes out. @Async so creating the account returns without waiting
    // on the OTP store or mail. A throttle refusal here is tolerated and logged
    // — the account still exists and the person can use forgot-password.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStaffAccountCreated(StaffAccountCreatedEvent event) {
        try {
            otpService.issueStaffInvitation(event.email());
        } catch (Exception e) {
            log.warn("Could not send the first-password code to new staff {}; "
                    + "they can use forgot-password", event.email(), e);
        }
    }
}
