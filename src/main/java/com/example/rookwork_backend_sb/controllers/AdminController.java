package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminController {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;

    /** Guard: only admins may call any endpoint in this controller */
    private void requireAdmin() {
        UUID userId = securityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    // ─────────────────────────────────────────────
    // GET /api/admin/stats  — Overview dashboard
    // ─────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        requireAdmin();

        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.stream().filter(u -> u.getDeletedAt() == null).count();
        long activeUsers = allUsers.stream()
                .filter(u -> u.getDeletedAt() == null && u.isActive()).count();
        long newUsersLast7Days = allUsers.stream()
                .filter(u -> u.getDeletedAt() == null
                        && u.getCreatedAt() != null
                        && u.getCreatedAt().isAfter(sevenDaysAgo)).count();
        long newUsersLast30Days = allUsers.stream()
                .filter(u -> u.getDeletedAt() == null
                        && u.getCreatedAt() != null
                        && u.getCreatedAt().isAfter(thirtyDaysAgo)).count();

        long totalProjects = projectRepository.count();

        long totalIssues = issueRepository.count();
        // issues by status
        Map<String, Long> issuesByStatus = issueRepository.findAll().stream()
                .filter(i -> i.getStatus() != null)
                .collect(Collectors.groupingBy(i -> i.getStatus().name(), Collectors.counting()));
        // issues by priority
        Map<String, Long> issuesByPriority = issueRepository.findAll().stream()
                .filter(i -> i.getPriority() != null)
                .collect(Collectors.groupingBy(i -> i.getPriority().name(), Collectors.counting()));
        // open issues (not DONE)
        long openIssues = issueRepository.findAll().stream()
                .filter(i -> i.getStatus() == null || !i.getStatus().name().equals("DONE")).count();
        long doneIssues = issuesByStatus.getOrDefault("DONE", 0L);
        double completionRate = totalIssues > 0
                ? Math.round((doneIssues * 100.0 / totalIssues) * 10.0) / 10.0
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("activeUsers", activeUsers);
        result.put("newUsersLast7Days", newUsersLast7Days);
        result.put("newUsersLast30Days", newUsersLast30Days);
        result.put("totalProjects", totalProjects);
        result.put("totalIssues", totalIssues);
        result.put("openIssues", openIssues);
        result.put("completionRate", completionRate);
        result.put("issuesByStatus", issuesByStatus);
        result.put("issuesByPriority", issuesByPriority);

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────
    // GET /api/admin/users — User list
    // ─────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers(
            @RequestParam(required = false) String search) {
        requireAdmin();

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = users.stream()
                .filter(u -> u.getDeletedAt() == null)
                .filter(u -> search == null || search.isBlank()
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase()))
                        || (u.getProfileName() != null && u.getProfileName().toLowerCase().contains(search.toLowerCase())))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("profileName", u.getProfileName());
                    m.put("email", u.getEmail());
                    m.put("picture", u.getPicture());
                    m.put("isActive", u.isActive());
                    m.put("isVerified", u.isVerified());
                    m.put("isAdmin", u.isAdmin());
                    m.put("jobTitle", u.getJobTitle());
                    m.put("organization", u.getOrganization());
                    m.put("createdAt", u.getCreatedAt());
                    m.put("projectCount", u.getProjectMembers() != null ? u.getProjectMembers().size() : 0);
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────
    // PUT /api/admin/users/{id}/toggle-active
    // ─────────────────────────────────────────────
    @PutMapping("/users/{id}/toggle-active")
    @Transactional
    public ResponseEntity<Void> toggleUserActive(@PathVariable UUID id) {
        requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // DELETE /api/admin/users/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        requireAdmin();
        // Soft delete via @SQLDelete on User entity
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // GET /api/admin/projects — All projects
    // ─────────────────────────────────────────────
    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getProjects(
            @RequestParam(required = false) String search) {
        requireAdmin();

        List<Map<String, Object>> result = projectRepository.findAll().stream()
                .filter(p -> search == null || search.isBlank()
                        || p.getProjectName().toLowerCase().contains(search.toLowerCase()))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("projectName", p.getProjectName());
                    m.put("description", p.getDescription());
                    m.put("isPrivate", p.isPrivate());
                    m.put("memberCount", p.getProjectMembers() != null ? p.getProjectMembers().size() : 0);
                    m.put("issueCount", p.getIssues() != null ? p.getIssues().size() : 0);
                    long doneCount = p.getIssues() != null ? p.getIssues().stream()
                            .filter(i -> i.getStatus() != null && i.getStatus().name().equals("DONE")).count() : 0;
                    long total = p.getIssues() != null ? p.getIssues().size() : 0;
                    m.put("completionRate", total > 0 ? Math.round(doneCount * 100.0 / total * 10) / 10.0 : 0.0);
                    m.put("createdAt", p.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────
    // GET /api/admin/activity-chart — 7-day activity
    // ─────────────────────────────────────────────
    @GetMapping("/activity-chart")
    public ResponseEntity<List<Map<String, Object>>> getActivityChart() {
        requireAdmin();

        List<Map<String, Object>> days = new ArrayList<>();
        Instant now = Instant.now();
        List<com.example.rookwork_backend_sb.entities.Issue> allIssues = issueRepository.findAll();

        for (int i = 6; i >= 0; i--) {
            Instant dayStart = now.minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
            long issuesCreated = allIssues.stream()
                    .filter(issue -> issue.getCreatedAt() != null
                            && issue.getCreatedAt().isAfter(dayStart)
                            && issue.getCreatedAt().isBefore(dayEnd))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dayStart.toString().substring(0, 10));
            point.put("issuesCreated", issuesCreated);
            days.add(point);
        }
        return ResponseEntity.ok(days);
    }
}
