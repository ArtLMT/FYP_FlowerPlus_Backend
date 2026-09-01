package com.lmt.fyp.flowerplus.module.auth.service;

public interface AuthService {

    void register(String email, String rawPassword, String fullName);

    TokenPair login(String email, String rawPassword);

    void logout(String refreshToken);

    TokenPair refresh(String refreshToken);
}
