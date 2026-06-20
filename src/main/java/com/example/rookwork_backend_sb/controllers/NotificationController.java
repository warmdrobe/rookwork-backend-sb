package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.notifications.NotificationResponse;
import com.example.rookwork_backend_sb.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller exposing endpoints for managing user notifications (retrieval, unread counts, status updates, deletion).
 */
@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves all notifications for the authenticated user.
     *
     * @return response entity containing a list of NotificationResponse DTOs
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    /**
     * Retrieves unread notifications for the authenticated user.
     *
     * @return response entity containing a list of unread NotificationResponse DTOs
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread() {
        return ResponseEntity.ok(notificationService.getUnread());
    }

    /**
     * Counts the total number of unread notifications for the authenticated user.
     *
     * @return response entity containing a map with the unread count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread() {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread()));
    }

    /**
     * Marks a specific notification as read.
     *
     * @param notificationId the unique identifier of the notification
     * @return response entity with no content
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks all unread notifications for the authenticated user as read.
     *
     * @return response entity with no content
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a specific notification.
     *
     * @param notificationId the unique identifier of the notification to delete
     * @return response entity with no content
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID notificationId) {
        notificationService.delete(notificationId);
        return ResponseEntity.noContent().build();
    }
}