package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.invitations.SendInviteRequest;
import com.example.rookwork_backend_sb.services.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendInvite(@RequestBody SendInviteRequest request) {
        invitationService.sendInvite(request.getProjectId(), request.getEmail());
        return ResponseEntity.ok("Invitation sent");
    }

    @PostMapping("/{invitationId}/respond")
    public ResponseEntity<?> respond(
            @PathVariable UUID invitationId,
            @RequestParam boolean accept) {
        invitationService.respondInvite(invitationId, accept);
        return ResponseEntity.ok(accept ? "Accepted" : "Declined");
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(invitationService.getPendingInvites());
    }
}
