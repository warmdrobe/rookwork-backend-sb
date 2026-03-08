package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.Dtos.notifications.NotificationResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("api/notifications")
public class Notification {
    @GetMapping("/")
    public ResponseEntity<List<NotificationResponse>> getNotification() {
        return ResponseEntity.ok(notificationService.getAll());
    }
}
