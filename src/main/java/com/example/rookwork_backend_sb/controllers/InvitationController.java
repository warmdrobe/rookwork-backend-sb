package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.invitations.SendInviteRequest;
import com.example.rookwork_backend_sb.services.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller exposing endpoints for sending project invitations, responding to invitations, and listing pending invites.
 */
@RestController
@RequestMapping("api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * Sends an invite to join a project.
     *
     * @param request the invite details containing project ID and recipient email
     * @return response entity indicating the invite has been sent successfully
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendInvite(@RequestBody SendInviteRequest request) {
        invitationService.sendInvite(request.getProjectId(), request.getEmail());
        return ResponseEntity.ok("Invitation sent");
    }

    /**
     * Responds (accepts/declines) to a pending project invitation.
     *
     * @param invitationId the unique identifier of the invitation
     * @param accept true to accept, false to decline
     * @return response entity indicating the response status
     */
    @PostMapping("/{invitationId}/respond")
    public ResponseEntity<?> respond(
            @PathVariable UUID invitationId,
            @RequestParam boolean accept) {
        invitationService.respondInvite(invitationId, accept);
        return ResponseEntity.ok(accept ? "Accepted" : "Declined");
    }

    /**
     * Retrieves all pending invitations for the current user.
     *
     * @return response entity containing a list of pending invitations
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(invitationService.getPendingInvites());
    }
}
