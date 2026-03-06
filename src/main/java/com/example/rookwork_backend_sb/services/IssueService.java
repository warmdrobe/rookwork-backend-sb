package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.Dtos.issues.CreateIssueRequest;
import com.example.rookwork_backend_sb.Dtos.issues.IssueResponse;
import com.example.rookwork_backend_sb.Entities.*;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final SecurityUtil securityUtil;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ActivityService activityService;
    private final UserRepository userRepository;

    @Transactional
    public IssueResponse createIssue(UUID projectId, CreateIssueRequest request) {
        //Check user
        UUID currentUserId = securityUtil.getCurrentUserId();


        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check membership
        projectMemberRepository
                .findById(new ProjectMemberId(currentUserId, projectId))
                .orElseThrow(() -> new RuntimeException("Not a member of this project"));

        // Get project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Create issue
        Issue issue = Issue.builder()
                .issueName(request.getIssueName())
                .description(request.getDescription())
                .issueType(request.getIssueType())
                .priority(request.getPriority())
                .status(request.getStatus())
                .deadline(request.getDeadline())
                .project(project)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        issueRepository.save(issue);

        activityService.log(
            issue.getProject(),
            currentUser,
            ActivityAction.CREATED,
            ActivityEntityType.ISSUE,
            issue.getId(),
            issue.getIssueName(),
            null
        );

        // Map response
        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setIssueName(issue.getIssueName());
        response.setDescription(issue.getDescription());
        response.setIssueType(issue.getIssueType());
        response.setPriority(issue.getPriority());
        response.setStatus(issue.getStatus());
        response.setParentId(issue.getParent().getId());
        response.setProjectId(projectId);
        response.setDeadline(issue.getDeadline());
        response.setCreatedAt(issue.getCreatedAt());
        response.setUpdatedAt(issue.getUpdatedAt());

        return response;
    }
}
