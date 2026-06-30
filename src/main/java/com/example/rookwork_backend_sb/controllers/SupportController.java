package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.SupportTicketRequest;
import com.example.rookwork_backend_sb.dtos.SupportTicketResponse;
import com.example.rookwork_backend_sb.entities.SupportTicket;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.SupportTicketRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportController {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<SupportTicketResponse> createTicket(@RequestBody SupportTicketRequest request) {
        User user = userRepository.findById(securityUtil.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject())
                .description(request.getDescription())
                .type(request.getType())
                .build();

        SupportTicket saved = supportTicketRepository.save(ticket);
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> getMyTickets() {
        List<SupportTicket> tickets = supportTicketRepository.findAllByUserIdOrderByCreatedAtDesc(securityUtil.getCurrentUserId());
        return ResponseEntity.ok(tickets.stream().map(this::mapToResponse).collect(Collectors.toList()));
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
