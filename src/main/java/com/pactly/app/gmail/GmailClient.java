package com.pactly.app.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.pactly.app.service.GmailTokenRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailClient {

    private static final String APPLICATION_NAME = "Pactly";
    private static final String ME = "me";
    private static final int MAX_RESULTS = 50;

    private final GmailTokenRefreshService tokenRefreshService;

    /**
     * Initial sync — fetches the last N emails and returns
     * the historyId of the most recent message for future incremental syncs.
     */
    @Retryable(
            retryFor = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public GmailFetchResult fetchRecentMessages(UUID userId) throws IOException {
        Gmail gmail = buildGmailService(userId);

        ListMessagesResponse response = gmail.users()
                .messages()
                .list(ME)
                .setMaxResults((long) MAX_RESULTS)
                .setQ("in:inbox")
                .execute();

        List<Message> messages = new ArrayList<>();
        if (response.getMessages() == null) {
            log.info("No messages found for user: {}", userId);
            return new GmailFetchResult(messages, null);
        }

        // Fetch full message details for each result
        for (Message msg : response.getMessages()) {
            Message fullMessage = gmail.users()
                    .messages()
                    .get(ME, msg.getId())
                    .setFormat("full")
                    .execute();
            messages.add(fullMessage);
        }

        // Get the historyId from the first (most recent) message
        String historyId = messages.isEmpty()
                ? null
                : String.valueOf(messages.get(0).getHistoryId());

        log.info("Fetched {} messages for user: {}", messages.size(), userId);
        return new GmailFetchResult(messages, historyId);
    }

    /**
     * Incremental sync only fetches emails newer than the lastHistoryId.
     * This is what runs on every subsequent poll.
     */
    @Retryable(
            retryFor = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public GmailFetchResult fetchMessagesSince(UUID userId,
                                               String lastHistoryId) throws IOException {
        Gmail gmail = buildGmailService(userId);

        List<Message> messages = new ArrayList<>();
        String newHistoryId = lastHistoryId;
        String pageToken = null;

        do {
            ListHistoryResponse historyResponse = gmail.users()
                    .history()
                    .list(ME)
                    .setStartHistoryId(BigInteger.valueOf(Long.parseLong(lastHistoryId)))
                    .setHistoryTypes(List.of("messageAdded"))
                    .setPageToken(pageToken)
                    .execute();

            if (historyResponse.getHistory() != null) {
                for (History history : historyResponse.getHistory()) {
                    if (history.getMessagesAdded() != null) {
                        for (var added : history.getMessagesAdded()) {
                            Message fullMessage = gmail.users()
                                    .messages()
                                    .get(ME, added.getMessage().getId())
                                    .setFormat("full")
                                    .execute();
                            messages.add(fullMessage);
                        }
                    }
                }
            }

            if (historyResponse.getHistoryId() != null) {
                newHistoryId = String.valueOf(historyResponse.getHistoryId());
            }

            pageToken = historyResponse.getNextPageToken();

        } while (pageToken != null);

        log.info("Incremental fetch: {} new messages for user: {}",
                messages.size(), userId);
        return new GmailFetchResult(messages, newHistoryId);
    }

    private Gmail buildGmailService(UUID userId) throws IOException {
        String accessToken = tokenRefreshService.getValidAccessToken(userId);

        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        try {
            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new IOException("Failed to build Gmail service", e);
        }
    }
}