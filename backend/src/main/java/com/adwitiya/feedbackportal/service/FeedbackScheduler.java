package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.config.properties.AppProperties;
import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.domain.enums.NotificationType;
import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import com.adwitiya.feedbackportal.repository.RefreshTokenRepository;
import com.adwitiya.feedbackportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackScheduler {
    private final FeedbackRepository feedbackRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void warnAboutOverdueFeedback() {
        List<Feedback> overdue = feedbackRepository
                .findOverdue(Instant.now(), PageRequest.of(0, 100))
                .getContent();

        if (overdue.isEmpty()) {
            return;
        }
        log.info("SLA sweep found {} overdue tickets", overdue.size());

        for (Feedback feedback : overdue) {
            if (feedback.getAssignedTo() == null || feedback.getAssignedTo().getUser() == null) {
                continue;
            }
            notificationService.notifyUser(
                    feedback.getAssignedTo().getUser(),
                    NotificationType.SLA_BREACH_WARNING,
                    "Overdue: " + feedback.getTicketNumber(),
                    "This %s-priority ticket passed its resolution deadline."
                            .formatted(feedback.getPriority()),
                    "/feedback/" + feedback.getId());
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoCloseResolvedFeedback() {
        Instant cutoff = Instant.now().minus(appProperties.getAutoCloseAfter());
        List<Feedback> stale = feedbackRepository.findResolvedBefore(cutoff);

        int closed = 0;
        for (Feedback feedback : stale) {
            try {
                feedback.transitionTo(FeedbackStatus.CLOSED, feedback.getSubmittedBy().getUser(),
                        "Automatically closed after %d days without a response."
                                .formatted(appProperties.getAutoCloseAfter().toDays()));
                feedbackRepository.save(feedback);
                closed++;
            } catch (IllegalStateException ex) {
                log.warn("Could not auto-close {}: {}", feedback.getTicketNumber(), ex.getMessage());
            }
        }
        if (closed > 0) {
            log.info("Auto-closed {} resolved tickets older than {}", closed, cutoff);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int removed = refreshTokenRepository.deleteExpiredBefore(Instant.now().minusSeconds(86_400));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    @Transactional
    public void releaseExpiredLockouts() {
        int unlocked = userRepository.unlockExpiredAccounts(Instant.now());
        if (unlocked > 0) {
            log.info("Released {} expired account lockouts", unlocked);
        }
    }
}
