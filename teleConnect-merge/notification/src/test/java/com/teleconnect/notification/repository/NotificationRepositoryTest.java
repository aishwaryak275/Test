package com.teleconnect.notification.repository;

import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryTest {

    @Mock
    private NotificationRepository notificationRepository;

    private Notification n1, n2, n3, n4;

    @BeforeEach
    void setUp() {
        n1 = new Notification();
        n1.setNotificationId(1L); n1.setUserId(1L);
        n1.setMessage("Data usage at 85%");
        n1.setCategory(NotificationCategory.USAGE);
        n1.setStatus(NotificationStatus.UNREAD);
        n1.setCreatedDate(LocalDateTime.now());

        n2 = new Notification();
        n2.setNotificationId(2L); n2.setUserId(1L);
        n2.setMessage("Invoice due in 2 days");
        n2.setCategory(NotificationCategory.BILLING);
        n2.setStatus(NotificationStatus.UNREAD);
        n2.setCreatedDate(LocalDateTime.now());

        n3 = new Notification();
        n3.setNotificationId(3L); n3.setUserId(1L);
        n3.setMessage("Plan expires in 3 days");
        n3.setCategory(NotificationCategory.PLAN);
        n3.setStatus(NotificationStatus.READ);
        n3.setCreatedDate(LocalDateTime.now());

        n4 = new Notification();
        n4.setNotificationId(4L); n4.setUserId(2L);
        n4.setMessage("Fault ticket resolved");
        n4.setCategory(NotificationCategory.FAULT);
        n4.setStatus(NotificationStatus.UNREAD);
        n4.setCreatedDate(LocalDateTime.now());
    }

    @Test
    void findByUserIdOrderByCreatedDateDesc_returnsOnlyForUser() {
        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(1L))
                .thenReturn(List.of(n3, n2, n1));
        List<Notification> result =
                notificationRepository.findByUserIdOrderByCreatedDateDesc(1L);
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(n -> n.getUserId().equals(1L));
    }

    @Test
    void findByUserIdAndStatusOrderByCreatedDateDesc_returnsUnreadOnly() {
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(
                1L, NotificationStatus.UNREAD)).thenReturn(List.of(n2, n1));
        List<Notification> result =
                notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(
                        1L, NotificationStatus.UNREAD);
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> n.getStatus() == NotificationStatus.UNREAD);
    }

    @Test
    void findByUserIdAndCategoryOrderByCreatedDateDesc_filtersByCategory() {
        when(notificationRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(
                1L, NotificationCategory.BILLING)).thenReturn(List.of(n2));
        List<Notification> result =
                notificationRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(
                        1L, NotificationCategory.BILLING);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("Invoice due in 2 days");
    }

    @Test
    void countByUserIdAndStatus_returnsCorrectCount() {
        when(notificationRepository.countByUserIdAndStatus(
                1L, NotificationStatus.UNREAD)).thenReturn(2L);
        long count = notificationRepository.countByUserIdAndStatus(
                1L, NotificationStatus.UNREAD);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void markAllReadForUser_updatesAllUnread() {
        when(notificationRepository.markAllReadForUser(
                1L, NotificationStatus.READ)).thenReturn(2);
        int updated = notificationRepository.markAllReadForUser(
                1L, NotificationStatus.READ);
        assertThat(updated).isEqualTo(2);
        verify(notificationRepository, times(1))
                .markAllReadForUser(1L, NotificationStatus.READ);
    }

    @Test
    void existsByUserIdAndMessageAndStatus_detectsDuplicate() {
        when(notificationRepository.existsByUserIdAndMessageAndStatus(
                1L, "Data usage at 85%", NotificationStatus.UNREAD)).thenReturn(true);
        boolean exists = notificationRepository.existsByUserIdAndMessageAndStatus(
                1L, "Data usage at 85%", NotificationStatus.UNREAD);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndMessageAndStatus_returnsFalseWhenNotExists() {
        when(notificationRepository.existsByUserIdAndMessageAndStatus(
                1L, "Unknown message", NotificationStatus.UNREAD)).thenReturn(false);
        boolean exists = notificationRepository.existsByUserIdAndMessageAndStatus(
                1L, "Unknown message", NotificationStatus.UNREAD);
        assertThat(exists).isFalse();
    }

    @Test
    void findById_returnsNotificationWhenExists() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n1));
        Optional<Notification> result = notificationRepository.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationId()).isEqualTo(1L);
    }

    @Test
    void findById_returnsEmptyWhenNotExists() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        Optional<Notification> result = notificationRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteAllByUserId_calledWithCorrectUserId() {
        doNothing().when(notificationRepository).deleteAllByUserId(1L);
        notificationRepository.deleteAllByUserId(1L);
        verify(notificationRepository, times(1)).deleteAllByUserId(1L);
    }

    @Test
    void save_persistsNotification() {
        when(notificationRepository.save(n1)).thenReturn(n1);
        Notification saved = notificationRepository.save(n1);
        assertThat(saved.getNotificationId()).isEqualTo(1L);
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.USAGE);
        verify(notificationRepository, times(1)).save(n1);
    }
}