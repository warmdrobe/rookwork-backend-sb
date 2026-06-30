package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.entities.User;
import com.example.rookwork_backend_sb.entities.SystemSetting;
import com.example.rookwork_backend_sb.repositories.IssueRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.SystemSettingRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.repositories.CommentRepository;
import com.example.rookwork_backend_sb.repositories.FileRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
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
    private final SystemSettingRepository systemSettingRepository;
    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;

    private void requireAdmin() {
        UUID userId = securityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Must be an admin to access admin dashboard");
        }
    }

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

        long dau = userRepository.countActiveUsersSince(now.minus(1, ChronoUnit.DAYS));
        long mau = userRepository.countActiveUsersSince(now.minus(30, ChronoUnit.DAYS));

        long totalProjects = projectRepository.count();

        List<com.example.rookwork_backend_sb.entities.Issue> allIssues = issueRepository.findAll();
        long totalIssues = allIssues.size();
        
        Map<String, Long> issuesByStatus = allIssues.stream()
                .filter(i -> i.getStatus() != null)
                .collect(Collectors.groupingBy(i -> i.getStatus().name(), Collectors.counting()));
                
        Map<String, Long> issuesByPriority = allIssues.stream()
                .filter(i -> i.getPriority() != null)
                .collect(Collectors.groupingBy(i -> i.getPriority().name(), Collectors.counting()));
                
        long openIssues = allIssues.stream()
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
        result.put("dau", dau);
        result.put("mau", mau);
        result.put("totalProjects", totalProjects);
        result.put("totalIssues", totalIssues);
        result.put("totalComments", commentRepository.count());
        result.put("totalFiles", fileRepository.count());
        result.put("openIssues", openIssues);
        result.put("completionRate", completionRate);
        result.put("issuesByStatus", issuesByStatus);
        result.put("issuesByPriority", issuesByPriority);

        return ResponseEntity.ok(result);
    }

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



    @PutMapping("/users/{id}/toggle-active")
    @Transactional
    public ResponseEntity<Void> toggleUserActive(@PathVariable UUID id) {
        requireAdmin();
        
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot toggle admin user");
        }
        user.setActive(!user.isActive());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        requireAdmin();
        User user = userRepository.findById(id).orElseThrow();
        if (user.isAdmin()) {
             throw new org.springframework.security.access.AccessDeniedException("Cannot delete admin user");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getProjects() {
        requireAdmin();
        List<com.example.rookwork_backend_sb.entities.Project> projects = projectRepository.findAll();
        List<Map<String, Object>> result = projects.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("projectName", p.getProjectName());
            m.put("createdAt", p.getCreatedAt());
            long issueCount = p.getIssues() != null ? p.getIssues().size() : 0;
            String ownerName = p.getProjectMembers() != null ? p.getProjectMembers().stream()
                    .filter(member -> "OWNER".equals(member.getRole().name()))
                    .findFirst()
                    .map(member -> member.getUser().getProfileName())
                    .orElse("Unknown") : "Unknown";
            m.put("memberCount", p.getProjectMembers() != null ? p.getProjectMembers().size() : 0);
            m.put("issueCount", issueCount);
            m.put("ownerName", ownerName);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-workspaces")
    public ResponseEntity<List<Map<String, Object>>> getTopWorkspaces() {
        requireAdmin();
        List<com.example.rookwork_backend_sb.entities.Project> projects = projectRepository.findTopActiveProjects(PageRequest.of(0, 5));
        
        List<Map<String, Object>> result = projects.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("projectName", p.getProjectName());
            m.put("createdAt", p.getCreatedAt());
            long issueCount = p.getIssues() != null ? p.getIssues().size() : 0;
            String ownerName = p.getProjectMembers() != null ? p.getProjectMembers().stream()
                    .filter(member -> "OWNER".equals(member.getRole().name()))
                    .findFirst()
                    .map(member -> member.getUser().getProfileName())
                    .orElse("Unknown") : "Unknown";
            m.put("ownerName", ownerName);
            m.put("issueCount", issueCount);
            // Count comments inside issues
            long commentCount = p.getIssues() != null ? p.getIssues().stream().mapToLong(i -> i.getComments() != null ? i.getComments().size() : 0).sum() : 0;
            m.put("commentCount", commentCount);
            m.put("totalInteractions", issueCount + commentCount);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/activity-chart")
    public ResponseEntity<List<Map<String, Object>>> getActivityChart() {
        requireAdmin();

        List<Map<String, Object>> days = new ArrayList<>();
        Instant now = Instant.now();
        List<User> allUsers = userRepository.findAll();

        for (int i = 6; i >= 0; i--) {
            Instant dayStart = now.minus(i, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
            long usersCreated = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && u.getCreatedAt().isAfter(dayStart)
                            && u.getCreatedAt().isBefore(dayEnd))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dayStart.toString().substring(0, 10));
            point.put("registrations", usersCreated);
            days.add(point);
        }
        return ResponseEntity.ok(days);
    }

    // =========================================================================
    // SETTINGS ENDPOINTS
    // =========================================================================

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        requireAdmin();
        List<SystemSetting> settingsList = systemSettingRepository.findAll();
        Map<String, String> settings = new HashMap<>();
        for (SystemSetting s : settingsList) {
            settings.put(s.getSettingKey(), s.getSettingValue());
        }
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    @Transactional
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> payload) {
        requireAdmin();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            String key = entry.getKey();
            SystemSetting setting = systemSettingRepository.findById(key).orElse(new SystemSetting());
            setting.setSettingKey(key);
            setting.setSettingValue(entry.getValue());
            systemSettingRepository.save(setting);
        }
        return getSettings();
    }

    @GetMapping("/resources")
    public ResponseEntity<List<Map<String, Object>>> getResourceUsage() {
        requireAdmin();

        // Collect file stats per project
        Map<UUID, long[]> fileStatsPerProject = new HashMap<>();
        for (Object[] row : fileRepository.getFileStatsByProject()) {
            UUID projectId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            long sizeBytes = ((Number) row[2]).longValue();
            fileStatsPerProject.put(projectId, new long[]{count, sizeBytes});
        }

        // Collect comment stats per project
        Map<UUID, Long> commentCountPerProject = new HashMap<>();
        for (Object[] row : commentRepository.getCommentCountByProject()) {
            UUID projectId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            commentCountPerProject.put(projectId, count);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        List<com.example.rookwork_backend_sb.entities.Project> projects = projectRepository.findAll();

        for (com.example.rookwork_backend_sb.entities.Project p : projects) {
            // Get owner (first OWNER role member)
            String ownerName = p.getProjectMembers().stream()
                    .filter(m -> "OWNER".equals(m.getRole().name()))
                    .findFirst()
                    .map(m -> m.getUser().getProfileName())
                    .orElse("Unknown");
            String ownerEmail = p.getProjectMembers().stream()
                    .filter(m -> "OWNER".equals(m.getRole().name()))
                    .findFirst()
                    .map(m -> m.getUser().getEmail())
                    .orElse("");

            long[] fileStats = fileStatsPerProject.getOrDefault(p.getId(), new long[]{0, 0});
            long commentCount = commentCountPerProject.getOrDefault(p.getId(), 0L);
            long issueCnt = p.getIssues().size();
            long memberCnt = p.getProjectMembers().size();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("projectId", p.getId());
            item.put("projectName", p.getProjectName());
            item.put("ownerName", ownerName);
            item.put("ownerEmail", ownerEmail);
            item.put("memberCount", memberCnt);
            item.put("issueCount", issueCnt);
            item.put("commentCount", commentCount);
            item.put("fileCount", fileStats[0]);
            item.put("storageSizeBytes", fileStats[1]);
            result.add(item);
        }

        // Sort by storage size desc
        result.sort((a, b) -> Long.compare(
                ((Number) b.get("storageSizeBytes")).longValue(),
                ((Number) a.get("storageSizeBytes")).longValue()
        ));

        return ResponseEntity.ok(result);
    }
}
