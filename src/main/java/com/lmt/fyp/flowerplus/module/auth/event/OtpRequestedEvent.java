package com.lmt.fyp.flowerplus.module.auth.event;

/**
 * Published by auth when a verification code is issued; consumed by the email
 * module to deliver it. Owned by the publisher, so auth never learns what SMTP
 * is and email never learns why a code was requested.
 */
public record OtpRequestedEvent(String email, String otp) { }
