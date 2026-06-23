package com.pactly.app.security;

import com.pactly.app.entity.User;
import com.pactly.app.service.OAuthUserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthUserRegistrationService registrationService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String displayName = oAuth2User.getAttribute("name");
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        log.info("OAuth2 login success for email: {}", email);

        // Load the authorized client — contains the Gmail access + refresh tokens
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                registrationId, oauthToken.getName()
        );

        String accessToken = client.getAccessToken().getTokenValue();

        String refreshToken = client.getRefreshToken() != null
                ? client.getRefreshToken().getTokenValue()
                : null;

        OffsetDateTime expiry = client.getAccessToken().getExpiresAt() != null
                ? client.getAccessToken().getExpiresAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);

        Set<String> scopeSet = client.getAccessToken().getScopes();
        String scopes = scopeSet != null ? String.join(",", scopeSet) : null;

        // 1. Find or create our user record
        User user = registrationService.findOrCreateUser(email, displayName);

        // 2. Store or update Gmail tokens
        registrationService.saveOrUpdateGmailToken(user, accessToken, refreshToken, expiry, scopes);

        // 3. Issue our own JWT
        String jwt = jwtUtil.generate(user.getEmail());

        // 4. Redirect to frontend with JWT as query param
        String redirectUrl = frontendUrl + "/auth/callback?token=" + jwt;
        log.debug("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
