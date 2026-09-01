package com.lmt.fyp.flowerplus.module.auth.service;

/**
 * A freshly issued access + refresh token pair. The service layer returns this;
 * the controller shapes it into the wire DTO.
 */
public record TokenPair(String accessToken, String refreshToken) { }
