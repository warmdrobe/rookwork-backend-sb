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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing project invitation workflows (sending invites, responding, and list queries).
 */
@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SecurityUtil securityUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url:https://www.rookwork.asia}")
    private String frontendUrl;
    /**
     * Sends a project invitation to a user by email.
     *
     * @param projectId the unique identifier of the project
     * @param invitedEmail the email of the user to invite
     * @throws ForbiddenException if current user is not a project owner
     * @throws ResourceNotFoundException if user or project is not found
     * @throws ConflictException if user is already a member or invitation is already pending
     */
    @org.springframework.transaction.annotation.Transactional
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
                .orElseGet(() -> {
                    User placeholder = User.builder()
                            .email(invitedEmail)
                            .profileName(invitedEmail.split("@")[0])
                            .isActive(false)
                            .isVerified(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return userRepository.save(placeholder);
                });

        // Prevent inviting users who are already members
        if (projectMemberRepository.findById(
                new ProjectMemberId(invitedUser.getId(), projectId)).isPresent())
            throw new ConflictException("User is already a member");

        // Prevent duplicate invitations to the same user
        java.util.Optional<Invitation> existingInviteOpt = invitationRepository.findByProjectIdAndInvitedUserId(projectId, invitedUser.getId());
        if (existingInviteOpt.isPresent()) {
            Invitation existingInvite = existingInviteOpt.get();
            if (existingInvite.getStatus() == InvitationStatus.PENDING) {
                Instant oneHourAgo = Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS);
                if (existingInvite.getUpdatedAt() != null && existingInvite.getUpdatedAt().isAfter(oneHourAgo)) {
                    throw new ConflictException("You can only resend the invitation after 1 hour");
                }
                
                // Allow re-invite by updating updatedAt and triggering notification and email again
                existingInvite.setUpdatedAt(Instant.now());
                invitationRepository.save(existingInvite);
                
                sendInvitationNotifications(existingInvite, sender.getUser(), invitedUser, projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found")));
                return;
            } else {
                // Delete notifications pointing to the old invitation
                notificationRepository.deleteByInvitationId(existingInvite.getId());
                // Delete the old invitation record
                invitationRepository.delete(existingInvite);
                invitationRepository.flush();
            }
        }

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
        
        sendInvitationNotifications(invitation, sender.getUser(), invitedUser, project);
    }

    private void sendInvitationNotifications(Invitation invitation, User sender, User invitedUser, Project project) {
        String invitationUrl;
        if (invitedUser.isActive()) {
            invitationUrl = frontendUrl + "/dashboard?acceptInvitationId=" + invitation.getId();
        } else {
            invitationUrl = frontendUrl + "/register?invitationId=" + invitation.getId();
        }

        log.info("[PROJECT INVITATION] Generated link for testing on localhost: {}", invitationUrl);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.sendProjectInvitation(
                                invitedUser.getEmail(),
                                project.getProjectName(),
                                sender.getProfileName(),
                                invitationUrl,
                                !invitedUser.isActive()
                        );
                    }
                }
            );
        } else {
            emailService.sendProjectInvitation(
                    invitedUser.getEmail(),
                    project.getProjectName(),
                    sender.getProfileName(),
                    invitationUrl,
                    !invitedUser.isActive()
            );
        }
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
    @org.springframework.transaction.annotation.Transactional
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

        if (accept) {
            notifyProjectMembersNewMember(invitation.getInvitedUser(), invitation.getProject());
        }
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

    /**
     * Retrieves all pending invitations for a specific project.
     *
     * @param projectId the unique identifier of the project
     * @return a list of InvitationResponse DTOs representing pending invitations for the project
     */
    public List<InvitationResponse> getPendingInvitesForProject(UUID projectId) {
        return invitationRepository
                .findByProjectIdAndStatus(projectId, InvitationStatus.PENDING)
                .stream()
                .map(inv -> InvitationResponse.builder()
                        .id(inv.getId())
                        .projectId(inv.getProject().getId())
                        .projectName(inv.getProject().getProjectName())
                        .invitedById(inv.getInvitedBy().getId())
                        .invitedByName(inv.getInvitedBy().getProfileName())
                        .invitedUserId(inv.getInvitedUser().getId())
                        .invitedUserName(inv.getInvitedUser().getProfileName())
                        .invitedUserEmail(inv.getInvitedUser().getEmail())
                        .invitedUserPicture(inv.getInvitedUser().getPicture())
                        .status(inv.getStatus().name())
                        .createdAt(inv.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Cancels a pending project invitation.
     *
     * @param invitationId the unique identifier of the invitation to cancel
     * @throws ResourceNotFoundException if the invitation is not found
     * @throws ForbiddenException if current user is not project owner
     * @throws ConflictException if invitation status is not PENDING
     */
    @org.springframework.transaction.annotation.Transactional
    public void cancelInvite(UUID invitationId) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING)
            throw new ConflictException("Can only cancel pending invitations");

        // Verify sender is project owner
        UUID projectId = invitation.getProject().getId();
        ProjectMember sender = projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new ForbiddenException("Not a member of this project"));

        if (sender.getRole() != ProjectRole.OWNER)
            throw new ForbiddenException("Only OWNER can cancel invitations");

        // Delete notifications first
        notificationRepository.deleteByInvitationId(invitationId);

        // Delete invitation
        invitationRepository.delete(invitation);
    }

    public void notifyProjectMembersNewMember(User newMember, Project project) {
        List<ProjectMember> members = projectMemberRepository.findAllByProject_Id(project.getId());
        for (ProjectMember existingMember : members) {
            if (existingMember.getUser().getId().equals(newMember.getId())) continue;

            Notification notification = Notification.builder()
                    .user(existingMember.getUser())
                    .sender(newMember)
                    .title("New Member Joined")
                    .message(newMember.getProfileName() + " joined the project \"" + project.getProjectName() + "\"")
                    .isRead(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            notificationRepository.save(notification);

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            messagingTemplate.convertAndSendToUser(
                                    existingMember.getUser().getId().toString(),
                                    "/queue/notifications",
                                    Map.of(
                                            "type", "MEMBER_JOINED",
                                            "notificationId", notification.getId(),
                                            "projectId", project.getId(),
                                            "projectName", project.getProjectName(),
                                            "memberName", newMember.getProfileName()
                                    )
                            );
                        }
                    }
                );
            } else {
                messagingTemplate.convertAndSendToUser(
                        existingMember.getUser().getId().toString(),
                        "/queue/notifications",
                        Map.of(
                                "type", "MEMBER_JOINED",
                                "notificationId", notification.getId(),
                                "projectId", project.getId(),
                                "projectName", project.getProjectName(),
                                "memberName", newMember.getProfileName()
                        )
                );
            }
        }
    }
}