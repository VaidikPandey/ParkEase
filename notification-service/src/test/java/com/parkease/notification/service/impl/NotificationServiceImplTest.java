package com.parkease.notification.service.impl;

import com.parkease.notification.domain.entity.Notification;
import com.parkease.notification.repository.NotificationRepository;
import com.parkease.notification.web.dto.BulkNotificationRequest;
import com.parkease.notification.web.dto.NotificationRequest;
import com.parkease.notification.web.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void markAsRead_ShouldCallRepository_WhenOwnerCalls() {
        Long notificationId = 1L;
        Long recipientId = 10L;
        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .recipientId(recipientId)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, recipientId, false);

        verify(notificationRepository).markAsReadById(notificationId);
    }

    @Test
    void markAsRead_ShouldThrow_WhenNotOwnerAndNotAdmin() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .recipientId(99L)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 5L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markAsRead_ShouldAllowAdmin_WhenNotOwner() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .recipientId(99L)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 5L, true);

        verify(notificationRepository).markAsReadById(1L);
    }

    @Test
    void send_ShouldSaveAndReturnResponse() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(1L);
        request.setType(Notification.NotificationType.BOOKING);
        request.setTitle("Test");
        request.setMessage("Hello");
        request.setChannel(Notification.NotificationChannel.APP);

        Notification saved = Notification.builder()
                .notificationId(1L)
                .recipientId(1L)
                .type(Notification.NotificationType.BOOKING)
                .title("Test")
                .message("Hello")
                .isRead(false)
                .build();

        when(notificationRepository.save(any())).thenReturn(saved);

        NotificationResponse response = notificationService.send(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test");
    }

    @Test
    void sendBulk_ShouldSaveForEachRecipient() {
        BulkNotificationRequest request = new BulkNotificationRequest();
        request.setRecipientIds(List.of(1L, 2L, 3L));
        request.setType(Notification.NotificationType.SYSTEM);
        request.setTitle("Bulk");
        request.setMessage("Msg");
        request.setChannel(Notification.NotificationChannel.APP);

        when(notificationRepository.save(any())).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setNotificationId(1L);
            return n;
        });

        List<NotificationResponse> responses = notificationService.sendBulk(request);

        assertThat(responses).hasSize(3);
        verify(notificationRepository, times(3)).save(any());
    }

    @Test
    void markAllRead_ShouldCallRepository() {
        notificationService.markAllRead(1L);

        verify(notificationRepository).markAllReadByRecipient(1L);
    }

    @Test
    void getUnreadCount_ShouldReturnCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void deleteAllByRecipient_ShouldCallRepository() {
        notificationService.deleteAllByRecipient(1L);

        verify(notificationRepository).deleteAllByRecipientId(1L);
    }

    @Test
    void deleteNotification_ShouldDelete_WhenOwnerCalls() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .recipientId(10L)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, 10L, false);

        verify(notificationRepository).deleteByNotificationId(1L);
    }

    @Test
    void deleteNotification_ShouldThrow_WhenNotOwner() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .recipientId(99L)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 5L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createFromEvent_ShouldSaveAndReturnResponse() {
        when(notificationRepository.save(any())).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setNotificationId(1L);
            return n;
        });

        NotificationResponse response = notificationService.createFromEvent(
                1L, Notification.NotificationType.BOOKING, "Title", "Msg", 101L, "BOOKING");

        assertThat(response).isNotNull();
        verify(notificationRepository).save(any());
    }

    @Test
    void sendEmail_ShouldReturnFalse_WhenMailDisabled() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", false);

        boolean result = notificationService.sendEmail("to@test.com", "Subject", "Body");

        assertThat(result).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    void sendEmail_ShouldSendAndReturnTrue_WhenMailEnabled() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "mailFrom", "from@test.com");

        boolean result = notificationService.sendEmail("to@test.com", "Subject", "Body");

        assertThat(result).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void getAll_ShouldReturnAllNotifications() {
        Notification n = Notification.builder().notificationId(1L).recipientId(1L).isRead(false).build();
        when(notificationRepository.findAll()).thenReturn(List.of(n));

        List<NotificationResponse> result = notificationService.getAll();

        assertThat(result).hasSize(1);
    }
}
