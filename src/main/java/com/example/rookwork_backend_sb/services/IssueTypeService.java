package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.issues.CreateIssueTypeRequest;
import com.example.rookwork_backend_sb.dtos.issues.IssueIconOption;
import com.example.rookwork_backend_sb.dtos.issues.IssueTypeResponse;
import com.example.rookwork_backend_sb.entities.IssueType;
import com.example.rookwork_backend_sb.entities.Project;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.IssueTypeRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class IssueTypeService {
    private final IssueTypeRepository issueTypeRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;

    private static final Set<String> SUPPORTED_ICONS = Set.of(
        "task", "story", "epic", "bug", "sparkles", "search", "wrench", "file-text", "test-tube", "life-buoy"
    );

    public List<IssueTypeResponse> getIssueTypes(UUID projectId) {
        return issueTypeRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueTypeResponse createIssueType(UUID projectId, CreateIssueTypeRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Issue type name cannot be empty");
        }
        if (request.getIconKey() == null || !SUPPORTED_ICONS.contains(request.getIconKey())) {
            throw new BadRequestException("Unsupported or empty icon key");
        }
        if (request.getColor() == null || request.getColor().trim().isEmpty()) {
            throw new BadRequestException("Color cannot be empty");
        }

        // Limit check
        long customCount = issueTypeRepository.findByProjectId(projectId).stream()
                .filter(t -> !t.isSystem())
                .count();
        if (customCount >= 3) {
            throw new BadRequestException("A project can have at most 3 custom issue types (6 total).");
        }

        // Unique name check
        if (issueTypeRepository.findByProjectIdAndNameIgnoreCase(projectId, request.getName().trim()).isPresent()) {
            throw new BadRequestException("An issue type with this name already exists in the project");
        }

        IssueType issueType = IssueType.builder()
                .project(project)
                .name(request.getName().trim())
                .description(request.getDescription())
                .iconKey(request.getIconKey())
                .color(request.getColor().trim())
                .isSystem(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        issueType = issueTypeRepository.save(issueType);
        return mapToResponse(issueType);
    }

    @Transactional
    public void deleteIssueType(UUID projectId, UUID issueTypeId) {
        IssueType issueType = issueTypeRepository.findByIdAndProjectId(issueTypeId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue type not found"));

        if (issueType.isSystem()) {
            throw new BadRequestException("Cannot delete system issue types");
        }

        long count = issueRepository.countByIssueTypeId(issueTypeId);
        if (count > 0) {
            throw new BadRequestException("Cannot delete issue type because it is currently used by " + count + " issue(s)");
        }

        issueTypeRepository.delete(issueType);
    }

    public List<IssueIconOption> getSupportedIcons() {
        return List.of(
            new IssueIconOption("bug", "Bug"),
            new IssueIconOption("sparkles", "Feature"),
            new IssueIconOption("search", "Research"),
            new IssueIconOption("wrench", "Refactor"),
            new IssueIconOption("file-text", "Documentation"),
            new IssueIconOption("test-tube", "Test"),
            new IssueIconOption("life-buoy", "Support")
        );
    }

    public IssueTypeResponse mapToResponse(IssueType issueType) {
        return IssueTypeResponse.builder()
                .id(issueType.getId())
                .name(issueType.getName())
                .description(issueType.getDescription())
                .iconKey(issueType.getIconKey())
                .color(issueType.getColor())
                .isSystem(issueType.isSystem())
                .build();
    }
}
