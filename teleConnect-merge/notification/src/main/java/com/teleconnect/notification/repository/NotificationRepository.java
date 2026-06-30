package com.teleconnect.notification.repository;

import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Used by: GET /fetchAll/{userId}
    List<Notification> findByUserIdOrderByCreatedDateDesc(Long userId);

    // Used by: GET /fetchUnread/{userId}
    List<Notification> findByUserIdAndStatusOrderByCreatedDateDesc(
            Long userId, NotificationStatus status);

    // Used by: GET /fetchByCategory/{userId}/{category}
    List<Notification> findByUserIdAndCategoryOrderByCreatedDateDesc(
            Long userId, NotificationCategory category);

    // Used by: GET /unreadCount/{userId}
    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    // Used by: PATCH /markAllRead/{userId}
    @Modifying
    @Query("UPDATE Notification n SET n.status = :newStatus " +
           "WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllReadForUser(@Param("userId") Long userId,
                           @Param("newStatus") NotificationStatus newStatus);

    // Used by: DELETE /deleteAll/{userId}
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    // Used by schedulers — prevent duplicate notifications
    boolean existsByUserIdAndMessageAndStatus(
            Long userId, String message, NotificationStatus status);
}
