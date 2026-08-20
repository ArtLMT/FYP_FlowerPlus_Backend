package com.lmt.fyp.flowerplus.module.email.application.event;

public record OtpRequestedEvent(String email, String otp) { }