package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.search.SearchResponseDto;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.UnauthorizedException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final FileRepository fileRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public SearchResponseDto search(String query) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        if (query == null || query.trim().isEmpty()) {
            return SearchResponseDto.builder()
                    .projects(List.of())
                    .issues(List.of())
                    .files(List.of())
                    .build();
        }

        String searchPattern = query.trim();

        // 1. Search Projects
        List<Project> projects = projectRepository.searchProjectsForUser(currentUserId, searchPattern);
        List<SearchResponseDto.ProjectResult> projectResults = projects.stream()
                .map(p -> SearchResponseDto.ProjectResult.builder()
                        .id(p.getId())
                        .projectName(p.getProjectName())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 2. Search Issues
        List<Issue> issues = issueRepository.searchIssuesForUser(currentUserId, searchPattern);
        List<SearchResponseDto.IssueResult> issueResults = issues.stream()
                .map(i -> SearchResponseDto.IssueResult.builder()
                        .id(i.getId())
                        .issueName(i.getIssueName())
                        .description(i.getDescription())
                        .issueType(i.getIssueType() != null ? i.getIssueType().getName() : null)
                        .status(i.getStatus() != null ? i.getStatus().getStatusName() : null)
                        .projectId(i.getProject().getId())
                        .projectName(i.getProject().getProjectName())
                        .build())
                .collect(Collectors.toList());

        // 3. Search Files
        List<File> files = fileRepository.searchFilesForUser(currentUserId, searchPattern);
        List<SearchResponseDto.FileResult> fileResults = files.stream()
                .map(f -> {
                    UUID issueId = f.getIssue() != null ? f.getIssue().getId() : null;
                    UUID projectId = null;
                    String projectName = null;
                    if (f.getIssue() != null && f.getIssue().getProject() != null) {
                        projectId = f.getIssue().getProject().getId();
                        projectName = f.getIssue().getProject().getProjectName();
                    }
                    return SearchResponseDto.FileResult.builder()
                            .id(f.getId())
                            .originalName(f.getOriginalName())
                            .mimeType(f.getMimeType())
                            .issueId(issueId)
                            .projectId(projectId)
                            .projectName(projectName)
                            .build();
                })
                .collect(Collectors.toList());

        return SearchResponseDto.builder()
                .projects(projectResults)
                .issues(issueResults)
                .files(fileResults)
                .build();
    }
}
