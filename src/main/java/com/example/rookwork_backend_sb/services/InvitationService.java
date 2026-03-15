package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.invitations.InvitationResponse;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.InvitationRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
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

    /// Send invite
    public void sendInvite(UUID projectId, String invitedEmail) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        // Kiểm tra người gửi có phải OWNER không
        ProjectMember sender = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        if (sender.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only OWNER can invite members");
        }

        // Tìm user được invite
        User invitedUser = userRepository.findByEmail(invitedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Kiểm tra đã là member chưa
        if (projectMemberRepository.findById(
                new ProjectMemberId(invitedUser.getId(), projectId)).isPresent()) {
            throw new ConflictException("User is already a member");
        }

        // Kiểm tra đã có invite pending chưa
        if (invitationRepository.findByProjectIdAndInvitedUserId(
                projectId, invitedUser.getId()).isPresent()) {
            throw new ConflictException("Invitation already sent");
        }

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

        // Gửi real-time notification
        messagingTemplate.convertAndSendToUser(
                invitedUser.getId().toString(),
                "/queue/notifications",
                Map.of(
                        "type", "INVITATION",
                        "invitationId", invitation.getId(),
                        "projectName", project.getProjectName(),
                        "invitedBy", sender.getUser().getProfileName()
                )
        );
    }

    /// Response (true - false)
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

        // Notify OWNER real-time khi đối phương respond
        messagingTemplate.convertAndSendToUser(
                invitation.getInvitedBy().getId().toString(),
                "/queue/notifications",
                Map.of(
                        "type", accept ? "INVITATION_ACCEPTED" : "INVITATION_DECLINED",
                        "invitationId", invitation.getId(),
                        "projectId", invitation.getProject().getId(),
                        "projectName", invitation.getProject().getProjectName(),
                        "respondedBy", invitation.getInvitedUser().getProfileName()
                )
        );
    }

    /// Get pending invites
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
