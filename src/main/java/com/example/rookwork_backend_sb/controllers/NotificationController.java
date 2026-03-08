package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.Dtos.notifications.NotificationResponse;
import com.example.rookwork_backend_sb.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getNotification() {
        return ResponseEntity.ok(notificationService.getAll());
    }
}