package com.lmt.fyp.flowerplus.module.auth.service;

import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.user.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    /**
     * Validates the presented token and rotates it: the presented token is
     * retired and a brand-new token returned. Replaying a token that was
     * already retired is treated as reuse and drops every token for that user.
     *
     * @throws com.lmt.fyp.flowerplus.exception.UnauthorizedException if the
     *         token is unknown, already revoked (reuse), or expired
     */
    RefreshToken rotate(String token);

    void revoke(String token);

    /**
     * Ends every session the user has by deleting all their refresh tokens.
     * Access tokens already issued stay valid until they expire.
     */
    void revokeAll(User user);
}
