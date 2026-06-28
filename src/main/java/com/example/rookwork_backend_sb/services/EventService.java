package com.example.rookwork_backend_sb.services;

import com.example.rookwork_backend_sb.dtos.UserSummary;
import com.example.rookwork_backend_sb.dtos.events.CreateEventRequest;
import com.example.rookwork_backend_sb.dtos.events.EventResponse;
import com.example.rookwork_backend_sb.entities.*;
import com.example.rookwork_backend_sb.exceptions.ForbiddenException;
import com.example.rookwork_backend_sb.exceptions.ResourceNotFoundException;
import com.example.rookwork_backend_sb.repositories.EventRepository;
import com.example.rookwork_backend_sb.repositories.NotificationRepository;
import com.example.rookwork_backend_sb.repositories.ProjectMemberRepository;
import com.example.rookwork_backend_sb.repositories.ProjectRepository;
import com.example.rookwork_backend_sb.repositories.UserRepository;
import com.example.rookwork_backend_sb.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityUtil securityUtil;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByProject(UUID projectId) {
        return eventRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents() {
        UUID currentUserId = securityUtil.getCurrentUserId();
        return eventRepository.findByUserIdOrGuestId(currentUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator user not found"));

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        }

        Set<User> guests = new HashSet<>();
        if (request.getGuestEmails() != null) {
            for (String email : request.getGuestEmails()) {
                userRepository.findByEmail(email).ifPresent(guests::add);
            }
        }

        Event event = Event.builder()
                .eventName(request.getEventName())
                .eventDescription(request.getEventDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .color(request.getColor())
                .user(creator)
                .project(project)
                .guests(guests)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Event saved = eventRepository.save(event);

        // Send notifications to all guests (except creator themselves, if included)
        for (User guest : guests) {
            if (guest.getId().equals(currentUserId)) continue;

            Notification notification = Notification.builder()
                    .user(guest)
                    .sender(creator)
                    .title("Event Invitation")
                    .message(creator.getProfileName() + " invited you to the event \"" + event.getEventName() + "\"")
                    .isRead(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            notificationRepository.save(notification);

            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            messagingTemplate.convertAndSendToUser(
                                    guest.getId().toString(),
                                    "/queue/notifications",
                                    Map.of(
                                            "type", "EVENT_INVITATION",
                                            "notificationId", notification.getId()
                                    )
                            );
                        }
                    }
                );
            } else {
                messagingTemplate.convertAndSendToUser(
                        guest.getId().toString(),
                        "/queue/notifications",
                        Map.of(
                                "type", "EVENT_INVITATION",
                                "notificationId", notification.getId()
                        )
                );
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public void deleteEvent(UUID eventId) {
        UUID currentUserId = securityUtil.getCurrentUserId();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        boolean isCreator = event.getUser().getId().equals(currentUserId);
        boolean isProjectOwner = false;
        if (event.getProject() != null) {
            isProjectOwner = projectMemberRepository.existsByIdAndRole(
                    new ProjectMemberId(currentUserId, event.getProject().getId()), ProjectRole.OWNER);
        }

        if (!isCreator && !isProjectOwner) {
            throw new ForbiddenException("You do not have permission to delete this event");
        }

        eventRepository.delete(event);
    }

    private EventResponse toResponse(Event event) {
        List<UserSummary> guestsList = Collections.emptyList();
        if (event.getGuests() != null) {
            guestsList = event.getGuests().stream()
                    .map(this::toUserSummary)
                    .collect(Collectors.toList());
        }

        return EventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .eventDescription(event.getEventDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .location(event.getLocation())
                .color(event.getColor())
                .creator(toUserSummary(event.getUser()))
                .guests(guestsList)
                .projectId(event.getProject() != null ? event.getProject().getId() : null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private UserSummary toUserSummary(User u) {
        if (u == null) return null;
        return UserSummary.builder()
                .id(u.getId())
                .profileName(u.getProfileName())
                .picture(s3Service.getAvatarUrl(u.getPicture()))
                .email(u.getEmail())
                .build();
    }
}
