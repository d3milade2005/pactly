package com.pactly.app.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.model.Message;
import com.pactly.app.entity.*;
import com.pactly.app.gmail.GmailClient;
import com.pactly.app.gmail.GmailFetchResult;
import com.pactly.app.gmail.GmailMessageParser;
import com.pactly.app.gmail.ParsedGmailMessage;
import com.pactly.app.repository.GmailSyncStateRepository;
import com.pactly.app.repository.OutboxEventRepository;
import com.pactly.app.repository.RawMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailIngestionService {

    private final GmailClient gmailClient;
    private final GmailSyncStateRepository syncStateRepository;
    private final RawMessageRepository rawMessageRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MessageFingerPrintService fingerprintService;
    private final GmailMessageParser messageParser;
    private final ObjectMapper objectMapper;

    @Transactional
    public void ingestForUser(User user) {
        log.info("Starting Gmail ingestion for user: {}", user.getEmail());

        GmailSyncState syncState = syncStateRepository
                .findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("No sync state found, creating for user: {}", user.getEmail());
                    return syncStateRepository.save(new GmailSyncState(user));
                });

        try {
            GmailFetchResult result = syncState.isFirstSync()
                    ? gmailClient.fetchRecentMessages(user.getId())
                    : gmailClient.fetchMessagesSince(user.getId(), syncState.getLastHistoryId());

            int saved   = 0;
            int skipped = 0;

            for (Message message : result.messages()) {
                boolean isNew = processMessage(user, message);
                if (isNew) saved++;
                else skipped++;
            }

            if (result.latestHistoryId() != null) {
                syncState.updateAfterSync(result.latestHistoryId());
                syncStateRepository.save(syncState);
            }

            log.info("Ingestion complete for user: {} — saved: {}, skipped: {}", user.getEmail(), saved, skipped);

        } catch (IOException e) {
            log.error("Gmail ingestion failed for user: {}", user.getEmail(), e);
        }
    }

    private boolean processMessage(User user, Message message) {
        ParsedGmailMessage parsed = messageParser.parse(message);

        String fingerprint = fingerprintService.compute(
                MessageSource.GMAIL.name(),
                parsed.externalId(),
                user.getId().toString()
        );

        if (rawMessageRepository.existsByMessageFingerprint(fingerprint)) {
            log.debug("Already processed message: {}", parsed.externalId());
            return false;
        }

        RawMessage rawMessage = buildRawMessage(user, parsed, fingerprint);
        rawMessageRepository.save(rawMessage);

        writeOutboxEvent(rawMessage, user);

        log.debug("Saved new message: {} from: {}", parsed.externalId(), parsed.senderEmail());
        return true;
    }

    private RawMessage buildRawMessage(User user,
                                       ParsedGmailMessage parsed,
                                       String fingerprint) {
        RawMessage raw = new RawMessage();
        raw.setUser(user);
        raw.setSource(MessageSource.GMAIL);
        raw.setExternalId(parsed.externalId());
        raw.setMessageFingerprint(fingerprint);
        raw.setSubject(parsed.subject());
        raw.setSenderEmail(parsed.senderEmail() != null ? parsed.senderEmail() : "unknown");
        raw.setRecipientEmails(parsed.recipientEmails());
        raw.setBodySnippet(parsed.fullBody());
        raw.setReceivedAt(parsed.receivedAt());
        return raw;
    }

    private void writeOutboxEvent(RawMessage rawMessage, User user) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "rawMessageId", rawMessage.getId().toString(),
                    "userId",       user.getId().toString(),
                    "source",       MessageSource.GMAIL.name()
            ));

            OutboxEvent event = OutboxEvent.of(
                    "RawMessage",
                    rawMessage.getId(),
                    "MessageReceived",
                    payload
            );
            outboxEventRepository.save(event);

        } catch (Exception e) {
            log.error("Failed to write outbox event for message: {}",
                    rawMessage.getId(), e);
        }
    }
}