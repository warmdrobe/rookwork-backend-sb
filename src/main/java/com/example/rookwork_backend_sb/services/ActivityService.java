package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.repositories.ActivityRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    /// Log mapping config
    public void log (Project project, User actor,
                ActivityAction action, ActivityEntityType entityType,
                UUID entityId, String entityName,
                String metadata){
        Activity activity = Activity.builder()
                .project(project)
                .actor(actor)
                .actionType(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
        activityRepository.save(activity);
    }

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
}
