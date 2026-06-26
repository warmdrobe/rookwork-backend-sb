package com.example.rookwork_backend_sb.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.rookwork_backend_sb.dtos.invitations.InvitationResponse;
import com.example.rookwork_backend_sb.entities.Invitation;
import com.example.rookwork_backend_sb.entities.InvitationStatus;
import com.example.rookwork_backend_sb.entities.Notification;
import com.example.rookwork_backend_sb.entities.Project;
import com.example.rookwork_backend_sb.entities.ProjectMember;
import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.entities.ProjectRole;
import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.InvitationRepository;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing project invitation workflows (sending invites, responding, and list queries).
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SecurityUtil securityUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    /**
     * Sends a project invitation to a user by email.
     *
     * @param projectId the unique identifier of the project
     * @param invitedEmail the email of the user to invite
     * @throws ForbiddenException if current user is not a project owner
     * @throws ResourceNotFoundException if user or project is not found
     * @throws ConflictException if user is already a member or invitation is already pending
     */
    public void sendInvite(UUID projectId, String invitedEmail) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        // Verify the sender is a member of the project
        ProjectMember sender = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        // Only the project owner can invite new members
        if (sender.getRole() != ProjectRole.OWNER)
            throw new ForbiddenException("Only OWNER can invite members");

        User invitedUser = userRepository.findByEmail(invitedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Prevent inviting users who are already members
        if (projectMemberRepository.findById(
                new ProjectMemberId(invitedUser.getId(), projectId)).isPresent())
            throw new ConflictException("User is already a member");

        // Prevent duplicate invitations to the same user
        if (invitationRepository.findByProjectIdAndInvitedUserId(
                projectId, invitedUser.getId()).isPresent())
            throw new ConflictException("Invitation already sent");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Invitation invitation = Invitation.builder()
                .project(project)
                .invitedBy(sender.getUser())
                .invitedUser(invitedUser)
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        invitationRepository.save(invitation);

        // Send a real-time and persistent notification to the invited user
        Notification notification = Notification.builder()
                .user(invitedUser)
                .sender(sender.getUser())
                .title("Project Invitation")
                .message(sender.getUser().getProfileName()
                        + " invited you to join \"" + project.getProjectName() + "\"")
                .invitation(invitation)
                .isRead(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                invitedUser.getId().toString(),
                "/queue/notifications",
                Map.of(
                        "type", "INVITATION",
                        "notificationId", notification.getId(),
                        "invitationId", invitation.getId(),
                        "projectName", project.getProjectName(),
                        "invitedBy", sender.getUser().getProfileName()
                )
        );
        emailService.sendProjectInvitation(
                invitedUser.getEmail(),
                project.getProjectName(),
                sender.getUser().getProfileName()
        );
    }

    /**
     * Responds to an invitation by accepting or declining it.
     *
     * @param invitationId the unique identifier of the invitation
     * @param accept true to accept the invitation, false to decline
     * @throws ResourceNotFoundException if the invitation is not found
     * @throws ForbiddenException if the invitation was not addressed to the current user
     * @throws ConflictException if the invitation status is not PENDING
     */
    public void respondInvite(UUID invitationId, boolean accept) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Verify the invitation belongs to the current user
        if (!invitation.getInvitedUser().getId().equals(currentUserId))
            throw new ForbiddenException("Not your invitation");

        if (invitation.getStatus() != InvitationStatus.PENDING)
            throw new ConflictException("Invitation already responded");

        // Handle acceptance: add user as project contributor
        if (accept) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            ProjectMember member = ProjectMember.builder()
                    .id(new ProjectMemberId(currentUserId, invitation.getProject().getId()))
                    .user(invitation.getInvitedUser())
                    .project(invitation.getProject())
                    .role(ProjectRole.CONTRIBUTOR)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            projectMemberRepository.save(member);
        } else {
            invitation.setStatus(InvitationStatus.DECLINED);
        }

        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        // Notify the inviter of the response
        Notification notification = Notification.builder()
                .user(invitation.getInvitedBy())
                .sender(invitation.getInvitedUser())
                .title(accept ? "Invitation Accepted" : "Invitation Declined")
                .message(invitation.getInvitedUser().getProfileName()
                        + (accept ? " accepted" : " declined")
                        + " your invitation to \"" + invitation.getProject().getProjectName() + "\"")
                .isRead(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                invitation.getInvitedBy().getId().toString(),
                "/queue/notifications",
                Map.of(
                        "type", accept ? "INVITATION_ACCEPTED" : "INVITATION_DECLINED",
                        "notificationId", notification.getId(),
                        "invitationId", invitation.getId(),
                        "projectId", invitation.getProject().getId(),
                        "projectName", invitation.getProject().getProjectName(),
                        "respondedBy", invitation.getInvitedUser().getProfileName()
                )
        );
    }

    /**
     * Retrieves all pending invitations for the current user.
     *
     * @return a list of InvitationResponse DTOs representing pending invitations
     */
    public List<InvitationResponse> getPendingInvites() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return invitationRepository
                .findByInvitedUserIdAndStatus(currentUserId, InvitationStatus.PENDING)
                .stream()
                .map(inv -> InvitationResponse.builder()
                        .id(inv.getId())
                        .projectId(inv.getProject().getId())
                        .projectName(inv.getProject().getProjectName())
                        .invitedById(inv.getInvitedBy().getId())
                        .invitedByName(inv.getInvitedBy().getProfileName())
                        .status(inv.getStatus().name())
                        .createdAt(inv.getCreatedAt())
                        .build())
                .toList();
    }
}