package com.lmt.fyp.flowerplus.security.oauth2;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service that handles loading and creating/updating users authenticated via OAuth2.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerName = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(providerName);
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + providerName);
        }

        return processOAuth2User(oAuth2User, provider);
    }

    private OAuth2User processOAuth2User(OAuth2User oAuth2User, AuthProvider provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = EmailNormalizer.normalize((String) attributes.get("email"));

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        if (!Boolean.TRUE.equals(attributes.get("email_verified"))) {
            throw new OAuth2AuthenticationException("Email not verified by OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            // BANNED is tested explicitly rather than through SecurityUser.isAuthBlocked:
            // that method covers BANNED *and* PENDING, and this path adopts a PENDING
            // account below instead of rejecting it. Login and refresh still go through
            // isAuthBlocked — this is the one deliberate exception to it, so do not
            // "simplify" this back or the pre-hijack path reopens.
            if (user.getStatus() == UserAccountStatus.BANNED) {
                // Carries a stable error code so OAuth2AuthenticationFailureHandler
                // can tell this apart from any other authentication failure.
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(OAuth2AuthenticationFailureHandler.ACCOUNT_BLOCKED),
                        "Account is not permitted to authenticate");
            }

            if (user.getStatus() == UserAccountStatus.PENDING) {
                // Adopt PENDING local account: invalidate old squatter password, link provider, activate
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setProvider(provider);
                user.setProviderId(getProviderId(attributes, provider));
                user.setStatus(UserAccountStatus.ACTIVE);
                user = userRepository.save(user);
            } else if (user.getProvider() == AuthProvider.LOCAL) {
                // Safe: only reached by a non-BANNED, non-PENDING account whose email the provider verified,
                // so there is nothing left to link.
                return new CustomOAuth2User(user, attributes);
            } else if (user.getProvider() != provider) {
                // Link provider details if they haven't been linked yet
                user.setProvider(provider);
                user.setProviderId(getProviderId(attributes, provider));
                user = userRepository.save(user);
            }
        } else {
            user = registerNewOAuth2User(attributes, provider);
        }

        return new CustomOAuth2User(user, attributes);
    }

    private User registerNewOAuth2User(Map<String, Object> attributes, AuthProvider provider) {
        String email = EmailNormalizer.normalize((String) attributes.get("email"));
        String fullName = (String) attributes.getOrDefault("name", "OAuth2 User");
        String providerId = getProviderId(attributes, provider);

        // Username is the email, matching local registration. Email is already
        // unique, so this needs no separate collision check — the old
        // prefix-derivation loop was a check-then-act race (N13).
        String randomPassword = UUID.randomUUID().toString();

        User user = User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(randomPassword))
                .role(UserRole.CUSTOMER)
                .status(UserAccountStatus.ACTIVE)
                .provider(provider)
                .providerId(providerId)
                .build();

        User savedUser = userRepository.save(user);

        // Google returns the avatar as a plain URL string.
        String avatar = (String) attributes.get("picture");

        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .fullName(fullName)
                .avatar(avatar)
                .build();
        userProfileRepository.save(profile);

        return savedUser;
    }

    private String getProviderId(Map<String, Object> attributes, AuthProvider provider) {
        if (provider == AuthProvider.GOOGLE) {
            // Google's OIDC subject claim is the stable per-user identifier.
            return (String) attributes.get("sub");
        }
        return null;
    }
}
