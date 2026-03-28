package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.invitations.InvitationResponse;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public void sendInvite(UUID projectId, String invitedEmail) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        ProjectMember sender = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        if (sender.getRole() != ProjectRole.OWNER)
            throw new ForbiddenException("Only OWNER can invite members");

        User invitedUser = userRepository.findByEmail(invitedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (projectMemberRepository.findById(
                new ProjectMemberId(invitedUser.getId(), projectId)).isPresent())
            throw new ConflictException("User is already a member");

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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        invitationRepository.save(invitation);

        Notification notification = Notification.builder()
                .user(invitedUser)
                .sender(sender.getUser())
                .title("Project Invitation")
                .message(sender.getUser().getProfileName()
                        + " invited you to join \"" + project.getProjectName() + "\"")
                .invitation(invitation)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
    }

    public void respondInvite(UUID invitationId, boolean accept) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(currentUserId))
            throw new ForbiddenException("Not your invitation");

        if (invitation.getStatus() != InvitationStatus.PENDING)
            throw new ConflictException("Invitation already responded");

        if (accept) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            ProjectMember member = ProjectMember.builder()
                    .id(new ProjectMemberId(currentUserId, invitation.getProject().getId()))
                    .user(invitation.getInvitedUser())
                    .project(invitation.getProject())
                    .role(ProjectRole.CONTRIBUTOR)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            projectMemberRepository.save(member);
        } else {
            invitation.setStatus(InvitationStatus.DECLINED);
        }

        invitation.setUpdatedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        Notification notification = Notification.builder()
                .user(invitation.getInvitedBy())
                .sender(invitation.getInvitedUser())
                .title(accept ? "Invitation Accepted" : "Invitation Declined")
                .message(invitation.getInvitedUser().getProfileName()
                        + (accept ? " accepted" : " declined")
                        + " your invitation to \"" + invitation.getProject().getProjectName() + "\"")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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