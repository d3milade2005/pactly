package com.pactly.app.gmail;

import com.pactly.app.entity.User;
import com.pactly.app.repository.UserRepository;
import com.pactly.app.service.GmailIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailIngestationScheduler {
    private final GmailIngestionService ingestionService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelayString = "${app.gmail.poll-interval-ms:300000}")
    public void pollAllUsers() {
        log.info("Gmail ingestion scheduler triggered");

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.debug("No users to ingest for");
            return;
        }

        for (User user : users) {
            try {
                ingestionService.ingestForUser(user);
            } catch (Exception e) {
                log.error("Ingestion failed for user: {}", user.getEmail(), e);
            }
        }
    }
}