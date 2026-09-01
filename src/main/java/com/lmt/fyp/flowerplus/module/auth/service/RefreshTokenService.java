package com.lmt.fyp.flowerplus.module.auth.service;

import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.user.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    /** @throws com.lmt.fyp.flowerplus.exception.UnauthorizedException if missing, revoked or expired */
    RefreshToken verify(String token);

    void revoke(String token);
}
