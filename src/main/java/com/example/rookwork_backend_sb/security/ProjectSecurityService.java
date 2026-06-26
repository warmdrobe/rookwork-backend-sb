package com.example.rookwork_backend_sb.security;

import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.entities.ProjectRole;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Central security service for project-level authorization.
 * Used by @PreAuthorize expressions throughout the application.
 *
 * Usage in controllers:
 *   @PreAuthorize("@projectSecurity.isMember(#projectId)")
 *   @PreAuthorize("@projectSecurity.isOwner(#projectId)")
 *   @PreAuthorize("@projectSecurity.isMemberOfIssue(#issueId)")
 */
@Component("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurityService {

    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;
    private final SecurityUtil securityUtil;

    /**
     * Kiểm tra user hiện tại có là thành viên của project không (bất kỳ role nào).
     * Dùng cho các endpoint đọc/ghi dữ liệu yêu cầu membership.
     *
     * @param projectId the project to check membership for
     * @return true if user is a member of the project
     */
    public boolean isMember(UUID projectId) {
        UUID userId = securityUtil.getCurrentUserId();
        return projectMemberRepository.existsById(new ProjectMemberId(userId, projectId));
    }

    /**
     * Kiểm tra user hiện tại có phải OWNER của project không.
     * Dùng cho các endpoint nhạy cảm: xóa project, xóa issue, kick member, v.v.
     *
     * @param projectId the project to check ownership for
     * @return true if user is the OWNER of the project
     */
    public boolean isOwner(UUID projectId) {
        UUID userId = securityUtil.getCurrentUserId();
        return projectMemberRepository.existsByIdAndRole(
                new ProjectMemberId(userId, projectId), ProjectRole.OWNER);
    }

    /**
     * Kiểm tra user hiện tại có là thành viên của project chứa issue này không.
     * Dùng cho các endpoint chỉ có {issueId} trong URL (không có {projectId}),
     * giúp phân quyền ở Controller mà không cần thay đổi URL ở frontend.
     *
     * @param issueId the issue whose project membership should be checked
     * @return true if user is a member of the project that owns the issue
     */
    public boolean isMemberOfIssue(UUID issueId) {
        UUID userId = securityUtil.getCurrentUserId();
        return issueRepository.findById(issueId)
                .map(issue -> projectMemberRepository.existsById(
                        new ProjectMemberId(userId, issue.getProject().getId())))
                .orElse(false); // Issue not found → deny access (404 will be thrown in service)
    }
}
