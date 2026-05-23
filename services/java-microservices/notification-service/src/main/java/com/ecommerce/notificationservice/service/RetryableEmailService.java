package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service that wraps email sending with retry logic using exponential backoff.
 * Retries up to the specified number of times before giving up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableEmailService {

    private final EmailService emailService;

    private static final long INITIAL_BACKOFF_MS = 1000L;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Sends an email with retry logic and exponential backoff.
     *
     * @param to           recipient email
     * @param subject      email subject
     * @param templateName template name
     * @param variables    template variables
     * @param maxRetries   maximum number of retry attempts
     */
    public void sendWithRetry(String to, String subject, String templateName,
                              Map<String, Object> variables, int maxRetries) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt <= maxRetries) {
            try {
                emailService.sendHtmlEmail(to, subject, templateName, variables);
                return;
            } catch (EmailSendException e) {
                attempt++;
                if (attempt > maxRetries) {
                    log.error("Failed to send email after {} attempts. to={}, subject={}", maxRetries, to, subject);
                    throw e;
                }
                log.warn("Email send attempt {} failed for to={}, retrying in {}ms. Error: {}",
                        attempt, to, backoffMs, e.getMessage());
                sleep(backoffMs);
                backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry sleep interrupted");
        }
    }
}
