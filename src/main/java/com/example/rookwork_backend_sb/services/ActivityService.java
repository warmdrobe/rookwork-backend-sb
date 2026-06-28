package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.activities.ActivityResponse;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.repositories.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for tracking and logging project activity history.
 */
@RequiredArgsConstructor
@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Logs a new activity entry to the database.
     *
     * @param project the project where the action occurred
     * @param actor the user who performed the action
     * @param action the type of action performed
     * @param entityType the type of entity affected by the action
     * @param entityId the unique identifier of the affected entity
     * @param entityName the display name of the affected entity
     * @param metadata additional JSON metadata associated with the action
     */
    public void log (Project project, User actor,
                ActivityAction action, ActivityEntityType entityType,
                UUID entityId, String entityName,
                Map<String, Object> metadata){
        log(project, actor, action, entityType, entityId, entityName, null, metadata);
    }

    public void log (Project project, User actor,
                ActivityAction action, ActivityEntityType entityType,
                UUID entityId, String entityName, Issue issue,
                Map<String, Object> metadata){
        String metadataJson = null;
        if (metadata != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize activity metadata", e);
            }
        }
        Activity activity = Activity.builder()
                .project(project)
                .actor(actor)
                .actionType(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .issue(issue)
                .metadata(metadataJson)
                .createdAt(Instant.now())
                .build();
        activityRepository.save(activity);

        // Broadcast real-time activity update
        try {
            ActivityResponse response = toResponse(activity);
            if (issue != null) {
                simpMessagingTemplate.convertAndSend(
                        "/topic/project/" + project.getId() + "/issue/" + issue.getId() + "/activities",
                        (Object) Map.of("type", "NEW_ACTIVITY", "activity", response)
                );
            }
            simpMessagingTemplate.convertAndSend(
                    "/topic/project/" + project.getId() + "/activities",
                    (Object) Map.of("type", "NEW_ACTIVITY", "activity", response)
            );
        } catch (Exception e) {
            // Log warning but don't fail transaction for WebSocket issues
            System.err.println("Failed to broadcast activity update: " + e.getMessage());
        }
    }

    /**
     * Retrieves recent raw activity logs for a project.
     *
     * @param projectId the unique identifier of the project
     * @param limit the maximum number of activity logs to retrieve
     * @return a list of activities matching the project sorted by creation date descending
     */
    public List<Activity> getProjectActivities(UUID projectId, int limit) {
        return activityRepository.findByProjectIdOrderByCreatedAtDesc(
                projectId, PageRequest.of(0, limit)
        );
    }

    //// use activity log

    /// Khi sửa issue
    // Trong IssueService khi update status
    // activityService.log(
    //    issue.getProject(),
    //    currentUser,
    //    "moved",
    //    "issue",
    //    issue.getId(),
    //    issue.getIssueName(),
    //    "{\"from\": \"In Progress\", \"to\": \"Done\"}"
    //);
    //
    //// Khi tạo issue mới
    // activityService.log(
    //    issue.getProject(),
    //    currentUser,
    //    "created",
    //    "issue",
    //    issue.getId(),
    //    issue.getIssueName(),
    //    null
    //);

    /// Khi tạo comment
    //activityService.log(
    //    comment.getIssue().getProject(),
    //    currentUser,
    //    ActivityAction.COMMENTED,
    //    ActivityEntityType.COMMENT,
    //    comment.getId(),
    //     comment.getIssue().getIssueName(),  // entityName = tên issue được comment
    //    "{\"preview\": \"" + truncate(comment.getContent(), 50) + "\"}"
    //);

    /// Khi xóa comment
    //activityService.log(
    //    comment.getIssue().getProject(),
    //    currentUser,
    //    ActivityAction.DELETED,
    //    ActivityEntityType.COMMENT,
    //    comment.getId(),
    //    comment.getIssue().getIssueName(),
    //    null
    //);

    /// Khi gửi invitation
    //activityService.log(
    //    project,
    //    inviter,
    //    ActivityAction.INVITED,
    //    ActivityEntityType.INVITATION,
    //    invitation.getId(),
    //    invitee.getEmail(),   // entityName = email người được mời
    //    "{\"role\": \"" + invitation.getRole() + "\"}"
    //);
    //
    //// Khi accept
    //activityService.log(
    //    project,
    //    currentUser,
    //    ActivityAction.ACCEPTED,
    //    ActivityEntityType.INVITATION,
    //    invitation.getId(),
    //    currentUser.getEmail(),
    //    null
    //);
    //
    //// Khi decline
    //activityService.log(
    //    project,
    //    currentUser,
    //    ActivityAction.DECLINED,
    //    ActivityEntityType.INVITATION,
    //    invitation.getId(),
    //    currentUser.getEmail(),
    //    null
    //);

    /**
     * Retrieves recent activities for a project formatted as response DTOs.
     *
     * @param projectId the unique identifier of the project
     * @param limit the maximum number of activities to retrieve
     * @return a list of ActivityResponse DTOs
     */
    public List<ActivityResponse> getProjectActivityResponses(UUID projectId, int limit) {
        return getProjectActivities(projectId, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves activity history specific to a given issue.
     *
     * @param projectId the unique identifier of the project
     * @param issueId the unique identifier of the issue
     * @param limit the maximum number of activity logs to retrieve
     * @return a list of ActivityResponse DTOs related to the issue
     */
    public List<ActivityResponse> getIssueActivity(UUID projectId, UUID issueId, int limit) {
        return activityRepository
                .findIssueActivities(projectId, issueId, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Map Activity entity → ActivityResponse DTO */
    private ActivityResponse toResponse(Activity a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .actorName(a.getActor().getProfileName())
                .actorPicture(s3Service.getAvatarUrl(a.getActor().getPicture()))
                .actionType(a.getActionType().name())
                .entityType(a.getEntityType().name())
                .entityId(a.getEntityId())
                .entityName(a.getEntityName())
                .metadata(a.getMetadata())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
