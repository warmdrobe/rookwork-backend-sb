package com.example.rookwork_backend_sb.config;

import com.example.rookwork_backend_sb.repositories.SystemSettingRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final SystemSettingRepository systemSettingRepository;
    private final SecurityUtil securityUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 1. Check if maintenance mode is active
        boolean isMaintenance = systemSettingRepository.findById("maintenance_mode")
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);

        if (!isMaintenance) {
            return true;
        }

        // 2. Only intercept /api/ requests
        if (!path.startsWith("/api/")) {
            return true;
        }

        // 3. Allow Public API for checking status
        if (path.startsWith("/api/sys/status")) {
            return true;
        }

        // 4. Allow Auth endpoints so users can attempt to log in
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // 5. Check if user is authenticated and is Admin
        try {
            if (securityUtil.isCurrentUserAdmin()) {
                // Admin bypasses maintenance
                return true;
            }
        } catch (Exception e) {
            // Not authenticated or not admin, fall through to block
        }

        // 6. Block request with 503 Service Unavailable
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"The system is currently under maintenance. Please try again later.\"}");
        return false;
    }
}
