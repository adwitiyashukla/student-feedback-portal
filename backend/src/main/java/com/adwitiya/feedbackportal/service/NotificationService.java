package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.config.properties.AppProperties;
import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.entity.Notification;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.NotificationType;
import com.adwitiya.feedbackportal.repository.NotificationRepository;
import com.adwitiya.feedbackportal.web.dto.response.NotificationResponse;
import com.adwitiya.feedbackportal.web.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Transactional
    public void notifyUser(User recipient, NotificationType type, String title, String message, String link) {
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .build());
    }

    @Transactional
    public void notifyAboutFeedback(User recipient, Feedback feedback, NotificationType type,
                                    String title, String message) {
        String link = "/feedback/" + feedback.getId();
        notifyUser(recipient, type, title, message, link);
        sendEmail(recipient.getEmail(), "[%s] %s".formatted(feedback.getTicketNumber(), title),
                """
                %s

                Ticket:  %s
                Subject: %s
                Status:  %s

                View it here: %s%s

                --
                Student Feedback Portal (automated message, do not reply)
                """.formatted(message, feedback.getTicketNumber(), feedback.getTitle(),
                        feedback.getStatus(), appProperties.getBaseUrl(), link));
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(appProperties.getMailFrom());
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
            log.debug("Notification email sent to {}", to);
        } catch (MailException ex) {
            log.warn("Could not send notification email to {}: {}", to, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recent(Long userId) {
        return notificationRepository.findTop10ByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
