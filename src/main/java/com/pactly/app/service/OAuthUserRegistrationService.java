package com.pactly.app.service;

import com.pactly.app.entity.OauthProvider;
import com.pactly.app.entity.OauthToken;
import com.pactly.app.entity.User;
import com.pactly.app.repository.OauthTokenRepository;
import com.pactly.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUserRegistrationService {

    private final UserRepository userRepository;
    private final OauthTokenRepository oauthTokenRepository;

    @Transactional
    public User findOrCreateUser(String email, String displayName) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new user for email: {}", email);
                    return userRepository.save(new User(email, displayName));
                });
    }

    @Transactional
    public void saveOrUpdateGmailToken(User user, String accessToken, String refreshToken, OffsetDateTime expiry, String scopes) {
        oauthTokenRepository.findByUserIdAndProvider(user.getId(), OauthProvider.GMAIL)
                .ifPresentOrElse(
                        existing -> {
                            existing.setAccessToken(accessToken);
                            existing.setTokenExpiry(expiry);
                            existing.setScopes(scopes);
                            // Google only returns refresh token on first consent.
                            // Never overwrite an existing refresh token with null.
                            if (refreshToken != null) {
                                existing.setRefreshToken(refreshToken);
                            }
                            oauthTokenRepository.save(existing);
                            log.debug("Updated Gmail token for user: {}", user.getEmail());
                        },
                        () -> {
                            OauthToken token = new OauthToken();
                            token.setUser(user);
                            token.setProvider(OauthProvider.GMAIL);
                            token.setAccessToken(accessToken);
                            token.setRefreshToken(refreshToken != null ? refreshToken : "");
                            token.setTokenExpiry(expiry);
                            token.setScopes(scopes);
                            oauthTokenRepository.save(token);
                            log.info("Saved new Gmail token for user: {}", user.getEmail());
                        }
                );
    }


}
