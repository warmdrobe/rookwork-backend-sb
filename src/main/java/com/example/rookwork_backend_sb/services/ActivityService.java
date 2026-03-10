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

}
