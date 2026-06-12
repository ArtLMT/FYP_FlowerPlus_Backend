package com.lmt.fyp.flowerplus.module.auth.service;

import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.user.entity.User;

public interface RefreshTokenService {

    /** Creates a new refresh token for the given user, deleting any existing ones. */
    RefreshToken createRefreshToken(User user);

    /** Finds a refresh token by its token string and verifies it has not expired. */
    RefreshToken verifyRefreshToken(String token);

    /** Revokes (deletes) a specific refresh token. */
    void revokeRefreshToken(String token);
}
