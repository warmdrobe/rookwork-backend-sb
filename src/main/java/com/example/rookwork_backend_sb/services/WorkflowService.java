package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.workflow.*;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.BadRequestException;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.*;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final StatusTransitionRepository transitionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStatusRepository statusRepository;
    private final ProjectMemberRepository memberRepository;
    private final SecurityUtil securityUtil;

    // ──────────────────────────────────────────────────────────────────────
    // Read
    // ──────────────────────────────────────────────────────────────────────

    public WorkflowResponse getWorkflow(UUID projectId) {
        UUID userId = securityUtil.getCurrentUserId();
        requireMember(userId, projectId);

        List<StatusTransition> transitions = transitionRepository.findAllByProjectId(projectId);
        long count = transitionRepository.countByProjectId(projectId);

        List<TransitionDto> dtos = transitions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return WorkflowResponse.builder()
                .projectId(projectId)
                .transitions(dtos)
                .openWorkflow(count == 0)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Write
    // ──────────────────────────────────────────────────────────────────────

    @Transactional
    public WorkflowResponse replaceWorkflow(UUID projectId, BulkWorkflowRequest request) {
        UUID userId = securityUtil.getCurrentUserId();
        requireOwner(userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Clear existing transitions
        transitionRepository.deleteAllByProjectId(projectId);
        transitionRepository.flush();

        // Save new transitions
        if (request.getTransitions() != null) {
            List<AddTransitionRequest> distinctTransitions = request.getTransitions().stream()
                    .distinct()
                    .collect(Collectors.toList());

            for (AddTransitionRequest req : distinctTransitions) {
                if (req.getFromStatusId().equals(req.getToStatusId())) {
                    throw new BadRequestException("Self-transitions (moving within the same status) are not allowed in workflow config");
                }

                ProjectStatus fromStatus = requireStatusBelongsToProject(req.getFromStatusId(), projectId);
                ProjectStatus toStatus = requireStatusBelongsToProject(req.getToStatusId(), projectId);

                StatusTransition transition = StatusTransition.builder()
                        .project(project)
                        .fromStatus(fromStatus)
                        .toStatus(toStatus)
                        .build();

                transitionRepository.save(transition);
            }
        }

        // Return updated workflow
        List<StatusTransition> transitions = transitionRepository.findAllByProjectId(projectId);
        List<TransitionDto> dtos = transitions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return WorkflowResponse.builder()
                .projectId(projectId)
                .transitions(dtos)
                .openWorkflow(dtos.isEmpty())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Validation Guard for Issues
    // ──────────────────────────────────────────────────────────────────────

    public void validateTransition(UUID projectId, UUID fromStatusId, UUID toStatusId) {
        // Self-moves within the same column are always allowed at the logic level (they are actually card reorders)
        if (fromStatusId.equals(toStatusId)) {
            return;
        }

        long count = transitionRepository.countByProjectId(projectId);
        if (count == 0) {
            // Open workflow: anything allowed
            return;
        }

        boolean allowed = transitionRepository.existsByProjectIdAndFromStatusIdAndToStatusId(projectId, fromStatusId, toStatusId);
        if (!allowed) {
            throw new BadRequestException("Transition is not allowed by this project's workflow rules.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private TransitionDto toDto(StatusTransition st) {
        return TransitionDto.builder()
                .id(st.getId())
                .fromStatusId(st.getFromStatus().getId())
                .fromStatusName(st.getFromStatus().getStatusName())
                .toStatusId(st.getToStatus().getId())
                .toStatusName(st.getToStatus().getStatusName())
                .build();
    }

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
            throw new ForbiddenException("Only the project OWNER can configure workflows");
        }
    }

    private ProjectStatus requireStatusBelongsToProject(UUID statusId, UUID projectId) {
        return statusRepository.findByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status " + statusId + " not found in project " + projectId));
    }
}
