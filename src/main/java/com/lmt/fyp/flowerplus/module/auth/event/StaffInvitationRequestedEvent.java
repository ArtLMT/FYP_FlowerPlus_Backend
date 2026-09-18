package com.lmt.fyp.flowerplus.module.auth.event;

/**
 * Published by auth when a first-password code has been issued for a new Staff
 * account; consumed by the email module to deliver it as a welcome email rather
 * than a password-reset one. The code itself is stored under
 * {@code OtpPurpose.PASSWORD_RESET}, so the ordinary reset endpoint sets the
 * password — only the wording of this email differs.
 */
public record StaffInvitationRequestedEvent(String email, String otp) {
}
