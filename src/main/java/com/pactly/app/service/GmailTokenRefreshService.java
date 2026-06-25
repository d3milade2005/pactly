package com.pactly.app.service;


import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.pactly.app.entity.OauthProvider;
import com.pactly.app.entity.OauthToken;
import com.pactly.app.exception.GmailAuthException;
import com.pactly.app.repository.OauthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailTokenRefreshService {

    private final OauthTokenRepository oAuthTokenRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Returns a valid access token for the given user.
     * Refreshes automatically if expired.
     */
    @Transactional
    @Retryable(
            retryFor = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String getValidAccessToken(UUID userId) throws IOException {
        OauthToken token = oAuthTokenRepository
                .findByUserIdAndProvider(userId, OauthProvider.GMAIL)
                .orElseThrow(() -> new GmailAuthException(
                        "No Gmail token found for user: " + userId
                ));

        if (!token.isExpired()) {
            log.debug("Access token still valid for user: {}", userId);
            return token.getAccessToken();
        }

        log.info("Access token expired for user: {}, refreshing...", userId);
        return refreshAndSave(token, userId);
    }

    private String refreshAndSave(OauthToken token, UUID userId) throws IOException {
        try {
            GoogleCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(token.getRefreshToken())
                    .setAccessToken(new AccessToken(
                            token.getAccessToken(),
                            Date.from(token.getTokenExpiry().toInstant())
                    ))
                    .build();

            credentials.refresh();

            AccessToken newAccessToken = credentials.getAccessToken();

            token.setAccessToken(newAccessToken.getTokenValue());
            token.setTokenExpiry(
                    newAccessToken.getExpirationTime() != null
                            ? newAccessToken.getExpirationTime()
                              .toInstant()
                              .atOffset(ZoneOffset.UTC)
                            : OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)
            );
            oAuthTokenRepository.save(token);

            log.info("Successfully refreshed Gmail token for user: {}", userId);
            return newAccessToken.getTokenValue();

        } catch (IOException e) {
            log.error("Failed to refresh Gmail token for user: {}", userId, e);
            throw e;
        }
    }
}