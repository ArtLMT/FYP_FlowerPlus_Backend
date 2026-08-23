package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.IssueOtpUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.ResendOtpUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.UserAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResendOtpService implements ResendOtpUseCase {

    private final UserAccountPort userAccountPort;
    private final IssueOtpUseCase issueOtpUseCase;

    @Override
    public void resend(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        userAccountPort.findByEmail(normalizedEmail)
                .filter(user -> user.getStatus() == UserAccountStatus.PENDING)
                .ifPresent(user -> issueOtpUseCase.issueOTP(normalizedEmail));
    }
}
