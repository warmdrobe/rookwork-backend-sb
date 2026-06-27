package com.example.rookwork_backend_sb.security;

import com.example.rookwork_backend_sb.entities.ProjectMemberId;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP WebSocket channel interceptor that enforces authentication and authorization.
 *
 * <p><b>CONNECT:</b> Validates the JWT token from the Authorization header.
 * Sets the authenticated principal so Spring Security tracks the session.
 *
 * <p><b>SUBSCRIBE:</b> Prevents unauthorized channel access (WebSocket Subscription Leak).
 * Any subscription to a project-scoped topic is checked against the project_members table.
 * If the user is not a member of the project, the subscription is rejected with
 * {@link AccessDeniedException}.
 *
 * <p>Covered topic patterns:
 * <ul>
 *   <li>{@code /topic/project/{projectId}/issues}</li>
 *   <li>{@code /topic/project/{projectId}/activities}</li>
 *   <li>{@code /topic/project/{projectId}/issue/{issueId}/comments}</li>
 *   <li>{@code /topic/project/{projectId}/issue/{issueId}/activities}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /** Regex to extract the projectId UUID from any /topic/project/{projectId}/... destination. */
    private static final Pattern PROJECT_TOPIC_PATTERN =
            Pattern.compile("^/topic/project/([0-9a-fA-F-]{36})(/.*)?$");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Authenticates the user during CONNECT by validating their JWT token.
     * Sets the authenticated {@link Principal} on the STOMP session so it is
     * available for subsequent SUBSCRIBE / SEND frames.
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String userId = jwtService.extractUserId(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                accessor.setUser(auth);
                log.debug("WebSocket CONNECT authenticated for user: {}", userId);
            } catch (Exception ex) {
                // Silently reject – unauthenticated sessions cannot subscribe to protected topics
                log.warn("WebSocket CONNECT rejected – invalid JWT: {}", ex.getMessage());
            }
        }
    }

    /**
     * Enforces project membership on SUBSCRIBE commands targeting project-scoped topics.
     *
     * <p>Non-project topics (e.g. {@code /user/queue/notifications}) are allowed through
     * without any additional check – they are already protected by per-user routing.
     *
     * @throws AccessDeniedException if the current user is not a member of the project
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        Matcher matcher = PROJECT_TOPIC_PATTERN.matcher(destination);

        // Only enforce membership check on project-scoped topics
        if (!matcher.matches()) {
            return;
        }

        UUID projectId;
        try {
            projectId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            log.warn("WebSocket SUBSCRIBE rejected – malformed projectId in destination: {}", destination);
            throw new AccessDeniedException("Invalid project identifier in WebSocket destination");
        }

        UUID userId = extractCurrentUserId(accessor);

        if (userId == null) {
            log.warn("WebSocket SUBSCRIBE rejected – unauthenticated session attempted to subscribe: {}", destination);
            throw new AccessDeniedException("Authentication required to subscribe to project topics");
        }

        boolean isMember = projectMemberRepository.existsById(new ProjectMemberId(userId, projectId));

        if (!isMember) {
            log.warn("WebSocket SUBSCRIBE rejected – user {} is not a member of project {} (destination: {})",
                    userId, projectId, destination);
            throw new AccessDeniedException(
                    "Access denied: you are not a member of project " + projectId);
        }

        log.debug("WebSocket SUBSCRIBE authorized – user {} subscribed to {}", userId, destination);
    }

    /**
     * Extracts the authenticated user's ID UUID from the STOMP session principal.
     * Returns {@code null} if the session is unauthenticated.
     */
    private UUID extractCurrentUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object details = auth.getPrincipal();
            if (details instanceof UserDetails userDetails) {
                try {
                    return UUID.fromString(userDetails.getUsername());
                } catch (IllegalArgumentException ex) {
                    log.error("WebSocket: principal username is not a valid UUID: {}", userDetails.getUsername());
                }
            }
        }

        return null;
    }
}