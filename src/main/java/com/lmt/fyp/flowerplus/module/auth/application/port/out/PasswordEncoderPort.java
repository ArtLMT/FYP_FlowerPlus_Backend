package com.lmt.fyp.flowerplus.module.auth.application.port.out;

public interface PasswordEncoderPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
