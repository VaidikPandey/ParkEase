package com.parkease.notification.service.impl;

import com.parkease.notification.domain.entity.Notification;
import com.parkease.notification.repository.NotificationRepository;
import com.parkease.notification.service.NotificationService;
import com.parkease.notification.web.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${mail.from:noreply@parkease.com}")
    private String mailFrom;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent: recipientId={} type={}", saved.getRecipientId(), saved.getType());
        return toResponse(saved);
    }

    @Override
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        List<NotificationResponse> responses = new ArrayList<>();
        for (Long recipientId : request.getRecipientIds()) {
            Notification n = Notification.builder()
                    .recipientId(recipientId)
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .channel(request.getChannel())
                    .isRead(false)
                    .build();
            responses.add(toResponse(notificationRepository.save(n)));
        }
        log.info("Bulk notification sent to {} recipients, type={}", request.getRecipientIds().size(), request.getType());
        return responses;
    }

    @Override
    public void markAsRead(Long notificationId, Long callerId, boolean isAdmin) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        if (!isAdmin && !n.getRecipientId().equals(callerId)) {
            throw new AccessDeniedException("You do not own this notification");
        }
        notificationRepository.markAsReadById(notificationId);
    }

    @Override
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllReadByRecipient(recipientId);
        log.info("All notifications marked read for recipientId={}", recipientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getByRecipient(Long recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdOrderBySentAtDesc(recipientId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnread(Long recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdAndIsReadOrderBySentAtDesc(recipientId, false, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public void deleteNotification(Long notificationId, Long callerId, boolean isAdmin) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        if (!isAdmin && !n.getRecipientId().equals(callerId)) {
            throw new AccessDeniedException("You do not own this notification");
        }
        notificationRepository.deleteByNotificationId(notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("Email disabled. Would send to={} subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to={}: {}", to, e.getMessage());
        }
    }

    @Override
    public NotificationResponse createFromEvent(Long recipientId,
                                                 Notification.NotificationType type,
                                                 String title, String message,
                                                 Long relatedId, String relatedType) {
        Notification n = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .channel(Notification.NotificationChannel.APP)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .isRead(false)
                .build();
        return toResponse(notificationRepository.save(n));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .isRead(n.getIsRead())
                .sentAt(n.getSentAt())
                .build();
    }
}
