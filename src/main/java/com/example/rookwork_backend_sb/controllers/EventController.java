package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.events.CreateEventRequest;
import com.example.rookwork_backend_sb.dtos.events.EventResponse;
import com.example.rookwork_backend_sb.services.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<EventResponse>> getEventsByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(eventService.getEventsByProject(projectId));
    }

    @GetMapping("/my-events")
    public ResponseEntity<List<EventResponse>> getMyEvents() {
        return ResponseEntity.ok(eventService.getMyEvents());
    }

    @PostMapping
    @PreAuthorize("#request.projectId == null or @projectSecurity.isMember(#request.projectId)")
    public ResponseEntity<EventResponse> createEvent(@RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
