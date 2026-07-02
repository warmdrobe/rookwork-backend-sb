package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.projectstatus.*;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ConflictException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.ProjectStatusRepository;
import com.example.rookwork_backend_sb.repositories.StatusTransitionRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages the dynamic workflow columns (statuses) of a project.
 *
 * <p>All write operations require the caller to be an OWNER of the project.
 * A project may have at most {@link #MAX_STATUSES_PER_PROJECT} status columns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectStatusService {

    /** Maximum number of status columns per project. Enforced at service layer AND should be reflected in UI. */
    public static final int MAX_STATUSES_PER_PROJECT = 5;

    private final ProjectStatusRepository statusRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final IssueRepository issueRepository;
    private final StatusTransitionRepository transitionRepository;
    private final SecurityUtil securityUtil;

    // ──────────────────────────────────────────────────────────────────────
    // Read
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns all status columns for a project ordered by position.
     * Any authenticated project member may call this endpoint.
     */
    public List<ProjectStatusResponse> listStatuses(UUID projectId) {
        UUID userId = securityUtil.getCurrentUserId();
        requireMember(userId, projectId);

        return statusRepository.findAllByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Create
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Adds a new status column at the end of the project's Kanban board.
     *
     * @throws BadRequestException if the project already has {@value #MAX_STATUSES_PER_PROJECT} statuses
     * @throws ForbiddenException  if the caller is not an OWNER of the project
     */
    @Transactional
    public ProjectStatusResponse createStatus(UUID projectId, CreateStatusRequest request) {
        UUID userId = securityUtil.getCurrentUserId();
        requireOwner(userId, projectId);

        long count = statusRepository.countByProjectId(projectId);
        if (count >= MAX_STATUSES_PER_PROJECT) {
            throw new BadRequestException(
                    "Project has reached the maximum of " + MAX_STATUSES_PER_PROJECT + " status columns");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        int nextPosition = statusRepository.findMaxPositionByProjectId(projectId)
                .map(max -> max + 1)
                .orElse(1);

        ProjectStatus status = ProjectStatus.builder()
                .statusName(request.getStatusName())
                .color(request.getColor() != null ? request.getColor() : "#94a3b8")
                .position(nextPosition)
                .statusCategory(request.getStatusCategory())
                .project(project)
                .build();

        statusRepository.save(status);
        log.info("Created status '{}' (pos={}) for project {}", status.getStatusName(), nextPosition, projectId);
        return toResponse(status);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Update
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Updates the display name and/or color of an existing status column.
     */
    @Transactional
    public ProjectStatusResponse updateStatus(UUID projectId, UUID statusId, UpdateStatusRequest request) {
        UUID userId = securityUtil.getCurrentUserId();
        requireOwner(userId, projectId);

        ProjectStatus status = requireStatusBelongsToProject(statusId, projectId);

        if (request.getStatusName() != null && !request.getStatusName().isBlank()) {
            status.setStatusName(request.getStatusName());
        }
        if (request.getColor() != null) {
            status.setColor(request.getColor());
        }
        if (request.getStatusCategory() != null) {
            status.setStatusCategory(request.getStatusCategory());
        }

        statusRepository.save(status);
        return toResponse(status);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Reorder (Drag-and-Drop)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Reorders multiple status columns in one batch.
     *
     * <p>Strategy to avoid violating the UNIQUE(project_id, position) constraint during the update:
     * <ol>
     *   <li>Set all targeted statuses to temporary negative positions.</li>
     *   <li>Flush to DB (writes negatives, no conflict with existing positives).</li>
     *   <li>Apply the final positive positions and flush again.</li>
     * </ol>
     *
     * <p>Optimistic locking: if any status's {@code version} in the request does not match the DB,
     * Spring throws {@link OptimisticLockingFailureException} which is mapped to HTTP 409 by the
     * global exception handler.
     *
     * @throws ForbiddenException if the caller is not an OWNER
     */
    @Transactional
    public List<ProjectStatusResponse> reorderStatuses(UUID projectId, ReorderStatusRequest request) {
        UUID userId = securityUtil.getCurrentUserId();
        requireOwner(userId, projectId);

        List<ReorderStatusRequest.StatusOrder> orders = request.getStatusOrders();

        // Step 1: assign temporary negative positions to avoid UNIQUE constraint violations
        for (int i = 0; i < orders.size(); i++) {
            ReorderStatusRequest.StatusOrder order = orders.get(i);
            ProjectStatus status = requireStatusBelongsToProject(order.getStatusId(), projectId);

            // Version check happens here via @Version — mismatched version -> OptimisticLockingFailureException
            if (status.getVersion() != order.getVersion()) {
                throw new ConflictException(
                        "Status " + order.getStatusId() + " was modified concurrently. Please refresh and try again.");
            }

            status.setPosition(-(i + 1)); // temporary negative
            statusRepository.save(status);
        }
        statusRepository.flush();

        // Step 2: apply final positive positions
        for (ReorderStatusRequest.StatusOrder order : orders) {
            ProjectStatus status = requireStatusBelongsToProject(order.getStatusId(), projectId);
            status.setPosition(order.getPosition());
            statusRepository.save(status);
        }
        statusRepository.flush();

        return statusRepository.findAllByProjectIdOrderByPositionAsc(projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Delete with Fallback
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Deletes a status column and migrates all its issues to {@code fallbackStatusId}.
     *
     * <p>The entire operation (migrate + delete + compact positions) runs in a single transaction.
     * If anything fails mid-way, no changes are committed to the database.
     *
     * @throws BadRequestException if fallbackStatusId == statusId or fallback doesn't belong to same project
     * @throws ForbiddenException  if the caller is not an OWNER
     */
    @Transactional
    public void deleteStatus(UUID projectId, UUID statusId, DeleteStatusRequest request) {
        UUID userId = securityUtil.getCurrentUserId();
        requireOwner(userId, projectId);

        UUID fallbackId = request.getFallbackStatusId();

        if (statusId.equals(fallbackId)) {
            throw new BadRequestException("Fallback status must be different from the status being deleted");
        }

        ProjectStatus toDelete  = requireStatusBelongsToProject(statusId, projectId);
        ProjectStatus fallback  = requireStatusBelongsToProject(fallbackId, projectId);

        // 1. Migrate all issues from the deleted status to the fallback status
        List<Issue> affectedIssues = issueRepository.findAllByProjectId(projectId)
                .stream()
                .filter(i -> i.getStatus() != null && i.getStatus().getId().equals(statusId))
                .collect(Collectors.toList());

        affectedIssues.forEach(issue -> issue.setStatus(fallback));
        issueRepository.saveAll(affectedIssues);
        issueRepository.flush();

        log.info("Migrated {} issues from status '{}' to '{}' in project {}",
                affectedIssues.size(), toDelete.getStatusName(), fallback.getStatusName(), projectId);

        // 1.5. Clean up any workflow transitions referencing the deleted status
        transitionRepository.deleteAllByFromStatusIdOrToStatusId(statusId, statusId);
        transitionRepository.flush();

        // 2. Delete the status column
        int deletedPosition = toDelete.getPosition();
        statusRepository.delete(toDelete);
        statusRepository.flush();

        // 3. Compact positions: shift down all statuses that were after the deleted one
        //    Use negative-then-positive strategy to avoid transient UNIQUE constraint violations.
        List<ProjectStatus> remaining = statusRepository.findAllByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .filter(s -> s.getPosition() > deletedPosition)
                .collect(Collectors.toList());

        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(-(i + 1));
            statusRepository.save(remaining.get(i));
        }
        statusRepository.flush();

        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(deletedPosition + i);
            statusRepository.save(remaining.get(i));
        }

        log.info("Deleted status '{}' from project {}", toDelete.getStatusName(), projectId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Package-scoped helper: seed default statuses on project creation
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Seeds the three default status columns ("To Do", "In Progress", "Done") for a newly created project.
     * Called internally by {@link ProjectService} immediately after persisting the new project.
     */
    @Transactional
    public void seedDefaultStatuses(Project project) {
        Object[][] defaults = {
            {"To Do",       "#94a3b8", 1, StatusCategory.TO_DO},
            {"In Progress", "#3b82f6", 2, StatusCategory.IN_PROGRESS},
            {"Done",        "#10b981", 3, StatusCategory.DONE}
        };

        for (Object[] row : defaults) {
            ProjectStatus s = ProjectStatus.builder()
                    .statusName((String) row[0])
                    .color((String) row[1])
                    .position((int) row[2])
                    .statusCategory((StatusCategory) row[3])
                    .project(project)
                    .build();
            statusRepository.save(s);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Mapper
    // ──────────────────────────────────────────────────────────────────────

    public ProjectStatusResponse toResponse(ProjectStatus s) {
        return ProjectStatusResponse.builder()
                .id(s.getId())
                .statusName(s.getStatusName())
                .color(s.getColor())
                .position(s.getPosition())
                .statusCategory(s.getStatusCategory())
                .version(s.getVersion())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private guards
    // ──────────────────────────────────────────────────────────────────────

    private void requireMember(UUID userId, UUID projectId) {
        if (!memberRepository.existsById(new ProjectMemberId(userId, projectId))) {
            throw new ForbiddenException("You are not a member of this project");
        }
    }

    private void requireOwner(UUID userId, UUID projectId) {
        ProjectMember member = memberRepository
                .findById(new ProjectMemberId(userId, projectId))
                .orElseThrow(() -> new ForbiddenException("You are not a member of this project"));
        if (member.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only the project OWNER can manage workflow statuses");
        }
    }

    private ProjectStatus requireStatusBelongsToProject(UUID statusId, UUID projectId) {
        return statusRepository.findByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status " + statusId + " not found in project " + projectId));
    }
}
