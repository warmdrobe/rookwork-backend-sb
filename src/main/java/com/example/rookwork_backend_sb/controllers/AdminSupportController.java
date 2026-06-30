package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.SupportTicketReplyRequest;
import com.example.rookwork_backend_sb.dtos.SupportTicketResponse;
import com.example.rookwork_backend_sb.entities.Notification;
import com.example.rookwork_backend_sb.entities.SupportTicket;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.repositories.SupportTicketRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/support/tickets")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    private void requireAdmin() {
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new UnauthorizedException("Only system administrators can access this resource");
        }
    }

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> getAllTickets() {
        requireAdmin();
        List<SupportTicket> tickets = supportTicketRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(tickets.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @PutMapping("/{id}/reply")
    @Transactional
    public ResponseEntity<SupportTicketResponse> replyTicket(@PathVariable UUID id, @RequestBody SupportTicketReplyRequest request) {
        requireAdmin();
        
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }
        
        if (request.getReply() != null) {
            ticket.setAdminReply(request.getReply());
        }

        SupportTicket saved = supportTicketRepository.save(ticket);

        // Gửi thông báo cho user
        User admin = userRepository.findById(securityUtil.getCurrentUserId()).orElse(null);
        Notification notif = Notification.builder()
                .user(ticket.getUser())
                .sender(admin)
                .title("Quản trị viên đã phản hồi Ticket của bạn")
                .message("Trạng thái: " + saved.getStatus() + "\nTin nhắn: " + saved.getAdminReply())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        notificationRepository.save(notif);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    private SupportTicketResponse mapToResponse(SupportTicket ticket) {
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .type(ticket.getType())
                .status(ticket.getStatus())
                .adminReply(ticket.getAdminReply())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .userId(ticket.getUser().getId())
                .userEmail(ticket.getUser().getEmail())
                .userProfileName(ticket.getUser().getProfileName())
                .build();
    }
}
