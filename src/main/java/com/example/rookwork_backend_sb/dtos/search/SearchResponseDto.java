package com.example.rookwork_backend_sb.dtos.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto {
    private List<ProjectResult> projects;
    private List<IssueResult> issues;
    private List<FileResult> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectResult {
        private UUID id;
        private String projectName;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueResult {
        private UUID id;
        private String issueName;
        private String description;
        private String issueType; // EPIC, STORY, TASK
        private String status;
        private UUID projectId;
        private String projectName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileResult {
        private UUID id;
        private String originalName;
        private String mimeType;
        private UUID issueId;
        private UUID projectId;
        private String projectName;
    }
}
