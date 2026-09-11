package com.lmt.fyp.flowerplus.module.auth.event;

import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;

/**
 * Published by auth when a code is issued; consumed by the email module to
 * deliver it. Owned by the publisher, so auth never learns what SMTP is. Carries
 * the purpose so the email can say what the code is for.
 */
public record OtpRequestedEvent(OtpPurpose purpose, String email, String otp) { }
