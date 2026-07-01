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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.example.rookwork_backend_sb.dtos.events.UpdateEventRequest;

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
    private final EmailService emailService;

    @Value("${app.frontend.url:https://www.rookwork.asia}")
    private String frontendUrl;

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

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
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
                            if (guest.isNotifyEventInvited()) {
                                emailService.sendEventInvitation(
                                        guest.getEmail(),
                                        event.getEventName(),
                                        event.getEventDescription(),
                                        formatEventTime(event.getStartTime(), event.getEndTime()),
                                        event.getLocation(),
                                        creator.getProfileName(),
                                        event.getProject() != null ? event.getProject().getProjectName() : null,
                                        frontendUrl + "/calendars"
                                );
                            }
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
                if (guest.isNotifyEventInvited()) {
                    emailService.sendEventInvitation(
                            guest.getEmail(),
                            event.getEventName(),
                            event.getEventDescription(),
                            formatEventTime(event.getStartTime(), event.getEndTime()),
                            event.getLocation(),
                            creator.getProfileName(),
                            event.getProject() != null ? event.getProject().getProjectName() : null,
                            frontendUrl + "/calendars"
                    );
                }
            }
        }

        return toResponse(saved);
    }

    private static final java.time.format.DateTimeFormatter EVENT_DATE_FORMATTER = 
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    private String formatEventTime(Instant start, Instant end) {
        if (start == null) return "";
        String startStr = EVENT_DATE_FORMATTER.format(start);
        if (end == null) {
            return startStr + " (GMT+7)";
        }
        String endStr = EVENT_DATE_FORMATTER.format(end);
        return startStr + " - " + endStr + " (GMT+7)";
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "Không có";
        return EVENT_DATE_FORMATTER.format(instant) + " (GMT+7)";
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

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Set<User> guests = event.getGuests();
        String eventName = event.getEventName();
        String eventTime = formatEventTime(event.getStartTime(), event.getEndTime());
        String cancelledByName = currentUser.getProfileName();

        for (User guest : guests) {
            if (guest.getId().equals(currentUserId)) continue;
            if (guest.isNotifyEventInvited()) {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            emailService.sendEventCancelled(guest.getEmail(), eventName, eventTime, cancelledByName);
                        }
                    });
                } else {
                    emailService.sendEventCancelled(guest.getEmail(), eventName, eventTime, cancelledByName);
                }
            }
        }

        eventRepository.delete(event);
    }

    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request) {
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
            throw new ForbiddenException("You do not have permission to update this event");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        String oldName = event.getEventName();
        Instant oldStart = event.getStartTime();
        Instant oldEnd = event.getEndTime();
        String oldLocation = event.getLocation();

        if (request.getEventName() != null) event.setEventName(request.getEventName());
        if (request.getEventDescription() != null) event.setEventDescription(request.getEventDescription());
        if (request.getStartTime() != null) event.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) event.setEndTime(request.getEndTime());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getColor() != null) event.setColor(request.getColor());

        Set<User> oldGuests = new HashSet<>(event.getGuests());
        if (request.getGuestEmails() != null) {
            Set<User> newGuests = new HashSet<>();
            for (String email : request.getGuestEmails()) {
                userRepository.findByEmail(email).ifPresent(newGuests::add);
            }
            event.setGuests(newGuests);
        }

        event.setUpdatedAt(Instant.now());
        Event saved = eventRepository.save(event);

        List<String> changes = new ArrayList<>();
        if (!Objects.equals(oldName, event.getEventName())) {
            changes.add(String.format("Tên sự kiện: \"%s\" -> \"%s\"", oldName, event.getEventName()));
        }
        if (!Objects.equals(oldStart, event.getStartTime())) {
            changes.add(String.format("Thời gian bắt đầu: %s -> %s", formatInstant(oldStart), formatInstant(event.getStartTime())));
        }
        if (!Objects.equals(oldEnd, event.getEndTime())) {
            changes.add(String.format("Thời gian kết thúc: %s -> %s", formatInstant(oldEnd), formatInstant(event.getEndTime())));
        }
        if (!Objects.equals(oldLocation, event.getLocation())) {
            changes.add(String.format("Địa điểm: \"%s\" -> \"%s\"", oldLocation != null ? oldLocation : "Không có", event.getLocation() != null ? event.getLocation() : "Không có"));
        }

        if (!changes.isEmpty()) {
            String changeSummary = String.join(", ", changes);
            String eventName = event.getEventName();
            String eventTime = formatEventTime(event.getStartTime(), event.getEndTime());
            String location = event.getLocation();
            String updatedByName = currentUser.getProfileName();

            for (User guest : event.getGuests()) {
                if (guest.getId().equals(currentUserId)) continue;
                if (guest.isNotifyEventInvited()) {
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                emailService.sendEventUpdated(
                                        guest.getEmail(),
                                        eventName,
                                        eventTime,
                                        location,
                                        updatedByName,
                                        changeSummary,
                                        frontendUrl + "/calendars"
                                );
                            }
                        });
                    } else {
                        emailService.sendEventUpdated(
                                guest.getEmail(),
                                eventName,
                                eventTime,
                                location,
                                updatedByName,
                                changeSummary,
                                frontendUrl + "/calendars"
                        );
                    }
                }
            }
        }

        for (User guest : event.getGuests()) {
            if (!oldGuests.contains(guest) && !guest.getId().equals(currentUserId)) {
                if (guest.isNotifyEventInvited()) {
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                emailService.sendEventInvitation(
                                        guest.getEmail(),
                                        event.getEventName(),
                                        event.getEventDescription(),
                                        formatEventTime(event.getStartTime(), event.getEndTime()),
                                        event.getLocation(),
                                        event.getUser().getProfileName(),
                                        event.getProject() != null ? event.getProject().getProjectName() : null,
                                        frontendUrl + "/calendars"
                                );
                            }
                        });
                    } else {
                        emailService.sendEventInvitation(
                                guest.getEmail(),
                                event.getEventName(),
                                event.getEventDescription(),
                                formatEventTime(event.getStartTime(), event.getEndTime()),
                                event.getLocation(),
                                event.getUser().getProfileName(),
                                event.getProject() != null ? event.getProject().getProjectName() : null,
                                frontendUrl + "/calendars"
                        );
                    }
                }
            }
        }

        return toResponse(saved);
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
